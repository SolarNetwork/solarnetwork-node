/* ===================================================================
 * DerbyMaintenanceRegistrationListener.java
 * 
 * Created Dec 6, 2009 1:03:43 PM
 * 
 * Copyright 2007-2009 SolarNetwork.net Dev Team
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
 * ===================================================================
 */

package net.solarnetwork.node.dao.jdbc.derby;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import org.osgi.framework.ServiceRegistration;
import org.quartz.CronTrigger;
import org.quartz.JobDetail;
import org.quartz.Scheduler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.scheduling.quartz.JobDetailFactoryBean;
import net.solarnetwork.node.dao.jdbc.JdbcDao;
import net.solarnetwork.node.job.RandomizedCronTriggerFactoryBean;
import net.solarnetwork.node.job.SimpleTriggerAndJobDetail;
import net.solarnetwork.node.job.TriggerAndJobDetail;
import net.solarnetwork.node.util.BaseServiceListener;
import net.solarnetwork.node.util.RegisteredService;

/**
 * An OSGi service registration listener for JdbcDao services, so various
 * Derby-specific maintenance jobs can be automatically registered/unregistered
 * with the job scheduler.
 * 
 * <p>
 * The idea of this listener is to automatically schedule jobs to perform
 * maintenance on Derby database tables used by the Solar Node. Over time the
 * tables will grow and maintenance procedures must be called to free up used
 * disk space. This listener assumes any {@link JdbcDao} service will be using
 * Derby, so this bundle should only be started on Solar Nodes actually using
 * Derby (which is the default database implementation).
 * </p>
 * 
 * <p>
 * The {@code maintenanceProperties} Properties can be used to customize each
 * task. The properties are in the form
 * <code>derby.maintenance.<em>schema</em>.<em>table</em>.<em>task</em></code>.
 * The <em>schema</em> and <em>table</em> values will be the database schema and
 * table names returned by {@link JdbcDao#getSchemaName()} and
 * {@link JdbcDao#getTableNames()}. The <em>task</em> value will be one of the
 * following:
 * </p>
 * 
 * <dl>
 * <dt>compress.cron</dt>
 * <dd>The Quartz cron expression to use for scheduling the
 * {@link DerbyCompressTableJob}. If not found, this defaults to
 * {@link #getCompressTableCronExpression()}.</dd>
 * </dl>
 * 
 * <p>
 * The configurable properties of this class are:
 * </p>
 * 
 * <dl class="class-properties">
 * <dt>scheduler</dt>
 * <dd>The Quartz {@link Scheduler} for scheduling and un-scheduling jobs with
 * as {@link TriggerAndJobDetail} services are registered and
 * un-registered.</dd>
 * 
 * <dt>jdbcOperations</dt>
 * <dd>The {@link JdbcOperations} to use.</dd>
 * 
 * <dt>compressTableCronExpression</dt>
 * <dd>The default Quartz cron expression to use for scheduling the
 * {@link DerbyCompressTableJob}. If a matching property is not found in the
 * {@code maintenanceProperties} Properties, this value will be used for the
 * cron expression for that job. Ideally all tables used in the Solar Node
 * system will use different cron expressions because the compress maintenance
 * can take a long time to complete and it is better to schedule different
 * tables at different times. Defaults to
 * {@link #DEFAULT_COMPRESS_TABLE_CRON_EXPRESSION}.</dd>
 * 
 * <dt>maintenanceProperties</dt>
 * <dd>Configuration properties to use when creating the maintenance jobs for
 * each registered table. In general properties will be in the form
 * <code>derby.maintenacne.schema.table.<em>task</em>. See the documentation for
 * each task for more information.</dd>
 * </dl>
 * 
 * @author matt
 * @version 1.2
 */
