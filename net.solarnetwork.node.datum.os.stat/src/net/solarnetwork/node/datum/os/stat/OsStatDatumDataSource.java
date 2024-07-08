/* ==================================================================
 * OsStatDatumDataSource.java - 10/08/2018 9:27:02 AM
 *
 * Copyright 2018 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.datum.os.stat;

import static java.util.stream.Collectors.toCollection;
import static net.solarnetwork.domain.datum.DatumSamplesType.Accumulating;
import static net.solarnetwork.domain.datum.DatumSamplesType.Instantaneous;
import static net.solarnetwork.domain.datum.DatumSamplesType.Status;
import static net.solarnetwork.util.DateUtils.formatForLocalDisplay;
import static net.solarnetwork.util.StringUtils.commaDelimitedStringFromCollection;
import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.springframework.context.MessageSource;
import net.solarnetwork.domain.datum.DatumSamplesOperations;
import net.solarnetwork.domain.datum.GeneralDatumMetadata;
import net.solarnetwork.node.domain.datum.MutableNodeDatum;
import net.solarnetwork.node.domain.datum.NodeDatum;
import net.solarnetwork.node.domain.datum.SimpleDatum;
import net.solarnetwork.node.service.DatumDataSource;
import net.solarnetwork.node.service.NodeMetadataService;
import net.solarnetwork.node.service.support.DatumDataSourceSupport;
import net.solarnetwork.service.OptionalService;
import net.solarnetwork.service.PingTest;
import net.solarnetwork.service.PingTestResult;
import net.solarnetwork.settings.MappableSpecifier;
import net.solarnetwork.settings.SettingSpecifier;
import net.solarnetwork.settings.SettingSpecifierProvider;
import net.solarnetwork.settings.support.BasicTextFieldSettingSpecifier;
import net.solarnetwork.settings.support.BasicTitleSettingSpecifier;
import net.solarnetwork.support.PrefixedMessageSource;
import net.solarnetwork.util.CachedResult;
import net.solarnetwork.util.StringUtils;

/**
 * {@link DatumDataSource} for OS statistics obtained from a helper command.
 *
 * <p>
 * The command-line helper program this has been designed for is the
 * {@literal solarstats.sh} bash script, but any program will work as long as it
 * follows this syntax:
 * </p>
 *
 * <pre>
 * prog action
 * </pre>
 *
 * <p>
 * Each invocation should return a comma-delimited list of header names followed
 * by any number of comma-delimited lines of data.
 * </p>
 *
 * <p>
 * The following actions are assumed:
 * </p>
 *
 * <dl>
 * <dt>{@literal cpu-use}</dt>
 * <dd>print CPU utilization statistics, with the following columns supported:
 * date, period-secs, user, system, idle. The date is assumed to be in
 * {@literal YYYY-MM-dd HH:mm:ss UTC} form. The remaining columns are decimal
 * numbers representing percentage utilization.</dd>
 *
 * <dt>{@literal fs-use}</dt>
 * <dd>print file system utilization, with the following columns supported:
 * mount, size-kb, used-kb, used-percent.</dd>
 *
 * <dt>{@literal net-traffic}</dt>
 * <dd>print network traffic statistics, with the following columns supported:
 * device, bytes-in, bytes-out, packets-in, packets-out</dd>
 *
 * <dt>{@literal sys-load}</dt>
 * <dd>print system average load information, with the following columns
 * supported: 1min, 5min, 15min</dd>
 *
 * <dt>{@literal sys-up}</dt>
 * <dd>print the system uptime, with the following columns supported:
 * up-sec</dd>
 * </dl>
 *
 * <p>
 * Other actions can be used, as long as the supporting OS script also supports
 * them and returns CSV data as outlined previously. By default, <i>status</i>
 * properties will be populated on the datum. To populate <i>instantaneous</i>
 * or <i>accumulating</i> properties the column name should be prefixed with
 * {@code i/} or {@code a/}, respectively. The actual property name used on the
 * datum will be stripped of this prefix. For example, an action
 * {@literal cpu-temp} could populate a "cpu_temp" instantaneous property if the
 * OS script returned the following output when called with that action:
 * </p>
 *
 * <pre>
 * <code>
 * i/cpu_temp
 * 30.1
 * </code>
 * </pre>
 *
 * @author matt
 * @version 1.5
 */
