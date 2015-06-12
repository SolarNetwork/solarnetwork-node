/* ==================================================================
 * HeartbeatJob.java - 6/06/2015 5:08:48 pm
 * 
 * Copyright 2007-2015 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.ocpp.impl;

import net.solarnetwork.node.job.AbstractJob;
import net.solarnetwork.node.ocpp.CentralSystemServiceFactory;
import ocpp.v15.cs.CentralSystemService;
import ocpp.v15.cs.HeartbeatRequest;
import ocpp.v15.cs.HeartbeatResponse;
import org.quartz.JobExecutionContext;
import org.quartz.StatefulJob;

/**
 * Job to post the {@link HeartbeatRequest} to let the OCPP system know the node
 * is alive and has network connectivity.
 * 
 * @author matt
 * @version 1.0
 */
public class HeartbeatJob extends AbstractJob implements StatefulJob {

	private CentralSystemServiceFactory service;

	@Override
	protected void executeInternal(JobExecutionContext jobContext) throws Exception {
		if ( service == null ) {
			log.warn("No CentralSystemServiceFactory available, cannot post heartbeat message.");
			return;
		}
		CentralSystemService client = service.service();
		if ( client == null ) {
			log.warn("No CentralSystemService avaialble, cannot post heartbeat message.");
			return;
		}
		HeartbeatRequest req = new HeartbeatRequest();
		HeartbeatResponse res = client.heartbeat(req, service.chargeBoxIdentity());
		log.info("OCPP heartbeat response: {}", res == null ? null : res.getCurrentTime());
	}

	/**
	 * Set the OCPP central service factory to use.
	 * 
	 * @param service
	 *        The service to use.
	 */
	public void setService(CentralSystemServiceFactory service) {
		this.service = service;
	}

}
