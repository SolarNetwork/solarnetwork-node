/* ==================================================================
 * MetricDaoCleanerJob.java - 15/07/2024 4:00:10 pm
 *
 * Copyright 2024 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.metrics.service;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.solarnetwork.node.job.JobService;
import net.solarnetwork.node.metrics.dao.BasicMetricFilter;
import net.solarnetwork.node.metrics.dao.MetricDao;
import net.solarnetwork.node.metrics.domain.Metric;
import net.solarnetwork.node.service.support.BaseIdentifiable;
import net.solarnetwork.settings.SettingSpecifier;
import net.solarnetwork.settings.support.BasicGroupSettingSpecifier;
import net.solarnetwork.settings.support.BasicTextFieldSettingSpecifier;
import net.solarnetwork.settings.support.SettingUtils;
import net.solarnetwork.util.ArrayUtils;
import net.solarnetwork.util.ObjectUtils;
import net.solarnetwork.util.StringUtils;

/**
 * Job to clean out old metrics.
 *
 * @author matt
 * @version 1.0
 */
public class MetricDaoCleanerJob extends BaseIdentifiable implements JobService {

	/**
	 * Cleaner configuration.
	 */
	public static final class MetricCleanConfig {

		/** The {@code types} default value. */
		public static final String DEFAULT_TYPE = Metric.METRIC_TYPE_SAMPLE;

		public static List<SettingSpecifier> settings(String prefix) {
			List<SettingSpecifier> results = new ArrayList<>(2);

			results.add(new BasicTextFieldSettingSpecifier(prefix + "ageDays", null));
			results.add(new BasicTextFieldSettingSpecifier(prefix + "typesValue", DEFAULT_TYPE));
			results.add(new BasicTextFieldSettingSpecifier(prefix + "namesValue", null));

			return results;
		}

		private Duration age;
		private Set<String> types = Collections.singleton(DEFAULT_TYPE);
		private Set<String> names;

		/**
		 * Test if the configuration is valid.
		 *
		 * @return {@literal true} if the configuration is valid
		 */
		public boolean isValid() {
			return (age != null);
		}

		/**
		 * Get the minimum age.
		 *
		 * @return the age
		 */
		public Duration getAge() {
			return age;
		}

		/**
		 * Set the minimum age.
		 *
		 * @param age
		 *        the age to set
		 */
		public void setAge(Duration age) {
			this.age = age;
		}

		/**
		 * Get the age in days.
		 *
		 * @return the age, in days
		 */
		public Long getAgeDays() {
			final Duration d = getAge();
			return (d != null ? d.toDays() : null);
		}

		/**
		 * Set the age in days.
		 *
		 * @param days
		 *        the age to set, in days
		 */
		public void setAgeDays(Long days) {
			setAge(days != null ? Duration.ofDays(days) : null);
		}

		/**
		 * Get the types.
		 *
		 * @return the types
		 */
		public Set<String> getTypes() {
			return types;
		}

		/**
		 * Set the types.
		 *
		 * @param types
		 *        the types to set
		 */
		public void setTypes(Set<String> types) {
			this.types = types;
		}

		/**
		 * Get the types as a comma-delimited string.
		 *
		 * @return the types
		 */
		public String getTypesValue() {
			return StringUtils.commaDelimitedStringFromCollection(types);
		}

		/**
		 * Set the types as a comma-delimited string.
		 *
		 * @param value
		 *        the types to set
		 */
		public void setTypesValue(String value) {
			setTypes(StringUtils.commaDelimitedStringToSet(value));
		}

		/**
		 * Get the names.
		 *
		 * @return the names
		 */
		public Set<String> getNames() {
			return names;
		}

		/**
		 * Set the names.
		 *
		 * @param names
		 *        the names to set
		 */
		public void setNames(Set<String> names) {
			this.names = names;
		}

		/**
		 * Get the names as a comma-delimited string.
		 *
		 * @return the names
		 */
		public String getNamesValue() {
			return StringUtils.commaDelimitedStringFromCollection(names);
		}

		/**
		 * Set the names as a comma-delimited string.
		 *
		 * @param value
		 *        the names to set
		 */
		public void setNamesValue(String value) {
			setNames(StringUtils.commaDelimitedStringToSet(value));
		}

	}

