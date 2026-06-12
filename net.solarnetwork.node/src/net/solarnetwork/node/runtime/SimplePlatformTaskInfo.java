/* ==================================================================
 * SimplePlatformTaskInfo.java - 21/11/2017 1:47:44 PM
 *
 * Copyright 2017 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.runtime;

import java.util.Locale;
import net.solarnetwork.node.service.PlatformService;
import net.solarnetwork.node.service.PlatformService.PlatformTaskInfo;

/**
 * Simple implementation of {@link PlatformTaskInfo}.
 *
 * @author matt
 * @version 1.0
 */
public class SimplePlatformTaskInfo implements PlatformTaskInfo {

	private final String taskId;
	private final String title;
	private final String message;
	private final double percentComplete;
	private final boolean complete;
	private final boolean restartRequired;

	/**
	 * Construct from values.
	 *
	 * @param taskId
	 *        the task ID
	 * @param title
	 *        the title
	 * @param message
	 *        the message
	 * @param percentComplete
	 *        the percent complete
	 * @param complete
	 *        {@literal true} if the task is complete
	 * @param restartRequired
	 *        the restart required flag
	 */
	public SimplePlatformTaskInfo(String taskId, String title, String message, double percentComplete,
			boolean complete, boolean restartRequired) {
		super();
		this.taskId = taskId;
		this.title = title;
		this.message = message;
		this.percentComplete = percentComplete;
		this.complete = complete;
		this.restartRequired = restartRequired;
	}

	/**
	 * Construct from a status and locale.
	 *
	 * @param status
	 *        the status
	 * @param locale
	 *        the desired locale
	 */
	public SimplePlatformTaskInfo(PlatformService.PlatformTaskStatus status, Locale locale) {
		this(status.getTaskId(), status.getTitle(locale), status.getMessage(locale),
				status.getPercentComplete(), status.isComplete(), status.isRestartRequired());
	}

	@Override
	public final String getTaskId() {
		return taskId;
	}

	@Override
	public final String getTitle() {
		return title;
	}

	@Override
	public final String getMessage() {
		return message;
	}

	@Override
	public final double getPercentComplete() {
		return percentComplete;
	}

	@Override
	public final boolean isComplete() {
		return complete;
	}

	@Override
	public final boolean isRestartRequired() {
		return restartRequired;
	}

}
