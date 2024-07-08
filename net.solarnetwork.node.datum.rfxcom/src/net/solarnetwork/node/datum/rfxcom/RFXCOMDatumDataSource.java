/* ==================================================================
 * RFXCOMDatumDataSource.java - Jul 8, 2012 4:44:30 PM
 *
 * Copyright 2007-2012 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.datum.rfxcom;

import static java.util.Collections.singletonMap;
import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.concurrent.ConcurrentSkipListSet;
import org.springframework.context.MessageSource;
import org.springframework.context.support.ResourceBundleMessageSource;
import net.solarnetwork.domain.datum.DatumSamples;
import net.solarnetwork.node.domain.datum.AcEnergyDatum;
import net.solarnetwork.node.domain.datum.NodeDatum;
import net.solarnetwork.node.domain.datum.SimpleAcEnergyDatum;
import net.solarnetwork.node.rfxcom.AddressSource;
import net.solarnetwork.node.rfxcom.CurrentMessage;
import net.solarnetwork.node.rfxcom.EnergyMessage;
import net.solarnetwork.node.rfxcom.Message;
import net.solarnetwork.node.rfxcom.MessageHandler;
import net.solarnetwork.node.rfxcom.RFXCOM;
import net.solarnetwork.node.service.DatumDataSource;
import net.solarnetwork.node.service.MultiDatumDataSource;
import net.solarnetwork.node.service.support.DatumDataSourceSupport;
import net.solarnetwork.node.service.support.SerialPortBeanParameters;
import net.solarnetwork.service.OptionalService;
import net.solarnetwork.settings.SettingSpecifier;
import net.solarnetwork.settings.SettingSpecifierProvider;
import net.solarnetwork.settings.support.BasicTextFieldSettingSpecifier;
import net.solarnetwork.settings.support.BasicTitleSettingSpecifier;
import net.solarnetwork.settings.support.BasicToggleSettingSpecifier;
import net.solarnetwork.support.PrefixedMessageSource;
import net.solarnetwork.util.StringUtils;

/**
 * {@link MultiDatumDataSource} for {@link AcEnergyDatum} entities read from the
 * supported energy formats of the RFXCOM transceiver.
 *
 * @author matt
 * @version 2.1
 */
