/* ==================================================================
 * OutstationService.java - 21/02/2019 11:05:32 am
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

package net.solarnetwork.node.io.dnp3;

import com.automatak.dnp3.ApplicationIIN;
import com.automatak.dnp3.Outstation;
import com.automatak.dnp3.OutstationApplication;
import com.automatak.dnp3.enums.AssignClassType;
import com.automatak.dnp3.enums.LinkStatus;
import com.automatak.dnp3.enums.PointClass;
import net.solarnetwork.util.OptionalService;

/**
 * A DNP3 "outstation" server service that publishes SolarNode datum/control
 * events to DNP3.
 * 
 * @author matt
 * @version 1.0
 */
public class OutstationService implements OutstationApplication {

	private final OptionalService<ChannelService> dnp3Channel;

	private Outstation outstation;

	/**
	 * Constructor.
	 * 
	 * @param dnp3Channel
	 *        the channel to use
	 */
	public OutstationService(OptionalService<ChannelService> dnp3Channel) {
		super();
		this.dnp3Channel = dnp3Channel;
	}

	/**
	 * Call to initialize after configuring class properties.
	 */
	public synchronized void startup() {
		// TODO
	}

	/**
	 * Shutdown this service when no longer needed.
	 */
	public synchronized void shutdown() {
		if ( outstation != null ) {
			outstation.shutdown();
			this.outstation = null;
		}
	}

	/*
	 * =========================================================================
	 * OutstationApplication implementation
	 * =========================================================================
	 */

	@Override
	public void onStateChange(LinkStatus value) {
		// TODO Auto-generated method stub
	}

	@Override
	public void onKeepAliveInitiated() {
		// TODO Auto-generated method stub
	}

	@Override
	public void onKeepAliveFailure() {
		// TODO Auto-generated method stub
	}

	@Override
	public void onKeepAliveSuccess() {
		// TODO Auto-generated method stub
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
	public void recordClassAssignment(AssignClassType type, PointClass clazz, int start, int stop) {
		// not supported
	}

	@Override
	public ApplicationIIN getApplicationIIN() {
		return ApplicationIIN.none();
	}

}
