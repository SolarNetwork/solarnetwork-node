/* ==================================================================
 * BaseControlCenterApplication.java - 7/08/2025 3:45:47 pm
 *
 * Copyright 2025 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.io.dnp3.impl;

import java.time.Clock;
import java.time.InstantSource;
import java.util.Collections;
import com.automatak.dnp3.ClassAssignment;
import com.automatak.dnp3.IINField;
import com.automatak.dnp3.MasterApplication;
import com.automatak.dnp3.TaskId;
import com.automatak.dnp3.TaskInfo;
import com.automatak.dnp3.enums.MasterTaskType;

/**
 * Base implementation of {@link MasterApplication}.
 *
 * @author matt
 * @version 1.0
 */
public class BaseControlCenterApplication extends BaseApplication implements MasterApplication {

	/**
	 * Constructor.
	 */
	public BaseControlCenterApplication() {
		super(Clock.systemUTC());
	}

	/**
	 * Constructor.
	 */
	public BaseControlCenterApplication(InstantSource clock) {
		super(clock);
	}

	@Override
	public long getMillisecondsSinceEpoch() {
		return clock.millis();
	}

	@Override
	public void onReceiveIIN(IINField iin) {
		// no-op
	}

	@Override
	public void onTaskStart(MasterTaskType type, TaskId id) {
		// no-op
	}

	@Override
	public void onTaskComplete(TaskInfo info) {
		// no-op
	}

	@Override
	public void onOpen() {
		// no-op
	}

	@Override
	public void onClose() {
		// no-op
	}

	@Override
	public boolean assignClassDuringStartup() {
		return false;
	}

	@Override
	public Iterable<ClassAssignment> getClassAssignments() {
		return Collections.emptyList();
	}

}
