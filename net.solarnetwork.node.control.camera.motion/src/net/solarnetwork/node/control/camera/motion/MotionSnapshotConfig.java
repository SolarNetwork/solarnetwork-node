/* ==================================================================
 * MotionSnapshotConfig.java - 29/10/2019 12:11:26 pm
 * 
 * Copyright 2019 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.control.camera.motion;

import java.util.ArrayList;
import java.util.List;
import net.solarnetwork.node.settings.SettingSpecifier;
import net.solarnetwork.node.settings.support.BasicCronExpressionSettingSpecifier;
import net.solarnetwork.node.settings.support.BasicTextFieldSettingSpecifier;

/**
 * Configuration for a scheduled snapshot.
 * 
 * @author matt
 * @version 1.0
 * @since 1.1
 */
public class MotionSnapshotConfig {

	private Integer cameraId;
	private String schedule;

	/**
	 * Get a list of setting specifiers suitable for configuring instances of
	 * this class.
	 * 
	 * @param prefix
	 *        a prefix to use for all setting keys
	 * @return the list of settings, never {@literal null}
	 */
	public static List<SettingSpecifier> settings(String prefix) {
		List<SettingSpecifier> results = new ArrayList<SettingSpecifier>();

		results.add(new BasicTextFieldSettingSpecifier(prefix + "cameraId", ""));
		results.add(new BasicCronExpressionSettingSpecifier(prefix + "schedule", ""));

		return results;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("MotionSnapshotConfig{cameraId=");
		builder.append(cameraId);
		builder.append(", schedule=");
		builder.append(schedule);
		builder.append("}");
		return builder.toString();
	}

	/**
	 * Test if this configuration is valid.
	 * 
	 * @return {@literal true} if the configuration appears valid
	 */
	public boolean isValid() {
		return (schedule != null && !schedule.isEmpty() && cameraId != null && cameraId.intValue() > 0);
	}

	/**
	 * Get the auto-snapshot schedule.
	 * 
	 * @return the schedule
	 */
	public String getSchedule() {
		return schedule;
	}

	/**
	 * Set the auto-snapshot schedule.
	 * 
	 * <p>
	 * This schedule defines when to manually request snapshot images from
	 * motion. If just a number, then the frequency in seconds at which to
	 * create snapshots. Otherwise a Quartz-compatible cron expression
	 * representing the schedule at which to create snapshots.
	 * </p>
	 * 
	 * @param schedule
	 */
	public void setSchedule(String schedule) {
		this.schedule = schedule;
	}

	/**
	 * Get the motion camera ID to snapshot.
	 * 
	 * @return the motion camera ID
	 */
	public Integer getCameraId() {
		return cameraId;
	}

	/**
	 * Set the motion camera ID.
	 * 
	 * @param cameraId
	 *        the motion camera ID
	 */
	public void setCameraId(Integer cameraId) {
		this.cameraId = cameraId;
	}

}