public class RFXCOMDatumDataSource extends DatumDataSourceSupport
		implements DatumDataSource, MultiDatumDataSource, SettingSpecifierProvider {

	/**
	 * The default value for the {@code maxWattHourSpikeVerificationDiff}
	 * property.
	 */
	private static final long DEFAULT_MAX_WATT_HOUR_SPIKE_VERIFICATION_DIFF = 5000L;

	/**
	 * The default value for the {@code maxWattHourVerificationDiff} property.
	 */
	private static final long DEFAULT_MAX_WATT_HOUR_WARMUP_VERIFICATION_DIFF = 500L;

	/**
	 * The default value for the {@code collectAllSourceIdsTimeout} property.
	 */
	public static final int DEFAULT_COLLECT_ALL_SOURCE_IDS_TIMEOUT = 55;

	/** The default value for the {@code voltage} property. */
	public static final float DEFAULT_VOLTAGE = 230.0F;

	/** The default value for the {@code currentSensorIndexFlags} property. */
	public static final int DEFAULT_CURRENT_SENSOR_INDEX_FLAGS = 1;

	/** The source ID template parameter for the RFXCOM device address. */
	public static final String ADDRESS_TEMPLATE_PARAM = "address";

	private OptionalService<RFXCOM> rfxcomTracker;
	private Map<String, String> addressSourceMapping = null;
	private boolean collectAllSourceIds = true;
	private int collectAllSourceIdsTimeout = DEFAULT_COLLECT_ALL_SOURCE_IDS_TIMEOUT;
	private float voltage = DEFAULT_VOLTAGE;
	private int currentSensorIndexFlags = DEFAULT_CURRENT_SENSOR_INDEX_FLAGS;

	// some in-memory error correction support, map keys are source IDs
	private long maxWattHourWarmupVerificationDiff = DEFAULT_MAX_WATT_HOUR_WARMUP_VERIFICATION_DIFF;
	private long maxWattHourSpikeVerificationDiff = DEFAULT_MAX_WATT_HOUR_SPIKE_VERIFICATION_DIFF;
	private final Map<String, Long> previousWattHours = new HashMap<>();
	private final Map<String, List<AcEnergyDatum>> datumBuffer = new HashMap<>();

	// in-memory listing of "seen" addresses, to support device discovery
	private final SortedSet<AddressSource> knownAddresses = new ConcurrentSkipListSet<>();

	/**
	 * Default constructor.
	 */
	public RFXCOMDatumDataSource() {
		super();
		setMessageSource(getDefaultMessageSource());
	}

	/**
	 * Add a new cached "known" address value.
	 *
	 * <p>
	 * This adds the address to the cached set of <em>known</em> addresses,
	 * which are shown as a read-only setting property to aid in mapping the
	 * right RFXCOM-recognized device address.
	 * </p>
	 *
	 * @param datum
	 *        the datum to add
	 */
	private void addKnownAddress(AddressSource datum) {
		knownAddresses.add(datum);
	}

	@Override
	public Class<? extends NodeDatum> getMultiDatumType() {
		return AcEnergyDatum.class;
	}

	@Override
	public Class<? extends NodeDatum> getDatumType() {
		return AcEnergyDatum.class;
	}

	private String getSourceIdForMessageAddress(String addr) {
		if ( getAddressSourceMapping() != null && getAddressSourceMapping().containsKey(addr) ) {
			addr = getAddressSourceMapping().get(addr);
		}
		return addr;
	}

	private List<AcEnergyDatum> getDatumBufferForSource(String source) {
		if ( !datumBuffer.containsKey(source) ) {
			datumBuffer.put(source, new ArrayList<AcEnergyDatum>(5));
		}
		return datumBuffer.get(source);
	}

	private void addToResultsCheckingData(AcEnergyDatum datum, List<NodeDatum> results) {
		if ( datum == null ) {
			return;
		}
		final String sourceId = (datum.getSourceId() == null ? "" : datum.getSourceId());
		if ( sourceId.isEmpty() ) {
			return;
		}

		if ( datum.getWattHourReading() != null ) {
			// calculate what would be the Wh diff
			Long prevGoodWh = previousWattHours.get(sourceId);
			List<AcEnergyDatum> buffer = getDatumBufferForSource(sourceId);
			if ( (prevGoodWh == null && buffer.size() < 2)
					|| (buffer.size() > 0 && buffer.size() < 2) ) {
				// don't know the Wh diff, or we've buffered one item, so buffer this value so we have
				// two buffered, because Wh might go back to zero when the transmitter is reset
				log.info("Buffering datum until enough collected for data verification: {}", datum);
				buffer.add(datum);
				return;
			} else if ( buffer.size() == 2 ) {
				// so we have 2 buffered items... and this new item. We expect this new item to have
				// Wh >= the last buffered item >= the first buffered item
				long diff = datum.getWattHourReading() - buffer.get(1).getWattHourReading();
				long diff2 = buffer.get(1).getWattHourReading() - buffer.get(0).getWattHourReading();
				if ( datum.getWattHourReading() >= buffer.get(1).getWattHourReading()
						&& buffer.get(1).getWattHourReading() >= buffer.get(0).getWattHourReading()
						&& (diff2 - diff) < maxWattHourWarmupVerificationDiff ) {
					prevGoodWh = buffer.get(1).getWattHourReading();
					results.addAll(buffer);
					buffer.clear();
				} else {
					// discard the oldest buffered item, and buffer our new one
					log.warn("Discarding datum that failed data validation: {}", buffer.get(0));
					buffer.remove(0);
					buffer.add(datum);
					return;
				}
			}

			if ( datum.getWattHourReading() < prevGoodWh ) {
				log.info("Buffering datum to verify data with next read (Wh decreased): {}", datum);
				buffer.add(datum);
				return;
			}

			long whDiff = Math.abs(datum.getWattHourReading() - prevGoodWh);
			if ( whDiff >= maxWattHourSpikeVerificationDiff ) {
				log.info("Buffering datum to verify data with next read ({} Wh spike): {}", whDiff,
						datum);
				buffer.add(datum);
				return;
			}

			previousWattHours.put(sourceId, datum.getWattHourReading());
		}

		results.add(datum);
	}

	private void addConsumptionDatumFromMessage(Message msg, List<NodeDatum> results) {
		final String address = ((AddressSource) msg).getAddress();
		if ( addressSourceMapping != null && !addressSourceMapping.containsKey(address) ) {
			return;
		}
		if ( msg instanceof EnergyMessage ) {
			EnergyMessage emsg = (EnergyMessage) msg;
			AcEnergyDatum d = new SimpleAcEnergyDatum(
					resolvePlaceholders(getSourceIdForMessageAddress(address),
							singletonMap(ADDRESS_TEMPLATE_PARAM, address)),
					Instant.now(), new DatumSamples());
			final double wh = emsg.getUsageWattHours();
			final double w = emsg.getInstantWatts();
			if ( wh > 0 ) {
				d.setWattHourReading(Math.round(wh));
			}
			d.setWatts((int) Math.ceil(w));
			addToResultsCheckingData(d, results);
		} else {
			// assume CurrentMessage
			CurrentMessage cmsg = (CurrentMessage) msg;

			// we turn each sensor into its own ConsumptionDatum, the sensors we collect
			// from are specified by the currentSensorIndexFlags property
			for ( int i = 1; i <= 3; i++ ) {
				if ( (i & currentSensorIndexFlags) != i ) {
					continue;
				}
				AcEnergyDatum d = new SimpleAcEnergyDatum(
						resolvePlaceholders(getSourceIdForMessageAddress(address) + "." + i,
								singletonMap(ADDRESS_TEMPLATE_PARAM, address)),
						Instant.now(), new DatumSamples());
				switch (i) {
					case 1:
						d.setWatts((int) Math.ceil(voltage * cmsg.getAmpReading1()));
						break;
					case 2:
						d.setWatts((int) Math.ceil(voltage * cmsg.getAmpReading2()));
						break;
					case 3:
						d.setWatts((int) Math.ceil(voltage * cmsg.getAmpReading3()));
						break;
				}
				addToResultsCheckingData(d, results);
			}
		}
	}

	@Override
	public NodeDatum readCurrentDatum() {
		final Collection<NodeDatum> results = readMultipleDatum();
		if ( results != null && results.size() > 0 ) {
			return results.iterator().next();
		}
		return null;
	}

	@Override
	public Collection<NodeDatum> readMultipleDatum() {
		final RFXCOM r = getRfxcomTracker().service();
		if ( r == null ) {
			return null;
		}
		try {
			List<Message> messages = readMessages();
			List<NodeDatum> results = new ArrayList<>(messages.size());
			for ( Message msg : messages ) {
				addConsumptionDatumFromMessage(msg, results);
			}
			return results;
		} catch ( IOException e ) {
			log.warn("Communication error collecting datum from RFXCOM: {}", e.getMessage());
			return null;
		}
	}

	private List<Message> readMessages() throws IOException {
		final List<Message> result = new ArrayList<>(3);
		RFXCOM rfxcom = OptionalService.service(rfxcomTracker);
		if ( rfxcom == null ) {
			return result;
		}
		final Set<String> desiredAddresses = (addressSourceMapping != null
				? addressSourceMapping.keySet()
				: Collections.emptySet());
		final int desiredAddressCount = (desiredAddresses.isEmpty() ? 1 : desiredAddresses.size());
		final long endTime = (collectAllSourceIds && !desiredAddresses.isEmpty()
				? System.currentTimeMillis() + (collectAllSourceIdsTimeout * 1000)
				: 0);
		final Set<String> seenAddresses = new HashSet<>(desiredAddressCount);
		rfxcom.listenForMessages(new MessageHandler() {

			@Override
			public boolean handleMessage(Message message) {
				if ( message instanceof AddressSource ) {
					// add a known address for this reading
					addKnownAddress((AddressSource) message);
				}
				if ( message instanceof EnergyMessage || message instanceof CurrentMessage ) {
					final String messageAddress = ((AddressSource) message).getAddress();
					if ( !seenAddresses.contains(messageAddress) && (desiredAddresses.isEmpty()
							|| desiredAddresses.contains(messageAddress)) ) {
						result.add(message);
						seenAddresses.add(messageAddress);
					}
				}
				return System.currentTimeMillis() < endTime
						&& seenAddresses.size() < desiredAddressCount;
			}
		});
		return result;
	}

	@Override
	public String getSettingUid() {
		return "net.solarnetwork.node.datum.rfxcom";
	}

	@Override
	public String getDisplayName() {
		return "RFXCOM energy consumption meter";
	}

	private MessageSource getDefaultMessageSource() {
		ResourceBundleMessageSource serial = new ResourceBundleMessageSource();
		serial.setBundleClassLoader(SerialPortBeanParameters.class.getClassLoader());
		serial.setBasename(SerialPortBeanParameters.class.getName());

		PrefixedMessageSource serialSource = new PrefixedMessageSource();
		serialSource.setDelegate(serial);
		serialSource.setPrefix("serialParams.");

		ResourceBundleMessageSource source = new ResourceBundleMessageSource();
		source.setBundleClassLoader(RFXCOMDatumDataSource.class.getClassLoader());
		source.setBasename(RFXCOMDatumDataSource.class.getName());
		source.setParentMessageSource(serialSource);
		return source;
	}

	@Override
	public List<SettingSpecifier> getSettingSpecifiers() {
		RFXCOMDatumDataSource defaults = new RFXCOMDatumDataSource();

		List<SettingSpecifier> results = new ArrayList<>(21);
		results.addAll(getIdentifiableSettingSpecifiers());
		results.add(new BasicTextFieldSettingSpecifier("rfxcomTracker.propertyFilters['uid']", null,
				false, "(objectClass=net.solarnetwork.node.rfxcom.RFXCOM)"));

		StringBuilder status = new StringBuilder();
		for ( AddressSource datum : knownAddresses ) {
			if ( status.length() > 0 ) {
				status.append(",\n");
			}
			status.append(datum.toString());
		}
		results.add(new BasicTitleSettingSpecifier("knownAddresses", status.toString(), true));

		results.add(new BasicTextFieldSettingSpecifier("addressSourceMappingValue", ""));
		results.add(
				new BasicToggleSettingSpecifier("collectAllSourceIds", defaults.collectAllSourceIds));
		results.add(new BasicTextFieldSettingSpecifier("collectAllSourceIdsTimeout",
				String.valueOf(defaults.collectAllSourceIdsTimeout)));
		results.add(new BasicTextFieldSettingSpecifier("currentSensorIndexFlags",
				String.valueOf(defaults.currentSensorIndexFlags)));
		results.add(new BasicTextFieldSettingSpecifier("maxWattHourWarmupVerificationDiff",
				String.valueOf(defaults.getMaxWattHourWarmupVerificationDiff())));
		results.add(new BasicTextFieldSettingSpecifier("maxWattHourSpikeVerificationDiff",
				String.valueOf(defaults.getMaxWattHourSpikeVerificationDiff())));

		return results;
	}

	/**
	 * Set a {@code addressSourceMapping} Map via an encoded String value.
	 *
	 * <p>
	 * The format of the {@code mapping} String should be:
	 * </p>
	 *
	 * <pre>
	 * key=val[,key=val,...]
	 * </pre>
	 *
	 * <p>
	 * Whitespace is permitted around all delimiters, and will be stripped from
	 * the keys and values.
	 * </p>
	 *
	 * @param mapping
	 *        the address mapping
	 */
	public void setAddressSourceMappingValue(String mapping) {
		setAddressSourceMapping(StringUtils.commaDelimitedStringToMap(mapping));
	}

	public OptionalService<RFXCOM> getRfxcomTracker() {
		return rfxcomTracker;
	}

	public void setRfxcomTracker(OptionalService<RFXCOM> rfxcomTracker) {
		this.rfxcomTracker = rfxcomTracker;
	}

	public Map<String, String> getAddressSourceMapping() {
		return addressSourceMapping;
	}

	public void setAddressSourceMapping(Map<String, String> addressSourceMapping) {
		this.addressSourceMapping = addressSourceMapping;
	}

	public boolean isCollectAllSourceIds() {
		return collectAllSourceIds;
	}

	public void setCollectAllSourceIds(boolean collectAllSourceIds) {
		this.collectAllSourceIds = collectAllSourceIds;
	}

	public int getCollectAllSourceIdsTimeout() {
		return collectAllSourceIdsTimeout;
	}

	public void setCollectAllSourceIdsTimeout(int collectAllSourceIdsTimeout) {
		this.collectAllSourceIdsTimeout = collectAllSourceIdsTimeout;
	}

	public float getVoltage() {
		return voltage;
	}

	public void setVoltage(float voltage) {
		this.voltage = voltage;
	}

	public int getCurrentSensorIndexFlags() {
		return currentSensorIndexFlags;
	}

	public void setCurrentSensorIndexFlags(int currentSensorIndexFlags) {
		this.currentSensorIndexFlags = currentSensorIndexFlags;
	}

	public long getMaxWattHourWarmupVerificationDiff() {
		return maxWattHourWarmupVerificationDiff;
	}

	public void setMaxWattHourWarmupVerificationDiff(long maxWattHourVerificationDiff) {
		this.maxWattHourWarmupVerificationDiff = maxWattHourVerificationDiff;
	}

	public long getMaxWattHourSpikeVerificationDiff() {
		return maxWattHourSpikeVerificationDiff;
	}

	public void setMaxWattHourSpikeVerificationDiff(long maxWattHourSpikeVerificationDiff) {
		this.maxWattHourSpikeVerificationDiff = maxWattHourSpikeVerificationDiff;
	}

}
