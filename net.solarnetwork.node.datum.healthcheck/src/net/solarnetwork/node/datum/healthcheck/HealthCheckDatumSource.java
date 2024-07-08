/* ==================================================================
 * HealthCheckDatumSource.java - 14/12/2021 4:51:05 PM
 *
 * Copyright 2021 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.datum.healthcheck;

import static java.util.Collections.singleton;
import static net.solarnetwork.node.domain.datum.SimpleDatum.nodeDatum;
import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import net.solarnetwork.domain.CodedValue;
import net.solarnetwork.domain.datum.DatumSamples;
import net.solarnetwork.node.domain.datum.NodeDatum;
import net.solarnetwork.node.service.MultiDatumDataSource;
import net.solarnetwork.node.service.SystemHealthService;
import net.solarnetwork.node.service.SystemHealthService.PingTestResults;
import net.solarnetwork.node.service.support.DatumDataSourceSupport;
import net.solarnetwork.service.PingTestResultDisplay;
import net.solarnetwork.settings.SettingSpecifier;
import net.solarnetwork.settings.SettingSpecifierProvider;
import net.solarnetwork.settings.support.BasicMultiValueSettingSpecifier;
import net.solarnetwork.settings.support.BasicTextFieldSettingSpecifier;
import net.solarnetwork.settings.support.SettingUtils;

/**
 * Datum source for health check information.
 *
 * @author matt
 * @version 1.2
 */
