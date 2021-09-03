/* ===================================================================
 * DatumDaoCleanerJob.java
 * 
 * Created Dec 4, 2009 1:50:50 PM
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

package net.solarnetwork.node.job;

import org.quartz.DisallowConcurrentExecution;
import org.quartz.JobExecutionContext;
import org.quartz.PersistJobDataAfterExecution;
import net.solarnetwork.node.dao.DatumDao;

/**
 * Job to delete locally persisted datum that have been uploaded already and are
 * safe to remove.
 * 
 * <p>
 * This job will call {@link DatumDao#deleteUploadedDataOlderThan(int)} and emit
 * a log line if this returns a positive value.
 * </p>
 * 
 * @author matt
 * @version 2.0
 */
@PersistJobDataAfterExecution
@DisallowConcurrentExecution
public class DatumDaoCleanerJob extends AbstractJob {

	/** The default value for the {@code hours} property. */
	public static final int DEFAULT_HOURS = 4;

	private int hours = DEFAULT_HOURS;
	private DatumDao datumDao = null;

	@Override
	protected void executeInternal(JobExecutionContext jobContext) throws Exception {
		if ( log.isDebugEnabled() ) {
			log.debug("Deleting datum data older than [{}] hours", hours);
		}
		int result = datumDao.deleteUploadedDataOlderThan(hours);
		if ( log.isInfoEnabled() && result > 0 ) {
			log.info("Deleted {} datum older than {} hours", hours);
		}
	}

	/**
	 * Get he minimum age of data that has been uploaded to delete.
	 * 
	 * @return the hours; defaults to {@link #DEFAULT_HOURS}
	 */
	public int getHours() {
		return hours;
	}

	/**
	 * Set he minimum age of data that has been uploaded to delete.
	 * 
	 * @param hours
	 *        the hours to set
	 */
	public void setHours(int hours) {
		this.hours = hours;
	}

	/**
	 * Get the datum DAO.
	 * 
	 * @return the datumDao
	 */
	public DatumDao getDatumDao() {
		return datumDao;
	}

	/**
	 * Set the datum DAO.
	 * 
	 * @param datumDao
	 *        the datumDao to set
	 */
	public void setDatumDao(DatumDao datumDao) {
		this.datumDao = datumDao;
	}

}
