/* ==================================================================
 * TempestUdpDatumDataSource.java - 4/11/2023 3:31:31 pm
 *
 * Copyright 2023 SolarNetwork.net Dev Team
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License as
 * published by the Free Software Foundation; either version 2 of
 * the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA
 * 02111-1307 USA
 * ==================================================================
 */

package net.solarnetwork.node.datum.weather.weatherflow.tempest;

import static java.util.Collections.emptySet;
import static net.solarnetwork.domain.datum.DatumSamplesType.Accumulating;
import static net.solarnetwork.domain.datum.DatumSamplesType.Instantaneous;
import static net.solarnetwork.domain.datum.DatumSamplesType.Status;
import static net.solarnetwork.service.OptionalService.service;
import static net.solarnetwork.util.NumberUtils.narrow;
import static net.solarnetwork.util.StringUtils.commaDelimitedStringFromCollection;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ScheduledFuture;
import org.springframework.scheduling.TaskScheduler;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.DatagramChannel;
import io.netty.channel.socket.DatagramPacket;
import io.netty.channel.socket.nio.NioDatagramChannel;
import io.netty.handler.codec.MessageToMessageDecoder;
import io.netty.handler.codec.json.JsonObjectDecoder;
import net.solarnetwork.codec.JsonUtils;
import net.solarnetwork.domain.BasicDeviceInfo;
import net.solarnetwork.domain.Bitmaskable;
import net.solarnetwork.domain.DeviceInfo;
import net.solarnetwork.domain.datum.DatumSamples;
import net.solarnetwork.domain.datum.GeneralDatumMetadata;
import net.solarnetwork.node.domain.datum.AtmosphericDatum;
import net.solarnetwork.node.domain.datum.NodeDatum;
import net.solarnetwork.node.domain.datum.SimpleAtmosphericDatum;
import net.solarnetwork.node.domain.datum.SimpleDatum;
import net.solarnetwork.node.service.DatumMetadataService;
import net.solarnetwork.node.service.DatumQueue;
import net.solarnetwork.node.service.MultiDatumDataSource;
import net.solarnetwork.node.service.support.DatumDataSourceSupport;
import net.solarnetwork.service.ServiceLifecycleObserver;
import net.solarnetwork.settings.SettingSpecifier;
import net.solarnetwork.settings.SettingSpecifierProvider;
import net.solarnetwork.settings.SettingsChangeObserver;
import net.solarnetwork.settings.support.BasicTextFieldSettingSpecifier;

/**
 * WeatherFlow Tempest datum source reading UDP messages.
 *
 * @author matt
 * @version 1.1
 */
