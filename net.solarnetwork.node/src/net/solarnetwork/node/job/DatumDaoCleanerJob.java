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

import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.util.Collections;
import java.util.List;
import net.solarnetwork.node.dao.DatumDao;
import net.solarnetwork.node.service.support.BaseIdentifiable;
import net.solarnetwork.settings.SettingSpecifier;

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
public class DatumDaoCleanerJob extends BaseIdentifiable implements JobService {

	/** The default value for the {@code hours} property. */
	public static final int DEFAULT_HOURS = 4;

	private final DatumDao datumDao;
	private int hours = DEFAULT_HOURS;

	/**
	 * Constructor.
	 * 
	 * @param datumDao
	 *        the datum DAO to use
	 * @throws IllegalArgumentException
	 *         if any argument is {@literal null}
	 */
	public DatumDaoCleanerJob(DatumDao datumDao) {
		super();
		this.datumDao = requireNonNullArgument(datumDao, "datumDao");
	}

	@Override
	public String getSettingUid() {
		String uid = getUid();
		return (uid != null && !uid.isEmpty() ? uid : "net.solarnetwork.node.job.DatumDaoCleanerJob");
	}

	@Override
	public List<SettingSpecifier> getSettingSpecifiers() {
		return Collections.emptyList();
	}

	@Override
	public void executeJobService() throws Exception {
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

}