public class OsStatDatumDataSource extends DatumDataSourceSupport
		implements DatumDataSource, SettingSpecifierProvider, PingTest {

	/** The {@code fsUseWarningThreshold} property default value. */
	public static final float DEFAULT_FS_USE_WARNING = 0.92f;

	/** The {@code sampleCacheMs} property default value. */
	public static final long DEFAULT_SAMPLE_CACHE_MS = 20_000L;

	/** The {@code fsUseMounts} property default value. */
	public static final List<String> DEFAULT_FS_USE_MOUNTS = Arrays.asList("/", "/run");

	/** The {@code netDevices} property default value. */
	public static final List<String> DEFAULT_NET_DEVICES = Arrays.asList("eth0");

	private final AtomicReference<CachedResult<NodeDatum>> sampleCache = new AtomicReference<>();

	private Set<String> actions = StatAction.ALL_ACTIONS;
	private ActionCommandRunner commandRunner = new ProcessActionCommandRunner();
	private Set<String> fsUseMounts = new LinkedHashSet<>(DEFAULT_FS_USE_MOUNTS);
	private Set<String> netDevices = new LinkedHashSet<>(DEFAULT_NET_DEVICES);
	private long sampleCacheMs = DEFAULT_SAMPLE_CACHE_MS;
	private String sourceId;
	private OptionalService<NodeMetadataService> nodeMetadataService;
	private float fsUseWarningThreshold = DEFAULT_FS_USE_WARNING;

	/**
	 * Constructor.
	 */
	public OsStatDatumDataSource() {
		super();
	}

	@Override
	public Class<? extends NodeDatum> getDatumType() {
		return NodeDatum.class;
	}

	@Override
	public NodeDatum readCurrentDatum() {
		NodeDatum d = getCurrentSample();
		if ( d != null ) {
			updateNodeMetadata();
		}
		return d;
	}

	private NodeDatum getCurrentSample() {
		// First check for a cached sample
		CachedResult<NodeDatum> cache = sampleCache.get();
		if ( cache != null && cache.isValid() ) {
			return cache.getResult();
		}

		// Cache has expired so initiate new instance and cache
		final String sourceId = resolvePlaceholders(this.sourceId);
		if ( sourceId == null || sourceId.isEmpty() ) {
			return null;
		}
		SimpleDatum result = SimpleDatum.nodeDatum(sourceId);

		for ( String action : actions ) {
			List<Map<String, String>> data = commandRunner.executeAction(action);
			populateActionData(action, data, result);
		}

		if ( result != null ) {
			sampleCache.compareAndSet(cache,
					new CachedResult<>(result, sampleCacheMs, TimeUnit.MILLISECONDS));
		}
		return result;
	}

	private void populateActionData(String action, List<Map<String, String>> data,
			MutableNodeDatum result) {
		if ( data == null || data.isEmpty() ) {
			return;
		}
		StatAction stdAction = null;
		try {
			stdAction = StatAction.forAction(action);
		} catch ( IllegalArgumentException e ) {
			// ignore
		}
		if ( stdAction == null ) {
			populateGeneralActionResults(action, data, result);
			return;
		}
		switch (stdAction) {
			case CpuUse:
				populateCpuUse(data, result);
				break;

			case FilesystemUse:
				populateFilesystemUse(data, result);
				break;

			case MemoryUse:
				populateMemoryUse(data, result);
				break;

			case NetworkTraffic:
				populateNetworkTraffic(data, result);
				break;

			case SystemLoad:
				populateSystemLoad(data, result);
				break;

			case SystemUptime:
				populateSystemUptime(data, result);
				break;
		}

	}

	private void populateInstantaneousValue(StatAction action, Map<String, String> row, String key,
			String propName, MutableNodeDatum result, BigDecimal scaleFactor) {
		populateInstantaneousValue(action.getAction(), row, key, propName, result, scaleFactor);
	}

	private void populateInstantaneousValue(String action, Map<String, String> row, String key,
			String propName, MutableNodeDatum result, BigDecimal scaleFactor) {
		BigDecimal d = scaledNumber(action, key, row.get(key), scaleFactor);
		result.asMutableSampleOperations().putSampleValue(Instantaneous, propName, d);
	}

	private void populateAccumulatingValue(StatAction action, Map<String, String> row, String key,
			String propName, MutableNodeDatum result, BigDecimal scaleFactor) {
		populateAccumulatingValue(action.getAction(), row, key, propName, result, scaleFactor);
	}

	private void populateAccumulatingValue(String action, Map<String, String> row, String key,
			String propName, MutableNodeDatum result, BigDecimal scaleFactor) {
		BigDecimal d = scaledNumber(action, key, row.get(key), scaleFactor);
		result.asMutableSampleOperations().putSampleValue(Accumulating, propName, d);
	}

	private void populateStatusValue(Map<String, String> row, String key, String propName,
			MutableNodeDatum result) {
		result.asMutableSampleOperations().putSampleValue(Status, propName, row.get(key));
	}

	private void populateGeneralActionResults(String action, List<Map<String, String>> data,
			MutableNodeDatum result) {
		Map<String, String> row = data.get(0);
		for ( Map.Entry<String, String> me : row.entrySet() ) {
			final String key = me.getKey();
			final int idx = key.indexOf('/');
			String propName = key;
			String groupKey = null;
			if ( idx > 0 && idx + 1 < key.length() ) {
				// treat text before / as i,a,s prefix
				groupKey = key.substring(0, idx);
				propName = key.substring(idx + 1);
			}
			switch (groupKey) {
				case "i":
					populateInstantaneousValue(action, row, key, propName, result, null);
					break;

				case "a":
					populateAccumulatingValue(action, row, key, propName, result, null);
					break;

				default:
					populateStatusValue(row, key, propName, result);
					break;
			}
		}
	}

	private void populateCpuUse(List<Map<String, String>> data, MutableNodeDatum result) {
		// use only last available row, ignore date,period-secs
		Map<String, String> row = data.get(data.size() - 1);
		populateInstantaneousValue(StatAction.CpuUse, row, "user", "cpu_user", result, null);
		populateInstantaneousValue(StatAction.CpuUse, row, "system", "cpu_system", result, null);
		populateInstantaneousValue(StatAction.CpuUse, row, "idle", "cpu_idle", result, null);
	}

	private void populateFilesystemUse(List<Map<String, String>> data, MutableNodeDatum result) {
		final BigDecimal kb = new BigDecimal("1024");
		for ( Map<String, String> row : data ) {
			String mount = row.get("mount");
			if ( mount == null || !fsUseMounts.contains(mount) ) {
				continue;
			}
			populateInstantaneousValue(StatAction.FilesystemUse, row, "size-kb", "fs_size_" + mount,
					result, kb);
			populateInstantaneousValue(StatAction.FilesystemUse, row, "used-kb", "fs_used_" + mount,
					result, kb);
			populateInstantaneousValue(StatAction.FilesystemUse, row, "used-percent",
					"fs_used_percent_" + mount, result, null);
		}
	}

	private void populateMemoryUse(List<Map<String, String>> data, MutableNodeDatum result) {
		final BigDecimal kb = new BigDecimal("1024");
		Map<String, String> row = data.get(0);
		populateInstantaneousValue(StatAction.MemoryUse, row, "total-kb", "ram_total", result, kb);
		populateInstantaneousValue(StatAction.MemoryUse, row, "avail-kb", "ram_avail", result, kb);
		BigDecimal total = result.asSampleOperations().getSampleBigDecimal(Instantaneous, "ram_total");
		BigDecimal avail = result.asSampleOperations().getSampleBigDecimal(Instantaneous, "ram_avail");
		if ( total != null && avail != null ) {
			result.asMutableSampleOperations().putSampleValue(Instantaneous, "ram_used_percent",
					total.subtract(avail).divide(total, 3, RoundingMode.HALF_UP)
							.multiply(new BigDecimal("100"), new MathContext(3)));
		}
	}

	private void populateSystemLoad(List<Map<String, String>> data, MutableNodeDatum result) {
		Map<String, String> row = data.get(0);
		populateInstantaneousValue(StatAction.SystemLoad, row, "1min", "sys_load_1min", result, null);
		populateInstantaneousValue(StatAction.SystemLoad, row, "5min", "sys_load_5min", result, null);
		populateInstantaneousValue(StatAction.SystemLoad, row, "15min", "sys_load_15min", result, null);
	}

	private void populateSystemUptime(List<Map<String, String>> data, MutableNodeDatum result) {
		Map<String, String> row = data.get(0);
		populateAccumulatingValue(StatAction.SystemUptime, row, "up-sec", "sys_up", result, null);
	}

	private void populateNetworkTraffic(List<Map<String, String>> data, MutableNodeDatum result) {
		for ( Map<String, String> row : data ) {
			String dev = row.get("device");
			if ( dev == null || !netDevices.contains(dev) ) {
				continue;
			}
			populateAccumulatingValue(StatAction.NetworkTraffic, row, "bytes-in", "net_bytes_in_" + dev,
					result, null);
			populateAccumulatingValue(StatAction.NetworkTraffic, row, "bytes-out",
					"net_bytes_out_" + dev, result, null);
			populateAccumulatingValue(StatAction.NetworkTraffic, row, "packets-in",
					"net_packets_in_" + dev, result, null);
			populateAccumulatingValue(StatAction.NetworkTraffic, row, "packets-out",
					"net_packets_out_" + dev, result, null);
		}
	}

	private void updateNodeMetadata() {
		NodeMetadataService service = OptionalService.service(nodeMetadataService);
		if ( service == null ) {
			return;
		}
		GeneralDatumMetadata meta = new GeneralDatumMetadata();
		Map<String, String> props = getJavaOsSystemProperties();
		for ( Map.Entry<String, String> me : props.entrySet() ) {
			meta.putInfoValue("os", me.getKey(), me.getValue());
		}
		try {
			service.addNodeMetadata(meta);
		} catch ( Exception e ) {
			Throwable root = e;
			while ( root.getCause() != null ) {
				root = root.getCause();
			}
			log.warn("Error publishing OS stats node metadata: {}", root.toString());
		}
	}

	private Map<String, String> getJavaOsSystemProperties() {
		Map<String, String> result = new LinkedHashMap<>();
		result.put("name", System.getProperty("os.name"));
		result.put("arch", System.getProperty("os.arch"));
		result.put("version", System.getProperty("os.version"));
		return result;
	}

	@Override
	public String getSettingUid() {
		return "net.solarnetwork.node.datum.os.stat";
	}

	@Override
	public String getDisplayName() {
		return "OS Statistics Data Source";
	}

	@Override
	public MessageSource getMessageSource() {
		MessageSource source = super.getMessageSource();
		if ( source != null ) {
			if ( commandRunner instanceof SettingSpecifierProvider ) {
				SettingSpecifierProvider runProvider = (SettingSpecifierProvider) commandRunner;
				MessageSource runSource = runProvider.getMessageSource();
				if ( runSource != null ) {
					PrefixedMessageSource pSource = new PrefixedMessageSource();
					pSource.setPrefix("commandRunner.");
					pSource.setDelegate(runSource);
					pSource.setParentMessageSource(source);
					source = pSource;
				}
			}
		}
		return source;
	}

	@Override
	public List<SettingSpecifier> getSettingSpecifiers() {
		List<SettingSpecifier> result = getIdentifiableSettingSpecifiers();

		try {
			String info = getInfoMessage();
			if ( info != null ) {
				result.add(0, new BasicTitleSettingSpecifier("info", info, true));
			}
		} catch ( RuntimeException e ) {
			// ignore this
		}

		result.add(new BasicTextFieldSettingSpecifier("sampleCacheMs",
				String.valueOf(DEFAULT_SAMPLE_CACHE_MS)));
		result.add(new BasicTextFieldSettingSpecifier("sourceId", null));
		result.add(new BasicTextFieldSettingSpecifier("actionsValue",
				commaDelimitedStringFromCollection(StatAction.ALL_ACTIONS)));
		result.add(new BasicTextFieldSettingSpecifier("fsUseMountsValue",
				commaDelimitedStringFromCollection(DEFAULT_FS_USE_MOUNTS)));
		result.add(new BasicTextFieldSettingSpecifier("fsUseWarningThreshold",
				String.valueOf(DEFAULT_FS_USE_WARNING)));
		result.add(new BasicTextFieldSettingSpecifier("netDevicesValue",
				commaDelimitedStringFromCollection(DEFAULT_NET_DEVICES)));

		if ( commandRunner instanceof SettingSpecifierProvider ) {
			SettingSpecifierProvider runProvider = (SettingSpecifierProvider) commandRunner;
			List<SettingSpecifier> runSpecifiers = runProvider.getSettingSpecifiers();
			if ( runSpecifiers != null && runSpecifiers.size() > 0 ) {
				for ( SettingSpecifier spec : runSpecifiers ) {
					if ( spec instanceof MappableSpecifier ) {
						result.add(((MappableSpecifier) spec).mappedTo("commandRunner."));
					} else {
						result.add(spec);
					}
				}
			}
		}

		return result;
	}

	private String getInfoMessage() {
		NodeDatum d = getCurrentSample();
		if ( d == null ) {
			return null;
		}
		DatumSamplesOperations s = d.asSampleOperations();
		if ( s == null ) {
			return null;
		}
		Map<String, Object> data = new LinkedHashMap<>(4);
		if ( s.hasSampleValue(Instantaneous, "cpu_user") ) {
			data.put("CPU", String.format("%s%%", s.getSampleBigDecimal(Instantaneous, "cpu_user")));
		}
		if ( s.hasSampleValue(Instantaneous, "ram_used_percent") ) {
			data.put("RAM used",
					String.format("%s%%", s.getSampleBigDecimal(Instantaneous, "ram_used_percent")));
		}
		if ( s.hasSampleValue(Instantaneous, "sys_load_5min") ) {
			data.put("Load (5 min)",
					String.format("%s", s.getSampleBigDecimal(Instantaneous, "sys_load_5min")));
		}
		if ( s.hasSampleValue(Instantaneous, "fs_used_percent_/") ) {
			data.put("Root disk use",
					String.format("%s%%", s.getSampleBigDecimal(Instantaneous, "fs_used_percent_/")));
		}
		if ( s.hasSampleValue(Instantaneous, "fs_used_percent_/run") ) {
			data.put("RAM disk use",
					String.format("%s%%", s.getSampleBigDecimal(Instantaneous, "fs_used_percent_/run")));
		}
		if ( s.hasSampleValue(Accumulating, "sys_up") ) {
			BigDecimal upSecs = s.getSampleBigDecimal(Accumulating, "sys_up");
			Duration dur = Duration.ofSeconds(upSecs.longValue());
			Instant bootDate = Instant.now().minusSeconds(upSecs.longValue());
			String bootDateStr = formatForLocalDisplay(bootDate);
			data.put("Up since", String.format("%s (%d days ago)", bootDateStr, dur.toDays()));
		}
		return StringUtils.delimitedStringFromMap(data, " = ", ", ");

	}

	private BigDecimal scaledNumber(String action, String key, String val, BigDecimal scaleFactor) {
		BigDecimal d = null;
		try {
			d = new BigDecimal(val);
			if ( scaleFactor != null ) {
				d = d.multiply(scaleFactor);
			}
			return d;
		} catch ( NullPointerException e ) {
			// ignore, property not available
		} catch ( NumberFormatException e ) {
			log.debug("Error parsing {} action {} value [{}]: {}", action, key, val, e.getMessage());
		}
		return null;
	}

	@Override
	public Collection<String> publishedSourceIds() {
		final String sourceId = resolvePlaceholders(this.sourceId);
		return (sourceId == null || sourceId.isEmpty() ? Collections.emptySet()
				: Collections.singleton(sourceId));
	}

	@Override
	public String getPingTestId() {
		String settingUid = getSettingUid();
		String uid = getUid();
		if ( uid == null || uid.isEmpty() ) {
			return settingUid;
		}
		return settingUid + "-" + uid;
	}

	@Override
	public String getPingTestName() {
		return getDisplayName();
	}

	@Override
	public long getPingTestMaximumExecutionMilliseconds() {
		return 10000;
	}

	@Override
	public Result performPingTest() throws Exception {
		List<Map<String, String>> data = null;
		boolean ok = true;
		String msg = null;
		Map<String, BigDecimal> props = new LinkedHashMap<>(4);
		final String action = StatAction.FilesystemUse.getAction();
		final float warnThreshold = getFsUseWarningThreshold();
		try {
			data = commandRunner.executeAction(action);
			if ( data != null ) {
				for ( Map<String, String> row : data ) {
					String mount = row.get("mount");
					if ( mount == null || !fsUseMounts.contains(mount) ) {
						continue;
					}
					BigDecimal sizeKb = scaledNumber(action, "size-kb", row.get("size-kb"), null);
					BigDecimal usedKb = scaledNumber(action, "used-kb", row.get("used-kb"), null);
					BigDecimal usedPercent = scaledNumber(action, "used-percent",
							row.get("used-percent"), null);
					if ( usedPercent != null ) {
						props.put("fs_used_percent_" + mount, usedPercent);
						String m = super.getMessageSource().getMessage("msg.fsUse",
								new Object[] { mount, usedPercent, sizeKb, usedKb },
								Locale.getDefault());
						if ( msg == null ) {
							msg = m;
						} else {
							msg = msg + " " + m;
						}
						float usedPercentFloat = usedPercent.floatValue() / 100.0f;
						if ( warnThreshold > 0f && usedPercentFloat >= warnThreshold ) {
							ok = false;
						}
					}
				}

			}
		} catch ( Exception e ) {
			msg = super.getMessageSource().getMessage("msg.action.exception",
					new Object[] { StatAction.FilesystemUse.getAction(), e.toString() },
					Locale.getDefault());
		}
		return new PingTestResult(ok, msg, props);
	}

	/**
	 * Get the command runner.
	 *
	 * @return the command runner; defaults to a
	 *         {@link ProcessActionCommandRunner} instance
	 */
	public ActionCommandRunner getCommandRunner() {
		return commandRunner;
	}

	/**
	 * Set the command runner to use.
	 *
	 * @param commandRunner
	 *        the runner to use
	 * @throws IllegalArgumentException
	 *         if {@code commandRunner} is {@literal null}
	 */
	public void setCommandRunner(ActionCommandRunner commandRunner) {
		if ( commandRunner == null ) {
			throw new IllegalArgumentException("The commandRunner is required.");
		}
		this.commandRunner = commandRunner;
	}

	/**
	 * Set the maximum time to cache sampled data for.
	 *
	 * @param sampleCacheMs
	 *        the sample cache time, in milliseconds
	 */
	public void setSampleCacheMs(long sampleCacheMs) {
		this.sampleCacheMs = sampleCacheMs;
	}

	/**
	 * Configure a {@link NodeMetadataService} to publish OS system information
	 * to.
	 *
	 * @param nodeMetadataService
	 *        the node metadata service to use
	 */
	public void setNodeMetadataService(OptionalService<NodeMetadataService> nodeMetadataService) {
		this.nodeMetadataService = nodeMetadataService;
	}

	/**
	 * Set the list of actions to perform and gather statistics from.
	 *
	 * @param actions
	 *        the actions to perform
	 */
	public void setActions(Set<StatAction> actions) {
		Set<String> s = null;
		if ( actions != null ) {
			s = actions.stream().map(e -> e.getAction()).collect(toCollection(LinkedHashSet::new));
			actions = Collections.emptySet();
		}
		setActionSet(s);
	}

	/**
	 * Set the action set of actions to perform and gather statistics from.
	 *
	 * @param actions
	 *        the actions
	 */
	public void setActionSet(Set<String> actions) {
		this.actions = (actions == null ? Collections.emptySet() : actions);
	}

	/**
	 * Get the list of actions to perform and gather statistics from, as a
	 * comma-delimited string.
	 *
	 * @return actions the actions to perform, as a comma-delimited string
	 */
	public String getActionsValue() {
		return StringUtils.commaDelimitedStringFromCollection(actions);
	}

	/**
	 * Set the actions to include, in the form of a comma-delimited list.
	 *
	 * @param keys
	 *        the action values, as a comma-delimited list
	 * @see #setActions(Set)
	 */
	public void setActionsValue(String keys) {
		setActionSet(StringUtils.commaDelimitedStringToSet(keys));
	}

	/**
	 * Set the filesystem mount points (paths) to include in filesystem
	 * statistics.
	 *
	 * <p>
	 * This value defaults to a set with {@literal /}, {@literal /run}.
	 * </p>
	 *
	 * @param fsUseMounts
	 *        the filesystem mount points to include
	 */
	public void setFsUseMounts(Set<String> fsUseMounts) {
		if ( fsUseMounts == null ) {
			fsUseMounts = Collections.emptySet();
		}
		this.fsUseMounts = fsUseMounts;
	}

	/**
	 * Get the network filesystem mount points (paths) to include in filesystem
	 * statistics, as a comma-delimited string.
	 *
	 * @return the comma-delimited list of filesystem mount points
	 */
	public String getFsUseMountsValue() {
		return StringUtils.commaDelimitedStringFromCollection(fsUseMounts);
	}

	/**
	 * Set the filesystem mount points (paths) to include in filesystem
	 * statistics, in the form of a comma-delimited list.
	 *
	 * @param mounts
	 *        the comma-delimited list of mounts to include
	 * @see #setFsUseMounts(Set)
	 */
	public void setFsUseMountsValue(String mounts) {
		setFsUseMounts(net.solarnetwork.util.StringUtils.commaDelimitedStringToSet(mounts));
	}

	/**
	 * Set the network device names to include in network traffic statistics.
	 *
	 * <p>
	 * This value defaults to a set with {@literal eht0}, {@literal wlan0}.
	 * </p>
	 *
	 * @param netDevices
	 *        the devices to include
	 */
	public void setNetDevices(Set<String> netDevices) {
		if ( netDevices == null ) {
			netDevices = Collections.emptySet();
		}
		this.netDevices = netDevices;
	}

	/**
	 * Get the network device names to include in network traffic statistics, as
	 * a comma-delimited string.
	 *
	 * @return the comma-delimited list of network device names
	 */
	public String getNetDevicesValue() {
		return StringUtils.commaDelimitedStringFromCollection(netDevices);
	}

	/**
	 * Set the network device names to include in network traffic statistics, in
	 * the form of a comma-delimited list.
	 *
	 * @param devices
	 *        the comma-delimited list of devices to include
	 * @see #setNetDevices(Set)
	 */
	public void setNetDevicesValue(String devices) {
		setNetDevices(net.solarnetwork.util.StringUtils.commaDelimitedStringToSet(devices));
	}

	/**
	 * Set the source ID to assign to captured datum.
	 *
	 * @param sourceId
	 *        the source ID
	 */
	public void setSourceId(String sourceId) {
		if ( sourceId == null || sourceId.isEmpty() ) {
			throw new IllegalArgumentException("The sourceId property is required");
		}
		this.sourceId = sourceId;
	}

	/**
	 * Get the filesystem use percentage threshold for triggering a ping test
	 * failure.
	 *
	 * @return the threshold; defaults to {@link #DEFAULT_FS_USE_WARNING}
	 */
	public float getFsUseWarningThreshold() {
		return fsUseWarningThreshold;
	}

	/**
	 * Set the filesystem use percentage threshold for triggering a ping test
	 * failure.
	 *
	 * @param fsUseWarningThreshold
	 *        the threshold to set, or {@literal 0} to disable
	 */
	public void setFsUseWarningThreshold(float fsUseWarningThreshold) {
		this.fsUseWarningThreshold = fsUseWarningThreshold;
	}

}
