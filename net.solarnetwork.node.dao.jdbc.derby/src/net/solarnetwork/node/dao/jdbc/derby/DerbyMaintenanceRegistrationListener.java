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
 * $Id$
 * ===================================================================
 */

package net.solarnetwork.node.dao.jdbc.derby;

import java.text.ParseException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import net.solarnetwork.node.dao.jdbc.JdbcDao;
import net.solarnetwork.node.job.TriggerAndJobDetail;
import org.quartz.CronTrigger;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.scheduling.quartz.CronTriggerBean;
import org.springframework.scheduling.quartz.JobDetailBean;

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
 * as {@link TriggerAndJobDetail} services are registered and un-registered.</dd>
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
 * <code>derby.maintenacne.schema.table.<em>task</em>. See
 *   the documentation for each task for more information.</dd>
 * </dl>
 * 
 * @author matt
 * @version $Revision$ $Date$
 */
public class DerbyMaintenanceRegistrationListener {

	/** The name of the {@link DerbyCompressTableJob} task. */
	public static final String TASK_COMPRESS = "compress";

	/** The Quartz job group used to schedule all jobs. */
	public static final String JOB_GROUP = "derby.maintenance";

	/**
	 * The default value for the {@code compressTableCronExpression} property.
	 */
	public static final String DEFAULT_COMPRESS_TABLE_CRON_EXPRESSION = "0 30 3 ? * WED,SAT";

	private final Logger log = LoggerFactory.getLogger(DerbyMaintenanceRegistrationListener.class);

	private Scheduler scheduler = null;
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
		for ( String tableName : jdbcDao.getTableNames() ) {
			JobDetailBean jobDetail = getCompressJobDetail(jdbcDao.getSchemaName(), tableName,
					getJobName(jdbcDao.getSchemaName(), tableName, TASK_COMPRESS));
			CronTriggerBean trigger = getCronTrigger(
					getTaskPropertyValue(jdbcDao.getSchemaName(), tableName, TASK_COMPRESS + ".cron",
							compressTableCronExpression), jobDetail,
					getTriggerName(jdbcDao.getSchemaName(), tableName, TASK_COMPRESS));
			try {
				scheduler.scheduleJob(jobDetail, trigger);
			} catch ( SchedulerException e ) {
				log.error("Unable to schedule compress job for " + jdbcDao.getSchemaName() + '.'
						+ tableName);
				throw new RuntimeException(e);
			}
		}
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
		for ( String tableName : jdbcDao.getTableNames() ) {
			String jobName = getJobName(jdbcDao.getSchemaName(), tableName, TASK_COMPRESS);
			try {
				scheduler.deleteJob(jobName, JOB_GROUP);
			} catch ( SchedulerException e ) {
				log.error("Unable to un-schedule compress job " + JOB_GROUP + '.' + jobName);
				throw new RuntimeException(e);
			}
		}
	}

	private JobDetailBean getCompressJobDetail(String schema, String table, String name) {
		JobDetailBean jobDetail = new JobDetailBean();
		jobDetail.setJobClass(DerbyCompressTableJob.class);
		jobDetail.setName(name);
		jobDetail.setGroup(JOB_GROUP);

		Map<String, Object> jobData = new HashMap<String, Object>();
		jobData.put("schema", schema.toUpperCase());
		jobData.put("table", table.toUpperCase());
		jobData.put("jdbcOperations", jdbcOperations);
		jobDetail.setJobDataAsMap(jobData);
		return jobDetail;
	}

	private CronTriggerBean getCronTrigger(String cronExpression, JobDetailBean jobDetail, String name) {
		CronTriggerBean cronTrigger = new CronTriggerBean();
		cronTrigger.setName(name);
		try {
			cronTrigger.setCronExpression(cronExpression);
		} catch ( ParseException e ) {
			throw new RuntimeException(e);
		}
		cronTrigger.setJobDetail(jobDetail);
		cronTrigger.setMisfireInstruction(CronTrigger.MISFIRE_INSTRUCTION_DO_NOTHING);
		return cronTrigger;
	}

	private String getJobName(String schema, String table, String task) {
		return schema + '.' + table + ' ' + task;
	}

	private String getTriggerName(String schema, String table, String task) {
		return schema + '.' + table + ' ' + task;
	}

	private String getTaskPropertyValue(String schema, String table, String task, String defaultValue) {
		if ( maintenanceProperties == null ) {
			return defaultValue;
		}
		String propKey = "derby.maintenance." + schema + '.' + table + '.' + task;
		return maintenanceProperties.getProperty(propKey, defaultValue);
	}

	/**
	 * @return the scheduler
	 */
	public Scheduler getScheduler() {
		return scheduler;
	}

	/**
	 * @param scheduler
	 *        the scheduler to set
	 */
	public void setScheduler(Scheduler scheduler) {
		this.scheduler = scheduler;
	}

	/**
	 * @return the maintenanceProperties
	 */
	public Properties getMaintenanceProperties() {
		return maintenanceProperties;
	}

	/**
	 * @param maintenanceProperties
	 *        the maintenanceProperties to set
	 */
	public void setMaintenanceProperties(Properties maintenanceProperties) {
		this.maintenanceProperties = maintenanceProperties;
	}

	/**
	 * @return the compressTableCronExpression
	 */
	public String getCompressTableCronExpression() {
		return compressTableCronExpression;
	}

	/**
	 * @param compressTableCronExpression
	 *        the compressTableCronExpression to set
	 */
	public void setCompressTableCronExpression(String compressTableCronExpression) {
		this.compressTableCronExpression = compressTableCronExpression;
	}

	/**
	 * @return the jdbcOperations
	 */
	public JdbcOperations getJdbcOperations() {
		return jdbcOperations;
	}

	/**
	 * @param jdbcOperations
	 *        the jdbcOperations to set
	 */
	public void setJdbcOperations(JdbcOperations jdbcOperations) {
		this.jdbcOperations = jdbcOperations;
	}

}