public class HealthCheckDatumSource extends DatumDataSourceSupport
		implements MultiDatumDataSource, SettingSpecifierProvider {

	/** The datum status property for an overall success boolean value. */
	public static final String PROP_SUCCESS = "ok";

	/** The datum status property for a message string value. */
	public static final String PROP_MESSAGE = "msg";

	/** The maximum length allowed for a source ID. */
	public static final int SOURCE_ID_MAX_LENGTH = 64;

	/** The {@code publishMode} property default value. */
	public static final PublishMode DEFAULT_PUBLISH_MODE = PublishMode.OnChange;

	/** The {@code unchangedPublishMaxSeconds} property default value. */
	public static final int DEFAULT_UNCHANGED_PUBLISH_MAX_SECONDS = 3599;

	private static final Pattern RESERVED_SOURCE_ID_CHARACTERS = Pattern.compile("[.*#+?]");

	private static final String NET_SOLARNETWORK_PREFIX = "net.solarnetwork.";

	// status map for all instances, to track changes
	private final Map<String, PingTestResultDisplay> resultStatusMap = new HashMap<>(32);

	private final SystemHealthService systemHealthService;
	private String sourceId;
	private PublishMode publishMode = DEFAULT_PUBLISH_MODE;
	private int unchangedPublishMaxSeconds = DEFAULT_UNCHANGED_PUBLISH_MAX_SECONDS;
	private String[] pingTestIdFilters;

	/**
	 * Constructor.
	 *
	 * @param systemHealthService
	 *        the service to use
	 * @throws IllegalArgumentException
	 *         if any argument is {@literal null}
	 */
	public HealthCheckDatumSource(SystemHealthService systemHealthService) {
		super();
		this.systemHealthService = requireNonNullArgument(systemHealthService, "systemHealthService");
	}

	@Override
	public Collection<String> publishedSourceIds() {
		final String sourceId = resolvePlaceholders(this.sourceId);
		return (sourceId == null || sourceId.isEmpty() ? Collections.emptySet()
				: Collections.singleton(sourceId));
	}

	@Override
	public Class<? extends NodeDatum> getMultiDatumType() {
		return NodeDatum.class;
	}

	@Override
	public Collection<NodeDatum> readMultipleDatum() {
		Set<String> idFilter = null;
		if ( pingTestIdFilters != null ) {
			idFilter = Arrays.stream(pingTestIdFilters).collect(Collectors.toSet());
		}
		PingTestResults results = systemHealthService.performPingTests(idFilter);
		if ( results == null ) {
			log.info("No ping test results returned; ID filter was {}", idFilter);
			return null;
		}
		Collection<NodeDatum> result = null;
		final String sourceId = resolvePlaceholders(this.sourceId);
		if ( sourceId == null || sourceId.isEmpty() ) {
			// return one Datum per ping test
			result = generateDatumCollection(results);
		} else {
			// merge all results into single datum
			NodeDatum datum = generateDatum(results, sourceId);
			if ( datum != null ) {
				result = singleton(datum);
			}
		}
		return result;
	}

	private List<NodeDatum> generateDatumCollection(PingTestResults results) {
		List<NodeDatum> list = new ArrayList<>(results.getResults().size());
		for ( PingTestResultDisplay result : results.getResults().values() ) {
			final boolean shouldPublish = trackResultStatus(result);
			if ( !shouldPublish ) {
				continue;
			}
			String sourceId = sourceIdForPingTestResult(result, SOURCE_ID_MAX_LENGTH);
			DatumSamples samples = new DatumSamples();
			samples.putStatusSampleValue(PROP_SUCCESS, result.isSuccess());
			if ( !result.isSuccess() ) {
				samples.putStatusSampleValue(PROP_MESSAGE, result.getMessage());
			}
			list.add(nodeDatum(sourceId, result.getStart(), samples));
		}
		return (list.isEmpty() ? null : list);
	}

	private boolean trackResultStatus(PingTestResultDisplay testResult) {
		final PingTestResultDisplay oldStatus;
		synchronized ( resultStatusMap ) {
			oldStatus = resultStatusMap.get(testResult.getPingTestId());

			boolean update = true; // maybe not if OnChange within threshold
			boolean result = true;
			final boolean changed = (oldStatus == null
					|| oldStatus.isSuccess() != testResult.isSuccess());
			final PublishMode mode = this.publishMode;
			final int unchangedMaxSecs = this.unchangedPublishMaxSeconds;
			switch (mode) {
				case OnChange:
					result = (changed || (unchangedMaxSecs > 0
							&& Duration.between(oldStatus.getStart(), testResult.getStart())
									.getSeconds() > unchangedMaxSecs));
					update = result;
					break;

				case OnFailure:
					result = !testResult.isSuccess();
					break;

				case Always:
					result = true;
					break;

				default:
					// should not be here
					throw new RuntimeException("Unsupported publish mode: " + mode);
			}

			if ( update ) {
				resultStatusMap.put(testResult.getPingTestId(), testResult);
			}
			return result;
		}
	}

	private String sourceIdForPingTestResult(final PingTestResultDisplay result, final int maxLength) {
		// expecting foo.bar.Bam-extra
		String[] components = result.getPingTestId().split("-", 2);
		StringBuilder buf = new StringBuilder();
		String c1 = components[0];
		if ( c1.startsWith(NET_SOLARNETWORK_PREFIX) ) {
			c1 = c1.substring(NET_SOLARNETWORK_PREFIX.length());
		}
		buf.append(RESERVED_SOURCE_ID_CHARACTERS.matcher(c1.replace('.', '/')).replaceAll("_"));
		if ( components.length > 1 ) {
			buf.append('/');
			buf.append(RESERVED_SOURCE_ID_CHARACTERS.matcher(components[1].replace('/', '_'))
					.replaceAll("_"));
		}
		if ( maxLength > 0 ) {
			if ( buf.length() > maxLength ) {
				buf.setLength(maxLength);
			}
		}
		return buf.toString();
	}

	private NodeDatum generateDatum(PingTestResults results, String sourceId) {
		DatumSamples samples = new DatumSamples();
		boolean shouldPublishOverall = false;
		List<PingTestResultDisplay> skippedResults = new ArrayList<>();
		for ( PingTestResultDisplay result : results.getResults().values() ) {
			final boolean shouldPublish = trackResultStatus(result);
			if ( !(shouldPublish || shouldPublishOverall) ) {
				skippedResults.add(result);
				continue;
			} else {
				// backfill any skipped results now that we know we're publishing
				for ( PingTestResultDisplay skipped : skippedResults ) {
					populateDatumProperties(samples, skipped);
				}
				shouldPublishOverall = true;
			}
			populateDatumProperties(samples, result);
		}
		if ( !shouldPublishOverall ) {
			return null;
		}
		samples.putStatusSampleValue(PROP_SUCCESS, results.isAllGood());
		return nodeDatum(sourceId, results.getDate(), samples);
	}

	private void populateDatumProperties(DatumSamples samples, PingTestResultDisplay result) {
		String pingId = sourceIdForPingTestResult(result, -1);
		samples.putStatusSampleValue(pingId + "_" + PROP_SUCCESS, result.isSuccess());
		if ( !result.isSuccess() ) {
			samples.putStatusSampleValue(pingId + "_" + PROP_MESSAGE, result.getMessage());
		}
	}

	@Override
	public String getSettingUid() {
		return "net.solarnetwork.node.datum.healthcheck";
	}

	@Override
	public List<SettingSpecifier> getSettingSpecifiers() {
		List<SettingSpecifier> results = getIdentifiableSettingSpecifiers();

		results.add(new BasicTextFieldSettingSpecifier("sourceId", null));

		// menu for publishMode
		BasicMultiValueSettingSpecifier pubModeSpec = new BasicMultiValueSettingSpecifier(
				"publishModeCode", Character.toString((char) DEFAULT_PUBLISH_MODE.getCode()));
		Map<String, String> pubModeTitles = new LinkedHashMap<>(3);
		for ( PublishMode e : PublishMode.values() ) {
			String title = getMessageSource().getMessage(String.format("publishMode.%c", e.getCode()),
					null, Locale.getDefault());
			pubModeTitles.put(Character.toString((char) e.getCode()), title);
		}
		pubModeSpec.setValueTitles(pubModeTitles);
		results.add(pubModeSpec);

		results.add(new BasicTextFieldSettingSpecifier("unchangedPublishMaxSeconds",
				String.valueOf(DEFAULT_UNCHANGED_PUBLISH_MAX_SECONDS)));

		// menu for pingTestIdFilters
		String[] filters = getPingTestIdFilters();
		List<String> filtersList = (filters != null ? Arrays.asList(filters)
				: Collections.<String> emptyList());
		results.add(SettingUtils.dynamicListSettingSpecifier("pingTestIdFilters", filtersList,
				new SettingUtils.KeyedListCallback<String>() {

					@Override
					public Collection<SettingSpecifier> mapListSettingKey(String value, int index,
							String key) {
						return Collections.<SettingSpecifier> singletonList(
								new BasicTextFieldSettingSpecifier(key, ""));
					}
				}));

		return results;
	}

	/**
	 * Get the ping test ID expressions to filter on.
	 *
	 * @return the patterns to limit reported ping test information on, or
	 *         {@literal null} for all tests
	 */
	public String[] getPingTestIdFilters() {
		return pingTestIdFilters;
	}

	/**
	 * Set the ping test ID expressions to filter on.
	 *
	 * <p>
	 * These expressions are treated as regular expressions, in a
	 * case-insensitive manner matching any part of a ping test ID value.
	 * </p>
	 *
	 * @param pingTestIdFilters
	 *        the patterns to limit reported ping test information on, or
	 *        {@literal null} for all tests
	 */
	public void setPingTestIdFilters(String[] pingTestIdFilters) {
		this.pingTestIdFilters = pingTestIdFilters;
	}

	/**
	 * Get the number of configured {@code pingTestIdFilters} elements.
	 *
	 * @return The number of {@code pingTestIdFilters} elements.
	 */
	public int getPingTestIdFiltersCount() {
		String[] pats = this.pingTestIdFilters;
		return (pats == null ? 0 : pats.length);
	}

	/**
	 * Adjust the number of configured {@code pingTestIdFilters} elements. Any
	 * newly added element values will be {@literal null}.
	 *
	 * @param count
	 *        The desired number of {@code pingTestIdFilters} elements.
	 */
	public void setPingTestIdFiltersCount(int count) {
		if ( count < 0 ) {
			count = 0;
		}
		String[] pats = this.pingTestIdFilters;
		int lCount = (pats == null ? 0 : pats.length);
		if ( lCount != count ) {
			String[] newPats = new String[count];
			if ( pats != null ) {
				System.arraycopy(pats, 0, newPats, 0, Math.min(count, pats.length));
			}
			this.pingTestIdFilters = newPats;
		}
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

	/**
	 * Get the publish mode.
	 *
	 * @return the mode, never {@literal null}
	 */
	public PublishMode getPublishMode() {
		return publishMode;
	}

	/**
	 * Set the publish mode.
	 *
	 * @param publishMode
	 *        the mode to publish datum; if {@literal null} then
	 *        {@link #DEFAULT_PUBLISH_MODE} will be set instead
	 */
	public void setPublishMode(PublishMode publishMode) {
		this.publishMode = (publishMode != null ? publishMode : DEFAULT_PUBLISH_MODE);
	}

	/**
	 * Get the publish mode code value.
	 *
	 * @return the publish mode code
	 */
	public int getPublishModeCode() {
		return publishMode.getCode();
	}

	/**
	 * Set the publish mode as a code value.
	 *
	 * @param code
	 *        the publish mode code; if unsupported then
	 *        {@link #DEFAULT_PUBLISH_MODE} will be set instead
	 */
	public void setPublishModeCode(int code) {
		setPublishMode(CodedValue.forCodeValue(code, PublishMode.class, DEFAULT_PUBLISH_MODE));
	}

	/**
	 * Get the unchanged publish maximum seconds.
	 *
	 * @return the maximum seconds to refrain from publishing an unchanged
	 *         status value, or {@literal 0} for no limit
	 */
	public int getUnchangedPublishMaxSeconds() {
		return unchangedPublishMaxSeconds;
	}

	/**
	 * Set the unchanged publish maximum seconds.
	 *
	 * @param unchangedPublishMaxSeconds
	 *        the maximum seconds to refrain from publishing an unchanged status
	 *        value, or {@literal 0} for no limit
	 */
	public void setUnchangedPublishMaxSeconds(int unchangedPublishMaxSeconds) {
		this.unchangedPublishMaxSeconds = unchangedPublishMaxSeconds;
	}

}