public class DerbyMaintenanceRegistrationListener
		extends BaseServiceListener<JdbcDao, RegisteredService<JdbcDao>> {

	/** The name of the {@link DerbyCompressTableJob} task. */
	public static final String TASK_COMPRESS = "compress";

	/**
	 * The default value for the {@code compressTableCronExpression} property.
	 */
	public static final String DEFAULT_COMPRESS_TABLE_CRON_EXPRESSION = "0 30 3 ? * WED,SAT";

	private final Logger log = LoggerFactory.getLogger(DerbyMaintenanceRegistrationListener.class);

	private JdbcOperations jdbcOperations = null;
	private Properties maintenanceProperties = null;
	private String compressTableCronExpression = DEFAULT_COMPRESS_TABLE_CRON_EXPRESSION;

	/**
	 * Callback when a JdbcDao has been registered.
	 * 
	 * @param jdbcDao
	 *        the DAO
	 * @param properties
	 *        the service properties
	 */
	public void onBind(JdbcDao jdbcDao, Map<String, ?> properties) {
		if ( log.isDebugEnabled() ) {
			log.debug("Bind called on [" + jdbcDao + "] with props " + properties);
		}
		List<ServiceRegistration<?>> services = new ArrayList<ServiceRegistration<?>>();
		Hashtable<String, Object> serviceProps = new Hashtable<String, Object>();
		serviceProps.put("Bundle-SymbolicName", getBundleContext().getBundle().getSymbolicName());

		for ( String tableName : jdbcDao.getTableNames() ) {
			JobDetail jobDetail = getCompressJobDetail(jdbcDao.getSchemaName(), tableName,
					getJobName(jdbcDao.getSchemaName(), tableName, TASK_COMPRESS));
			CronTrigger trigger = getCronTrigger(
					getTaskPropertyValue(jdbcDao.getSchemaName(), tableName, TASK_COMPRESS + ".cron",
							compressTableCronExpression),
					jobDetail, getTriggerName(jdbcDao.getSchemaName(), tableName, TASK_COMPRESS));
			SimpleTriggerAndJobDetail tjd = new SimpleTriggerAndJobDetail();
			tjd.setJobDetail(jobDetail);
			tjd.setTrigger(trigger);
			tjd.setMessageSource(jdbcDao.getMessageSource());
			ServiceRegistration<TriggerAndJobDetail> reg = getBundleContext()
					.registerService(TriggerAndJobDetail.class, tjd, serviceProps);
			services.add(reg);
		}
		this.addRegisteredService(new RegisteredService<JdbcDao>(jdbcDao, properties), services);
	}

	/**
	 * Callback when a JdbcDao has been un-registered.
	 * 
	 * @param jdbcDao
	 *        the DAO
	 * @param properties
	 *        the service properties
	 */
	public void onUnbind(JdbcDao jdbcDao, Map<String, ?> properties) {
		if ( jdbcDao == null ) {
			// Gemini Blueprint calls this when availability="optional" and no services available
			return;
		}
		if ( log.isDebugEnabled() ) {
			log.debug("Unbind called on [" + jdbcDao + "] with props " + properties);
		}
		removeRegisteredService(jdbcDao, properties);
	}

	private JobDetail getCompressJobDetail(String schema, String table, String name) {
		JobDetailFactoryBean jobDetail = new JobDetailFactoryBean();
		jobDetail.setJobClass(DerbyCompressTableJob.class);
		jobDetail.setName(name);

		Map<String, Object> jobData = new HashMap<String, Object>();
		jobData.put("schema", schema.toUpperCase());
		jobData.put("table", table.toUpperCase());
		jobData.put("jdbcOperations", jdbcOperations);
		jobDetail.setJobDataAsMap(jobData);
		jobDetail.afterPropertiesSet();
		return jobDetail.getObject();
	}

	private CronTrigger getCronTrigger(String cronExpression, JobDetail jobDetail, String name) {
		RandomizedCronTriggerFactoryBean cronTrigger = new RandomizedCronTriggerFactoryBean();
		cronTrigger.setName(name);
		try {
			cronTrigger.setCronExpression(cronExpression);
			cronTrigger.setJobDetail(jobDetail);
			cronTrigger.setMisfireInstruction(CronTrigger.MISFIRE_INSTRUCTION_DO_NOTHING);
			cronTrigger.setRandomSecond(true);
			cronTrigger.afterPropertiesSet();
		} catch ( ParseException e ) {
			throw new RuntimeException(e);
		}
		return cronTrigger.getObject();
	}

	private String getJobName(String schema, String table, String task) {
		return schema + '.' + table + '.' + task;
	}

	private String getTriggerName(String schema, String table, String task) {
		return schema + '.' + table + '.' + task;
	}

	private String getTaskPropertyValue(String schema, String table, String task, String defaultValue) {
		if ( maintenanceProperties == null ) {
			return defaultValue;
		}
		String propKey = "derby.maintenance." + schema + '.' + table + '.' + task;
		return maintenanceProperties.getProperty(propKey, defaultValue);
	}

	public Properties getMaintenanceProperties() {
		return maintenanceProperties;
	}

	public void setMaintenanceProperties(Properties maintenanceProperties) {
		this.maintenanceProperties = maintenanceProperties;
	}

	public String getCompressTableCronExpression() {
		return compressTableCronExpression;
	}

	public void setCompressTableCronExpression(String compressTableCronExpression) {
		this.compressTableCronExpression = compressTableCronExpression;
	}

	public JdbcOperations getJdbcOperations() {
		return jdbcOperations;
	}

	public void setJdbcOperations(JdbcOperations jdbcOperations) {
		this.jdbcOperations = jdbcOperations;
	}

}