public class TempestUdpDatumDataSource extends DatumDataSourceSupport implements MultiDatumDataSource,
		SettingSpecifierProvider, ServiceLifecycleObserver, SettingsChangeObserver {

	/** The default UDP port to listen to. */
	public static final int DEFAULT_PORT = 50222;

	/** The startup delay, in seconds. */
	public static final int STARTUP_DELAY_SECS = 5;

	private final ObjectMapper objectMapper;
	private int port = DEFAULT_PORT;
	private String sourceId;

	private DeviceInfo deviceInfo;
	private DeviceInfo hubDeviceInfo;
	private ScheduledFuture<?> startupFuture;
	private volatile Channel channel;

	/**
	 * Constructor.
	 */
	public TempestUdpDatumDataSource() {
		super();
		objectMapper = JsonUtils.newObjectMapper();
	}

	@Override
	public String getSettingUid() {
		return "net.solarnetwork.node.datum.weatherflow.tempest";
	}

	@Override
	public List<SettingSpecifier> getSettingSpecifiers() {
		List<SettingSpecifier> results = new ArrayList<>(8);
		results.addAll(baseIdentifiableSettings(null));
		results.add(new BasicTextFieldSettingSpecifier("sourceId", null));
		results.add(new BasicTextFieldSettingSpecifier("port", String.valueOf(DEFAULT_PORT)));
		results.addAll(getDeviceInfoMetadataSettingSpecifiers());
		return results;
	}

	@Override
	public String deviceInfoSourceId() {
		return resolvePlaceholders(sourceId);
	}

	@Override
	public DeviceInfo deviceInfo() {
		final DeviceInfo di = this.deviceInfo;
		final DeviceInfo hdi = this.hubDeviceInfo;
		if ( di == null && hdi == null ) {
			return null;
		}

		StringBuilder sn = new StringBuilder();
		if ( di != null && di.getSerialNumber() != null ) {
			sn.append("Sensor: ").append(di.getSerialNumber());
		}
		if ( hdi != null && hdi.getSerialNumber() != null ) {
			if ( sn.length() > 0 ) {
				sn.append(", ");
			}
			sn.append("Hub: ").append(hdi.getSerialNumber());
		}

		StringBuilder v = new StringBuilder();
		if ( di != null && di.getVersion() != null ) {
			v.append("Sensor: ").append(di.getVersion());
		}
		if ( hdi != null && hdi.getVersion() != null ) {
			if ( v.length() > 0 ) {
				v.append(", ");
			}
			v.append("Hub: ").append(hdi.getVersion());
		}

		// @formatter:off
		final BasicDeviceInfo.Builder info = BasicDeviceInfo.builder()
				.withName(getUid())
				.withManufacturer("WeatherFlow")
				.withModelName("Tempest")
				;
		// @formatter:on

		if ( sn.length() > 0 ) {
			info.withSerialNumber(sn.toString());
		}
		if ( v.length() > 0 ) {
			info.withVersion(v.toString());
		}

		return info.build();
	}

	@Override
	public synchronized void serviceDidStartup() {
		configurationChanged(null);
	}

	@Override
	public synchronized void serviceDidShutdown() {
		stop();
	}

	@Override
	public synchronized void configurationChanged(Map<String, Object> properties) {
		if ( startupFuture != null ) {
			startupFuture.cancel(true);
			startupFuture = null;
		}
		stop();
		if ( sourceId == null || sourceId.isEmpty() ) {
			return;
		}
		final TaskScheduler scheduler = getTaskScheduler();
		if ( scheduler != null ) {
			log.info("Starting Tempest UDP listener for source [{}] in {}s", sourceId,
					STARTUP_DELAY_SECS);
			startupFuture = scheduler.schedule(this::start,
					Instant.now().plusSeconds(STARTUP_DELAY_SECS));
		} else {
			start();
		}
	}

	@Override
	public Collection<String> publishedSourceIds() {
		final String sourceId = resolvePlaceholders(this.sourceId);
		if ( sourceId == null || sourceId.isEmpty() ) {
			return Collections.emptySet();
		}
		final Set<String> result = new TreeSet<>();
		result.add(sourceId);
		result.add(sourceId + LIGHTNING_STRIKE_EVENT_SOURCE_ID_SUFFIX);
		result.add(sourceId + PRECIP_EVENT_SOURCE_ID_SUFFIX);
		result.add(sourceId + RAPID_WIND_EVENT_SOURCE_ID_SUFFIX);
		result.add(sourceId + STATUS_EVENT_SOURCE_ID_SUFFIX);
		return result;
	}

	private synchronized void start() {
		log.info("Starting Tempest UDP listener for source [{}]", sourceId);
		startupFuture = null;
		EventLoopGroup group = new NioEventLoopGroup();
		Bootstrap b = new Bootstrap();
		b.group(group).channel(NioDatagramChannel.class).option(ChannelOption.SO_BROADCAST, true)
				.handler(new ServerInitializer());
		try {
			Channel channel = b.bind(port).sync().channel();
			channel.closeFuture().addListener((f) -> {
				group.shutdownGracefully();
			});
			this.channel = channel;
		} catch ( Exception e ) {
			log.warn("Interrupted during start.", e);
		}
	}

	private synchronized void stop() {
		final Channel ch = this.channel;
		if ( ch == null ) {
			return;
		}
		ch.close().addListener(closeFuture -> {
			if ( closeFuture.isSuccess() ) {
				log.info("Tempest UDP listener stopped for source [{}]", sourceId);
			} else {
				log.warn("Error stopping Tempest UDP listener for source [{}]: {}", sourceId,
						closeFuture.cause());
			}
		});
		this.channel = null;
	}

	private class ServerInitializer extends ChannelInitializer<DatagramChannel> {

		@Override
		protected void initChannel(DatagramChannel ch) throws Exception {
			ch.pipeline().addLast("datagramDecoder", new MessageToMessageDecoder<DatagramPacket>() {

				@Override
				protected void decode(ChannelHandlerContext ctx, DatagramPacket msg, List<Object> out)
						throws Exception {
					out.add(msg.content().retain());
				}

			});
			ch.pipeline().addLast("jsonDecoder", new JsonObjectDecoder());
			ch.pipeline().addLast("jsonHandler", new JsonMessageHandler());
		}

		@Override
		public void channelActive(ChannelHandlerContext ctx) throws Exception {
			super.channelActive(ctx);
			log.info("Tempest UDP listening on port {} for source [{}]", port, sourceId);
		}

		@Override
		public void channelInactive(ChannelHandlerContext ctx) throws Exception {
			super.channelInactive(ctx);
			log.info("Tempest UDP stopped on port {} for source [{}]", port, sourceId);
		}

	}

	private class JsonMessageHandler extends SimpleChannelInboundHandler<ByteBuf> {

		@Override
		protected void channelRead0(ChannelHandlerContext ctx, ByteBuf msg) throws Exception {
			String json = msg.toString(StandardCharsets.UTF_8);
			log.debug("Received JSON message: {}", json);
			String sourceId = deviceInfoSourceId();
			if ( sourceId == null ) {
				return;
			}
			try {
				processJsonMessage(json);
			} catch ( IOException e ) {
				log.warn("Invalid JSON encountered in Tempest UDP source [{}]: {}: [{}]", sourceId,
						e.toString(), json);
			}
		}

	}

	/**
	 * Process a Tempest JSON message, generating datum as appropriate.
	 *
	 * @param json
	 *        the JSON to process
	 * @throws IOException
	 *         if any I/O error occurs
	 */
	public void processJsonMessage(String json) throws IOException {
		JsonNode root = objectMapper.readTree(json);
		processJsonMessage(root, sourceId);
	}

	private BasicDeviceInfo.Builder hubDeviceInfoBuilder(BasicDeviceInfo.Builder info) {
		return (info != null ? info
				: hubDeviceInfo == null ? BasicDeviceInfo.builder()
						: BasicDeviceInfo.builderFrom(hubDeviceInfo));
	}

	private BasicDeviceInfo.Builder deviceInfoBuilder(BasicDeviceInfo.Builder info) {
		return (info != null ? info
				: deviceInfo == null ? BasicDeviceInfo.builder()
						: BasicDeviceInfo.builderFrom(deviceInfo));
	}

	private void processJsonMessage(JsonNode root, String sourceId) {
		if ( (hubDeviceInfo == null || hubDeviceInfo.getSerialNumber() == null) && root.has("hub_sn") ) {
			BasicDeviceInfo.Builder info = hubDeviceInfoBuilder(null);
			this.hubDeviceInfo = info.withSerialNumber(root.path("hub_sn").asText()).build();
		}
		final String type = root.path("type").textValue();
		if ( handleHubStatusMessage(root, sourceId, type) ) {
			// done
		} else if ( handleDeviceStatusMessage(root, sourceId, type) ) {
			// done
		} else if ( handlePrecipMessage(root, sourceId, type) ) {
			// done
		} else if ( handleLightningStrikeMessage(root, sourceId, type) ) {
			// done
		} else if ( handleRapidWindMessage(root, sourceId, type) ) {
			// done
		} else if ( handleObsAirMessage(root, sourceId, type) ) {
			// done
		} else if ( handleObsSkyMessage(root, sourceId, type) ) {
			// done
		} else if ( handleObsStMessage(root, sourceId, type) ) {
			// done
		}
	}

	/** The source ID suffix added for status events. */
	public static final String STATUS_EVENT_SOURCE_ID_SUFFIX = "/status";

	private boolean handleHubStatusMessage(JsonNode root, String sourceId, String type) {
		if ( !"hub_status".equals(type) ) {
			return false;
		}

		BasicDeviceInfo.Builder hubInfo = null;

		if ( (hubDeviceInfo == null || hubDeviceInfo.getSerialNumber() == null)
				&& root.has("serial_number") ) {
			hubInfo = hubDeviceInfoBuilder(hubInfo);
			hubInfo.withSerialNumber(root.path("serial_number").asText());
		}

		if ( (hubDeviceInfo == null || hubDeviceInfo.getVersion() == null)
				&& root.has("firmware_revision") ) {
			hubInfo = hubDeviceInfoBuilder(hubInfo);
			hubInfo.withVersion(root.path("firmware_revision").asText());
		}

		if ( hubInfo != null ) {
			this.hubDeviceInfo = hubInfo.build();
			publishDeviceInfoMetadata(sourceId);
		}
		return true;
	}

	private boolean handleDeviceStatusMessage(JsonNode root, String sourceId, String type) {
		if ( !"device_status".equals(type) ) {
			return false;
		}

		BasicDeviceInfo.Builder info = null;

		if ( (deviceInfo == null || deviceInfo.getSerialNumber() == null)
				&& root.has("serial_number") ) {
			info = deviceInfoBuilder(info);
			info.withSerialNumber(root.path("serial_number").asText());
		}

		if ( (deviceInfo == null || deviceInfo.getVersion() == null) && root.has("firmware_revision") ) {
			info = deviceInfoBuilder(info);
			info.withVersion(root.path("firmware_revision").asText());
		}

		long secs = -1;
		JsonNode ts = root.path("timestamp");
		if ( ts.canConvertToLong() ) {
			secs = ts.asLong();
		}

		int status = -1;
		Set<SensorStatus> states = null;
		if ( root.has("sensor_status") ) {
			JsonNode s = root.path("sensor_status");
			if ( s.canConvertToInt() ) {
				status = s.asInt();
			}
			states = Bitmaskable.setForBitmask(status, SensorStatus.class);
		}

		double voltage = 0;
		if ( root.has("voltage") ) {
			voltage = root.path("voltage").asDouble();
		}

		long uptime = 0;
		if ( root.has("uptime") ) {
			uptime = root.path("uptime").asLong();
		}

		if ( secs > 0 ) {
			SimpleDatum d = SimpleDatum.nodeDatum(sourceId + STATUS_EVENT_SOURCE_ID_SUFFIX,
					Instant.ofEpochSecond(secs));

			if ( voltage > 0.0 ) {
				d.putSampleValue(Instantaneous, "voltage", narrow(voltage, 2));
			}

			if ( uptime > 0L ) {
				d.putSampleValue(Accumulating, "uptime", uptime);
			}

			if ( status > 0 ) {
				d.putSampleValue(Status, "opStates", status);
				if ( states != null && !states.isEmpty() ) {
					d.putSampleValue(Status, "status", commaDelimitedStringFromCollection(states));
				}
			}

			offerDatum(d);
		}

		if ( info != null ) {
			this.deviceInfo = info.build();
			publishDeviceInfoMetadata(sourceId);
		}

		return true;
	}

	/** The source ID suffix added for precipitation events. */
	public static final String PRECIP_EVENT_SOURCE_ID_SUFFIX = "/precip";

	private boolean handlePrecipMessage(JsonNode root, String sourceId, String type) {
		if ( !("evt_precip".equals(type) && root.path("evt").isArray()) ) {
			return false;
		}
		JsonNode ts = root.path("evt").path(0);
		if ( ts.canConvertToLong() ) {
			long secs = ts.asLong();
			SimpleDatum d = SimpleDatum.nodeDatum(sourceId + PRECIP_EVENT_SOURCE_ID_SUFFIX,
					Instant.ofEpochSecond(secs));
			d.putSampleValue(Status, "start", "1");
			offerDatum(d);
		}
		return true;
	}

	private void offerDatum(NodeDatum d) {
		if ( d == null ) {
			return;
		}
		DatumQueue q = service(getDatumQueue());
		if ( q != null ) {
			q.offer(d);
		}
	}

	/** The source ID suffix added for lightning strike events. */
	public static final String LIGHTNING_STRIKE_EVENT_SOURCE_ID_SUFFIX = "/strike";

	private boolean handleLightningStrikeMessage(JsonNode root, String sourceId, String type) {
		if ( !("evt_strike".equals(type) && root.path("evt").isArray()) ) {
			return false;
		}
		long secs = -1;
		double km = -1;
		long energy = -1;

		JsonNode array = root.path("evt");
		JsonNode el = array.path(0);
		if ( el.canConvertToLong() ) {
			secs = el.asLong();
		}

		el = array.path(1);
		if ( el.isNumber() ) {
			km = el.asDouble();
		}

		el = array.path(2);
		if ( el.canConvertToLong() ) {
			energy = el.asLong();
		}

		if ( secs >= 0 && km >= 0.0 ) {
			SimpleDatum d = SimpleDatum.nodeDatum(sourceId + LIGHTNING_STRIKE_EVENT_SOURCE_ID_SUFFIX,
					Instant.ofEpochSecond(secs));
			d.putSampleValue(Instantaneous, "distance", (int) (km * 1000));
			if ( energy >= 0 ) {
				d.putSampleValue(Instantaneous, "energy", energy);
			}
			offerDatum(d);
		}

		return true;
	}

	/** The source ID suffix added for rapid wind events. */
	public static final String RAPID_WIND_EVENT_SOURCE_ID_SUFFIX = "/wind";

	private boolean handleRapidWindMessage(JsonNode root, String sourceId, String type) {
		if ( !("rapid_wind".equals(type) && root.path("ob").isArray()) ) {
			return false;
		}
		long secs = -1;
		BigDecimal speed = null;
		int direction = -1;

		JsonNode array = root.path("ob");
		JsonNode el = array.path(0);
		if ( el.canConvertToLong() ) {
			secs = el.asLong();
		}

		el = array.path(1);
		if ( el.isNumber() ) {
			speed = el.decimalValue();
		}

		el = array.path(2);
		if ( el.canConvertToInt() ) {
			direction = el.asInt();
		}

		if ( secs >= 0 && speed != null ) {
			SimpleAtmosphericDatum d = new SimpleAtmosphericDatum(
					sourceId + RAPID_WIND_EVENT_SOURCE_ID_SUFFIX, Instant.ofEpochSecond(secs),
					new DatumSamples());
			d.setWindSpeed(speed);
			d.setWindDirection(direction);
			offerDatum(d);
		}

		return true;
	}

	private boolean handleObsAirMessage(JsonNode root, String sourceId, String type) {
		if ( !("obs_air".equals(type) && root.path("obs").isArray()) ) {
			return false;
		}

		JsonNode topArray = root.path("obs");
		for ( JsonNode array : topArray ) {
			if ( !array.isArray() ) {
				continue;
			}

			SimpleAtmosphericDatum d = null;

			JsonNode el = array.path(0);
			if ( el.canConvertToLong() ) {
				long secs = el.asLong();
				if ( secs > 0 ) {
					d = new SimpleAtmosphericDatum(sourceId, Instant.ofEpochSecond(secs),
							new DatumSamples());
				}
			}
			if ( d == null ) {
				continue;
			}

			el = array.path(1);
			if ( el.isNumber() ) {
				double mbar = el.asDouble();
				d.setAtmosphericPressure((int) (mbar * 100)); // store as pascals
			}

			el = array.path(2);
			if ( el.isNumber() ) {
				d.setTemperature(el.decimalValue());
			}

			el = array.path(3);
			if ( el.canConvertToInt() ) {
				d.setHumidity(el.asInt());
			}

			el = array.path(4);
			if ( el.canConvertToInt() ) {
				int count = el.asInt();
				if ( count > 0 ) {
					d.putSampleValue(Instantaneous, "strikes", count);
				}
			}

			el = array.path(5);
			if ( el.isNumber() ) {
				double avgDistanceKm = el.asDouble();
				if ( avgDistanceKm > 0.0 ) {
					d.putSampleValue(Instantaneous, "avgStrikeDistance", (int) (avgDistanceKm * 1000));
				}
			}

			el = array.path(6);
			if ( el.isNumber() ) {
				d.putSampleValue(Instantaneous, "batteryVoltage", el.decimalValue());
			}

			el = array.path(7);
			if ( el.isNumber() ) {
				d.putSampleValue(Instantaneous, "duration", el.asInt() * 60);
			}

			if ( !d.isEmpty() ) {
				offerDatum(d);
			}
		}

		return true;
	}

	private boolean handleObsSkyMessage(JsonNode root, String sourceId, String type) {
		if ( !("obs_sky".equals(type) && root.path("obs").isArray()) ) {
			return false;
		}

		JsonNode topArray = root.path("obs");
		for ( JsonNode array : topArray ) {
			if ( !array.isArray() ) {
				continue;
			}

			SimpleAtmosphericDatum d = null;

			JsonNode el = array.path(0);
			if ( el.canConvertToLong() ) {
				long secs = el.asLong();
				if ( secs > 0 ) {
					d = new SimpleAtmosphericDatum(sourceId, Instant.ofEpochSecond(secs),
							new DatumSamples());
				}
			}
			if ( d == null ) {
				continue;
			}

			el = array.path(1);
			if ( el.isNumber() ) {
				d.setLux(el.decimalValue());
			}

			el = array.path(2);
			if ( el.isNumber() ) {
				d.putSampleValue(Instantaneous, "uvIndex", el.decimalValue());
			}

			el = array.path(3);
			if ( el.isNumber() ) {
				d.putSampleValue(Instantaneous, "rain", el.decimalValue());
			}

			el = array.path(4);
			if ( el.isNumber() ) {
				d.putSampleValue(Instantaneous, "wspeed_lull", el.decimalValue());
			}

			el = array.path(5);
			if ( el.isNumber() ) {
				d.setWindSpeed(el.decimalValue());
			}

			el = array.path(6);
			if ( el.isNumber() ) {
				d.putSampleValue(Instantaneous, "wspeed_gust", el.decimalValue());
			}

			el = array.path(7);
			if ( el.isNumber() ) {
				d.setWindDirection(el.asInt());
			}

			el = array.path(8);
			if ( el.isNumber() ) {
				d.putSampleValue(Instantaneous, "batteryVoltage", el.decimalValue());
			}

			el = array.path(9);
			if ( el.isNumber() ) {
				d.putSampleValue(Instantaneous, "duration", el.asInt() * 60);
			}

			el = array.path(10);
			if ( el.isNumber() ) {
				d.setIrradiance(el.decimalValue());
			}

			el = array.path(11);
			if ( el.isNumber() ) {
				d.putSampleValue(Accumulating, "rain_day", el.decimalValue());
			}

			el = array.path(12);
			if ( el.canConvertToInt() ) {
				d.putSampleValue(Status, "precipType", el.asInt());
			}

			el = array.path(13);
			if ( el.canConvertToInt() ) {
				d.putSampleValue(Instantaneous, "windDuration", el.asInt());
			}

			if ( !d.isEmpty() ) {
				offerDatum(d);
			}
		}

		return true;
	}

	private boolean handleObsStMessage(JsonNode root, String sourceId, String type) {
		if ( !("obs_st".equals(type) && root.path("obs").isArray()) ) {
			return false;
		}

		JsonNode topArray = root.path("obs");
		for ( JsonNode array : topArray ) {
			if ( !array.isArray() ) {
				continue;
			}

			SimpleAtmosphericDatum d = null;

			JsonNode el = array.path(0);
			if ( el.canConvertToLong() ) {
				long secs = el.asLong();
				if ( secs > 0 ) {
					d = new SimpleAtmosphericDatum(sourceId, Instant.ofEpochSecond(secs),
							new DatumSamples());
				}
			}
			if ( d == null ) {
				continue;
			}

			el = array.path(1);
			if ( el.isNumber() ) {
				d.putSampleValue(Instantaneous, "wspeed_lull", el.decimalValue());
			}

			el = array.path(2);
			if ( el.isNumber() ) {
				d.setWindSpeed(el.decimalValue());
			}

			el = array.path(3);
			if ( el.isNumber() ) {
				d.putSampleValue(Instantaneous, "wspeed_gust", el.decimalValue());
			}

			el = array.path(4);
			if ( el.isNumber() ) {
				d.setWindDirection(el.asInt());
			}

			el = array.path(5);
			if ( el.canConvertToInt() ) {
				d.putSampleValue(Instantaneous, "windDuration", el.asInt());
			}

			el = array.path(6);
			if ( el.isNumber() ) {
				double mbar = el.asDouble();
				d.setAtmosphericPressure((int) (mbar * 100)); // store as pascals
			}

			el = array.path(7);
			if ( el.isNumber() ) {
				d.setTemperature(el.decimalValue());
			}

			el = array.path(8);
			if ( el.canConvertToInt() ) {
				d.setHumidity(el.asInt());
			}

			el = array.path(9);
			if ( el.isNumber() ) {
				d.setLux(el.decimalValue());
			}

			el = array.path(10);
			if ( el.isNumber() ) {
				d.putSampleValue(Instantaneous, "uvIndex", el.decimalValue());
			}

			el = array.path(11);
			if ( el.isNumber() ) {
				d.setIrradiance(el.decimalValue());
			}

			el = array.path(12);
			if ( el.isNumber() ) {
				d.putSampleValue(Instantaneous, "rain", el.decimalValue());
			}

			el = array.path(13);
			if ( el.canConvertToInt() ) {
				d.putSampleValue(Status, "precipType", el.asInt());
			}

			el = array.path(14);
			if ( el.isNumber() ) {
				double avgDistanceKm = el.asDouble();
				if ( avgDistanceKm > 0.0 ) {
					d.putSampleValue(Instantaneous, "avgStrikeDistance", (int) (avgDistanceKm * 1000));
				}
			}

			el = array.path(15);
			if ( el.canConvertToInt() ) {
				int count = el.asInt();
				if ( count > 0 ) {
					d.putSampleValue(Instantaneous, "strikes", count);
				}
			}

			el = array.path(16);
			if ( el.isNumber() ) {
				d.putSampleValue(Instantaneous, "batteryVoltage", el.decimalValue());
			}

			el = array.path(17);
			if ( el.isNumber() ) {
				d.putSampleValue(Instantaneous, "duration", el.asInt() * 60);
			}
			if ( !d.isEmpty() ) {
				offerDatum(d);
			}
		}

		return true;
	}

	private void publishDeviceInfoMetadata(String metaSourceId) {
		final DeviceInfo info = deviceInfo();
		if ( metaSourceId == null || info == null ) {
			return;
		}
		final DatumMetadataService metadataService = service(getDatumMetadataService());
		if ( metadataService == null ) {
			return;
		}
		Map<String, Object> m = JsonUtils.getStringMapFromObject(info);
		if ( m != null && !m.isEmpty() ) {
			GeneralDatumMetadata meta = new GeneralDatumMetadata(null,
					Collections.singletonMap(DeviceInfo.DEVICE_INFO_METADATA_KEY, m));
			metadataService.addSourceMetadata(metaSourceId, meta);
		}
	}

	@Override
	public Class<? extends NodeDatum> getMultiDatumType() {
		return AtmosphericDatum.class;
	}

	@Override
	public Collection<NodeDatum> readMultipleDatum() {
		// TODO
		return emptySet();
	}

	/**
	 * Get the port to listen on.
	 *
	 * @return the port; defaults to {@link #DEFAULT_PORT}
	 */
	public int getPort() {
		return port;
	}

	/**
	 * Set the port to listen on.
	 *
	 * @param port
	 *        the port to set
	 */
	public void setPort(int port) {
		this.port = port;
	}

	/**
	 * Get a single source ID to publish datum under.
	 *
	 * @return the sourceId to use, or {@literal null} to publish individual
	 *         sources per ping test
	 */
	public String getSourceId() {
		return sourceId;
	}

	/**
	 * Set a single source ID to publish datum under.
	 *
	 * @param sourceId
	 *        the sourceId to use, or {@literal null} to publish individual
	 *        sources per ping test
	 */
	public void setSourceId(String sourceId) {
		this.sourceId = sourceId;
	}

}
