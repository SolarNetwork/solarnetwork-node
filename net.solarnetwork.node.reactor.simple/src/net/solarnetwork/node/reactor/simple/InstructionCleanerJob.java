/* ==================================================================
 * InstructionCleanerJob.java - Oct 3, 2011 4:21:34 PM
 * 
 * Copyright 2007-2011 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.reactor.simple;

import org.quartz.DisallowConcurrentExecution;
import org.quartz.JobExecutionContext;
import org.quartz.PersistJobDataAfterExecution;
import net.solarnetwork.node.job.AbstractJob;
import net.solarnetwork.node.reactor.InstructionDao;

/**
 * Job to clean out old instructions so they don't build up.
 * 
 * @author matt
 * @version 2.0
 */
@PersistJobDataAfterExecution
@DisallowConcurrentExecution
public class InstructionCleanerJob extends AbstractJob {

	/** The default value for the {@code hours} property. */
	public static final int DEFAULT_HOURS = 72;

	private int hours = DEFAULT_HOURS;
	private InstructionDao instructionDao = null;

	@Override
	protected void executeInternal(JobExecutionContext jobContext) throws Exception {
		int deleted = instructionDao.deleteHandledInstructionsOlderThan(hours);
		if ( deleted > 0 ) {
			log.info("Deleted {} handled instructions older than {} hours", deleted, hours);
		}
	}

	public int getHours() {
		return hours;
	}

	public void setHours(int hours) {
		this.hours = hours;
	}

	public InstructionDao getInstructionDao() {
		return instructionDao;
	}

	public void setInstructionDao(InstructionDao instructionDao) {
		this.instructionDao = instructionDao;
	}

}
