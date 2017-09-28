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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.concurrent.ConcurrentSkipListSet;
import org.springframework.context.MessageSource;
import org.springframework.context.support.ResourceBundleMessageSource;
import net.solarnetwork.node.ConversationalDataCollector;
import net.solarnetwork.node.DatumDataSource;
import net.solarnetwork.node.MultiDatumDataSource;
import net.solarnetwork.node.domain.ACEnergyDatum;
import net.solarnetwork.node.domain.GeneralNodeACEnergyDatum;
import net.solarnetwork.node.rfxcom.AddressSource;
import net.solarnetwork.node.rfxcom.CurrentMessage;
import net.solarnetwork.node.rfxcom.EnergyMessage;
import net.solarnetwork.node.rfxcom.Message;
import net.solarnetwork.node.rfxcom.MessageFactory;
import net.solarnetwork.node.rfxcom.MessageListener;
import net.solarnetwork.node.rfxcom.RFXCOM;
import net.solarnetwork.node.settings.SettingSpecifier;
import net.solarnetwork.node.settings.SettingSpecifierProvider;
import net.solarnetwork.node.settings.support.BasicTextFieldSettingSpecifier;
import net.solarnetwork.node.settings.support.BasicTitleSettingSpecifier;
import net.solarnetwork.node.settings.support.BasicToggleSettingSpecifier;
import net.solarnetwork.node.support.DatumDataSourceSupport;
import net.solarnetwork.node.support.SerialPortBeanParameters;
import net.solarnetwork.node.util.PrefixedMessageSource;
import net.solarnetwork.util.OptionalService;
import net.solarnetwork.util.StringUtils;

/**
 * {@link MultiDatumDataSource} for {@link ACEnergyDatum} entities read from the
 * supported energy formats of the RFXCOM transceiver.
 * 
 * @author matt
 * @version 1.0
 */
