/* ==================================================================
 * BaseOutstationApplication.java - 22/02/2019 10:02:06 am
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

package net.solarnetwork.node.io.dnp3.impl;

import com.automatak.dnp3.ApplicationIIN;
import com.automatak.dnp3.OutstationApplication;
import com.automatak.dnp3.enums.AssignClassType;
import com.automatak.dnp3.enums.LinkStatus;
import com.automatak.dnp3.enums.PointClass;
import com.automatak.dnp3.enums.RestartMode;

/**
 * Base implementation of {@link OutstationApplication}.
 *
 * @author matt
 * @version 1.1
 */
public class BaseOutstationApplication implements OutstationApplication {

	private LinkStatus linkStatus = LinkStatus.UNRESET;

	/**
	 * Constructor.
	 */
	public BaseOutstationApplication() {
		super();
	}

	@Override
	public void onStateChange(LinkStatus value) {
		linkStatus = value;
	}

	/**
	 * Get the current link status.
	 *
	 * @return the status
	 */
	protected LinkStatus getLinkStatus() {
		return linkStatus;
	}

	@Override
	public void onKeepAliveInitiated() {
		// nothing
	}

	@Override
	public void onKeepAliveFailure() {
		// nothing
	}

	@Override
	public void onKeepAliveSuccess() {
		// nothing
	}

	@Override
	public boolean supportsWriteAbsoluteTime() {
		return false;
	}

	@Override
	public boolean writeAbsoluteTime(long msSinceEpoch) {
		return false;
	}

	@Override
	public boolean supportsAssignClass() {
		return false;
	}

	@Override
	public RestartMode coldRestartSupport() {
		return RestartMode.UNSUPPORTED;
	}

	@Override
	public RestartMode warmRestartSupport() {
		return RestartMode.UNSUPPORTED;
	}

	@Override
	public int coldRestart() {
		return 0xFFFF;
	}

	@Override
	public int warmRestart() {
		return 0xFFFF;
	}

	@Override
	public void recordClassAssignment(AssignClassType type, PointClass clazz, int start, int stop) {
		// not supported
	}

	@Override
	public ApplicationIIN getApplicationIIN() {
		return ApplicationIIN.none();
	}

}