	private final Logger log = LoggerFactory.getLogger(MetricDaoCleanerJob.class);

	private final MetricDao metricDao;
	private MetricCleanConfig[] configs;

	/**
	 * Constructor.
	 *
	 * @param metricDao
	 *        the metric DAO
	 * @throws IllegalArgumentException
	 *         if any argument is {@literal null}
	 */
	public MetricDaoCleanerJob(MetricDao metricDao) {
		super();
		this.metricDao = ObjectUtils.requireNonNullArgument(metricDao, "metricDao");
	}

	@Override
	public String getSettingUid() {
		String uid = getUid();
		return (uid != null && !uid.isEmpty() ? uid : "net.solarnetwork.node.metrics.dao.cleaner");
	}

	@Override
	public List<SettingSpecifier> getSettingSpecifiers() {
		List<SettingSpecifier> result = new ArrayList<>(8);

		MetricCleanConfig[] propConfs = getConfigs();
		List<MetricCleanConfig> propConfList = (propConfs != null ? asList(propConfs) : emptyList());
		result.add(SettingUtils.dynamicListSettingSpecifier("configs", propConfList,
				new SettingUtils.KeyedListCallback<MetricCleanConfig>() {

					@Override
					public Collection<SettingSpecifier> mapListSettingKey(MetricCleanConfig value,
							int index, String key) {
						SettingSpecifier configGroup = new BasicGroupSettingSpecifier(
								MetricCleanConfig.settings(key + "."));
						return Collections.singletonList(configGroup);
					}
				}));

		return result;
	}

	@Override
	public void executeJobService() throws Exception {
		final MetricCleanConfig[] confs = getConfigs();
		if ( confs == null || confs.length < 1 ) {
			return;
		}
		for ( MetricCleanConfig c : confs ) {
			if ( c == null || !c.isValid() ) {
				continue;
			}

			BasicMetricFilter filter = new BasicMetricFilter();
			filter.setEndDate(Instant.now().minus(c.getAge()));
			if ( c.getTypes() != null && !c.getTypes().isEmpty() ) {
				filter.setTypes(c.getTypes().toArray(new String[c.getTypes().size()]));
			}
			if ( c.getNames() != null && !c.getNames().isEmpty() ) {
				filter.setNames(c.getNames().toArray(new String[c.getNames().size()]));
			}

			if ( log.isDebugEnabled() ) {
				log.debug("Deleting metrics older than [{}] matching types [{}] and names [{}]",
						c.getAge(), filter.hasTypeCriteria() ? c.getTypesValue() : "*",
						filter.hasNameCriteria() ? c.getNamesValue() : "*");
			}
			int result = metricDao.deleteFiltered(filter);
			if ( log.isInfoEnabled() && result > 0 ) {
				log.info("Deleted {} metrics older than [{}] matching types [{}] and names [{}]", result,
						c.getAge(), filter.hasTypeCriteria() ? c.getTypesValue() : "*",
						filter.hasNameCriteria() ? c.getNamesValue() : "*");
			}
		}
	}

	/**
	 * Get the configurations.
	 *
	 * @return the configurations
	 */
	public MetricCleanConfig[] getConfigs() {
		return configs;
	}

	/**
	 * Set the configurations to use.
	 *
	 * @param configs
	 *        the configurations to use
	 */
	public void setConfigs(MetricCleanConfig[] configs) {
		this.configs = configs;
	}

	/**
	 * Get the number of configured {@code configs} elements.
	 *
	 * @return the number of {@code configs} elements
	 */
	public int getConfigsCount() {
		MetricCleanConfig[] confs = this.configs;
		return (confs == null ? 0 : confs.length);
	}

	/**
	 * Adjust the number of configured {@code configs} elements.
	 *
	 * <p>
	 * Any newly added element values will be set to new
	 * {@link MetricHarvesterConfig} instances.
	 * </p>
	 *
	 * @param count
	 *        The desired number of {@code configs} elements.
	 */
	public void setConfigsCount(int count) {
		this.configs = ArrayUtils.arrayWithLength(this.configs, count, MetricCleanConfig.class,
				MetricCleanConfig::new);
	}

}
