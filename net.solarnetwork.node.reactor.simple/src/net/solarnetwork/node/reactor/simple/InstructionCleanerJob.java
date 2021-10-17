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

import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.util.Collections;
import java.util.List;
import net.solarnetwork.node.job.JobService;
import net.solarnetwork.node.reactor.InstructionDao;
import net.solarnetwork.node.service.support.BaseIdentifiable;
import net.solarnetwork.settings.SettingSpecifier;

/**
 * Job to clean out old instructions so they don't build up.
 * 
 * @author matt
 * @version 2.0
 */
public class InstructionCleanerJob extends BaseIdentifiable implements JobService {

	/** The default value for the {@code hours} property. */
	public static final int DEFAULT_HOURS = 72;

	private final InstructionDao instructionDao;
	private int hours = DEFAULT_HOURS;

	public InstructionCleanerJob(InstructionDao instructionDao) {
		super();
		this.instructionDao = requireNonNullArgument(instructionDao, "instructionDao");
	}

	@Override
	public String getSettingUid() {
		return "net.solarnetwork.node.reactor.simple";
	}

	@Override
	public List<SettingSpecifier> getSettingSpecifiers() {
		return Collections.emptyList();
	}

	@Override
	public void executeJobService() throws Exception {
		int deleted = instructionDao.deleteHandledInstructionsOlderThan(hours);
		if ( deleted > 0 ) {
			log.info("Deleted {} handled instructions older than {} hours", deleted, hours);
		}
	}

	/**
	 * Get the minimum age of handled instructions to delete, in hours.
	 * 
	 * @return the hours
	 */
	public int getHours() {
		return hours;
	}

	/**
	 * Set the minimum age of handled instructions to delete, in hours.
	 * 
	 * @param hours
	 *        the hours
	 */
	public void setHours(int hours) {
		this.hours = hours;
	}

}
