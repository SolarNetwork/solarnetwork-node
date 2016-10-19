/* ==================================================================
 * DerbyCompressTablesJob.java - 30/09/2016 7:25:29 AM
 * 
 * Copyright 2007-2016 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.dao.jdbc.derby;

import org.quartz.DisallowConcurrentExecution;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.PersistJobDataAfterExecution;
import net.solarnetwork.node.job.AbstractJob;

/**
 * Job to compress Derby tables using the
 * {@code SYSCS_UTIL.SYSCS_INPLACE_COMPRESS_TABLE} procedure.
 * 
 * @author matt
 * @version 1.0
 */
@PersistJobDataAfterExecution
@DisallowConcurrentExecution
public class DerbyCompressTablesJob extends AbstractJob {

	private DerbyCompressTablesService compressService;

	public static final String JOB_KEY_LAST_TABLE_KEY = "CompressTablesLastKey";

	@Override
	protected void executeInternal(JobExecutionContext jobContext) throws Exception {
		if ( compressService == null ) {
			return;
		}
		JobDataMap jd = jobContext.getJobDetail().getJobDataMap();
		String startAfterKey = (String) jd.get(JOB_KEY_LAST_TABLE_KEY);
		startAfterKey = compressService.processTables(startAfterKey);
		if ( startAfterKey == null ) {
			jd.remove(JOB_KEY_LAST_TABLE_KEY);
		} else {
			jd.put(JOB_KEY_LAST_TABLE_KEY, startAfterKey);
		}
	}

	/**
	 * Set the service to use.
	 * 
	 * @param compressService
	 *        The compress service.
	 */
	public void setCompressService(DerbyCompressTablesService compressService) {
		this.compressService = compressService;
	}

}