public class RFXCOMDatumDataSource extends DatumDataSourceSupport
		implements DatumDataSource<ACEnergyDatum>, MultiDatumDataSource<ACEnergyDatum>,
		ConversationalDataCollector.Moderator<List<Message>>, SettingSpecifierProvider {

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

	private OptionalService<RFXCOM> rfxcomTracker;
	private Map<String, String> addressSourceMapping = null;
	private Set<String> sourceIdFilter = null;
	private boolean collectAllSourceIds = true;
	private int collectAllSourceIdsTimeout = DEFAULT_COLLECT_ALL_SOURCE_IDS_TIMEOUT;
	private float voltage = DEFAULT_VOLTAGE;
	private int currentSensorIndexFlags = DEFAULT_CURRENT_SENSOR_INDEX_FLAGS;

	// some in-memory error correction support, map keys are source IDs
	private long maxWattHourWarmupVerificationDiff = DEFAULT_MAX_WATT_HOUR_WARMUP_VERIFICATION_DIFF;
	private long maxWattHourSpikeVerificationDiff = DEFAULT_MAX_WATT_HOUR_SPIKE_VERIFICATION_DIFF;
	private final Map<String, Long> previousWattHours = new HashMap<String, Long>();
	private final Map<String, List<ACEnergyDatum>> datumBuffer = new HashMap<String, List<ACEnergyDatum>>();

	// in-memory listing of "seen" addresses, to support device discovery
	private final SortedSet<AddressSource> knownAddresses = new ConcurrentSkipListSet<AddressSource>();

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
	public Class<? extends ACEnergyDatum> getMultiDatumType() {
		return GeneralNodeACEnergyDatum.class;
	}

	@Override
	public Class<? extends ACEnergyDatum> getDatumType() {
		return GeneralNodeACEnergyDatum.class;
	}

	private String getSourceIdForMessageAddress(String addr) {
		if ( getAddressSourceMapping() != null && getAddressSourceMapping().containsKey(addr) ) {
			addr = getAddressSourceMapping().get(addr);
		}
		return addr;
	}

	private GeneralNodeACEnergyDatum filterConsumptionDatumInstance(GeneralNodeACEnergyDatum d) {
		String addr = getSourceIdForMessageAddress(d.getSourceId());
		if ( getSourceIdFilter() != null && !getSourceIdFilter().contains(addr) ) {
			if ( log.isInfoEnabled() ) {
				log.info("Rejecting source [" + addr + "] not in source ID filter set");
			}
			return null;
		}

		// create a copy, because CurrentMessage might still be using input object for 
		// other sensors...
		GeneralNodeACEnergyDatum copy = (GeneralNodeACEnergyDatum) d.clone();
		copy.setSourceId(addr);
		copy.setCreated(new Date());
		return copy;
	}

	private List<ACEnergyDatum> getDatumBufferForSource(String source) {
		if ( !datumBuffer.containsKey(source) ) {
			datumBuffer.put(source, new ArrayList<ACEnergyDatum>(5));
		}
		return datumBuffer.get(source);
	}

	private void addToResultsCheckingData(GeneralNodeACEnergyDatum datum, List<ACEnergyDatum> results) {
		if ( datum == null ) {
			return;
		}
		final String sourceId = (datum.getSourceId() == null ? "" : datum.getSourceId());
		if ( sourceIdFilter != null && !sourceIdFilter.contains(sourceId) ) {
			return;
		}

		if ( datum.getWattHourReading() != null ) {
			// calculate what would be the Wh diff
			Long prevGoodWh = previousWattHours.get(sourceId);
			List<ACEnergyDatum> buffer = getDatumBufferForSource(sourceId);
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

	private void addConsumptionDatumFromMessage(Message msg, List<ACEnergyDatum> results) {
		final String address = ((AddressSource) msg).getAddress();
		if ( msg instanceof EnergyMessage ) {
			EnergyMessage emsg = (EnergyMessage) msg;
			GeneralNodeACEnergyDatum d = new GeneralNodeACEnergyDatum();
			d.setSourceId(address);
			final double wh = emsg.getUsageWattHours();
			final double w = emsg.getInstantWatts();
			if ( wh > 0 ) {
				d.setWattHourReading(Math.round(wh));
			}
			d.setWatts((int) Math.ceil(w));
			d = filterConsumptionDatumInstance(d);
			addToResultsCheckingData(d, results);
		} else {
			// assume CurrentMessage
			CurrentMessage cmsg = (CurrentMessage) msg;
			GeneralNodeACEnergyDatum d = new GeneralNodeACEnergyDatum();

			// we turn each sensor into its own ConsumptionDatum, the sensors we collect
			// from are specified by the currentSensorIndexFlags property
			for ( int i = 1; i <= 3; i++ ) {
				if ( (i & currentSensorIndexFlags) != i ) {
					continue;
				}
				d.setSourceId(address + "." + i);
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
				GeneralNodeACEnergyDatum filtered = filterConsumptionDatumInstance(d);
				addToResultsCheckingData(filtered, results);
			}
		}
	}

	@Override
	public ACEnergyDatum readCurrentDatum() {
		final Collection<ACEnergyDatum> results = readMultipleDatum();
		if ( results != null && results.size() > 0 ) {
			return results.iterator().next();
		}
		return null;
	}

	@Override
	public Collection<ACEnergyDatum> readMultipleDatum() {
		final RFXCOM r = getRfxcomTracker().service();
		if ( r == null ) {
			return null;
		}

		final List<Message> messages;
		final ConversationalDataCollector dc = r.getDataCollectorInstance();
		if ( dc == null ) {
			return null;
		}
		try {
			messages = dc.collectData(this);
		} finally {
			if ( dc != null ) {
				dc.stopCollecting();
			}
		}

		if ( messages == null ) {
			return null;
		}

		final List<ACEnergyDatum> results = new ArrayList<ACEnergyDatum>(messages.size());
		for ( Message msg : messages ) {
			addConsumptionDatumFromMessage(msg, results);
		}
		return results;
	}

	@Override
	public List<Message> conductConversation(ConversationalDataCollector dc) {
		final List<Message> result = new ArrayList<Message>(3);
		final long endTime = (isCollectAllSourceIds() && getSourceIdFilter() != null
				&& getSourceIdFilter().size() > 1
						? System.currentTimeMillis() + (getCollectAllSourceIdsTimeout() * 1000)
						: 0);
		final Set<String> sourceIdSet = new HashSet<String>(
				getSourceIdFilter() == null ? 0 : getSourceIdFilter().size());

		final MessageFactory mf = new MessageFactory();
		final MessageListener listener = new MessageListener();

		do {
			listener.reset();
			dc.listen(listener);
			byte[] data = dc.getCollectedData();
			if ( data == null ) {
				log.warn("Null serial data received, serial communications problem");
				return null;
			}
			Message msg = mf.parseMessage(data, 0);
			if ( msg instanceof AddressSource ) {
				// add a known address for this reading
				addKnownAddress((AddressSource) msg);
			}
			if ( msg instanceof EnergyMessage || msg instanceof CurrentMessage ) {
				final String sourceId = getSourceIdForMessageAddress(((AddressSource) msg).getAddress());
				if ( !sourceIdSet.contains(sourceId) ) {
					result.add(msg);
					sourceIdSet.add(sourceId);
				}
			}
		} while ( System.currentTimeMillis() < endTime
				&& sourceIdSet.size() < (getSourceIdFilter() == null ? 0 : getSourceIdFilter().size()) );
		return result;
	}

	@Override
	public String getSettingUID() {
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

		List<SettingSpecifier> results = new ArrayList<SettingSpecifier>(21);
		results.addAll(getIdentifiableSettingSpecifiers());
		results.add(new BasicTextFieldSettingSpecifier("rfxcomTracker.propertyFilters['UID']",
				"/dev/ttyUSB0"));

		StringBuilder status = new StringBuilder();
		for ( AddressSource datum : knownAddresses ) {
			if ( status.length() > 0 ) {
				status.append(",\n");
			}
			status.append(datum.toString());
		}
		results.add(new BasicTitleSettingSpecifier("knownAddresses", status.toString(), true));

		results.add(new BasicTextFieldSettingSpecifier("addressSourceMappingValue", ""));
		results.add(new BasicTextFieldSettingSpecifier("sourceIdFilterValue", ""));
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
	 */
	public void setAddressSourceMappingValue(String mapping) {
		setAddressSourceMapping(StringUtils.commaDelimitedStringToMap(mapping));
	}

	/**
	 * Set a {@link sourceIdFilter} List via an encoded String value.
	 * 
	 * <p>
	 * The format of the {@code filters} String should be a comma-delimited list
	 * of values. Whitespace is permitted around the commas, and will be
	 * stripped from the values.
	 * </p>
	 * 
	 * @param filters
	 */
	public void setSourceIdFilterValue(String filters) {
		setSourceIdFilter(StringUtils.commaDelimitedStringToSet(filters));
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

	public Set<String> getSourceIdFilter() {
		return sourceIdFilter;
	}

	public void setSourceIdFilter(Set<String> sourceIdFilter) {
		this.sourceIdFilter = sourceIdFilter;
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
