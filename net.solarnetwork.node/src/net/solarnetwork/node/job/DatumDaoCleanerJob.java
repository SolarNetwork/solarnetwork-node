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

import net.solarnetwork.node.Datum;
import net.solarnetwork.node.UploadService;
import net.solarnetwork.node.dao.DatumDao;

import org.quartz.JobExecutionContext;
import org.quartz.StatefulJob;

/**
 * Job to query a {@link DatumDao} for data to upload via an
 * {@link UploadService}.
 * 
 * <p>This job will call {@link DatumDao#deleteUploadedDataOlderThan(int)} and
 * emit a log line if this returns a positive value.</p>
 * 
 * <p>The configurable properties of this class are:</p>
 * 
 * <dl class="class-properties">
 *   <dt>datumDao</dt>
 *   <dd>The {@link DatumDao} to use to query for {@link Datum} to upload.</dd>
 *   
 *   <dt>hours</dt>
 *   <dd>The minimum age of data that has been uploaded to delete. Defaults to
 *   {@link #DEFAULT_HOURS}</dd>
 * </dl>
 *
 * @param <T> the Datum type for this job
 * @author matt
 * @version $Revision$ $Date$
 */
public class DatumDaoCleanerJob<T extends Datum> extends AbstractJob
implements StatefulJob {

	/** The default value for the {@code hours} property. */
	public static final int DEFAULT_HOURS = 72;
	
	private int hours = DEFAULT_HOURS;
	private DatumDao<T> datumDao = null;
	
	@Override
	protected void executeInternal(JobExecutionContext jobContext)
			throws Exception {
		if ( log.isDebugEnabled() ) {
			log.debug("Deleting [" +datumDao.getDatumType().getSimpleName() 
					+"] data older than [" +hours 
					+"] hours");
		}
		int result = datumDao.deleteUploadedDataOlderThan(hours);
		if ( log.isInfoEnabled() && result > 0 ) {
			log.info("Deleted " +result +" [" 
					+datumDao.getDatumType().getSimpleName() 
					+"] entities older than " +hours +" hours");
		}
	}

	/**
	 * @return the hours
	 */
	public int getHours() {
		return hours;
	}

	/**
	 * @param hours the hours to set
	 */
	public void setHours(int hours) {
		this.hours = hours;
	}

	/**
	 * @return the datumDao
	 */
	public DatumDao<T> getDatumDao() {
		return datumDao;
	}

	/**
	 * @param datumDao the datumDao to set
	 */
	public void setDatumDao(DatumDao<T> datumDao) {
		this.datumDao = datumDao;
	}

}
