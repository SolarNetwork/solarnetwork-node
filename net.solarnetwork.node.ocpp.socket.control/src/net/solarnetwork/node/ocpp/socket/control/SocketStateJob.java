/* ==================================================================
 * ChargeSessionCleanerJob.java - 31/07/2016 7:23:08 AM
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

package net.solarnetwork.node.ocpp.socket.control;

import java.util.ArrayList;
import java.util.List;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.JobExecutionContext;
import org.quartz.PersistJobDataAfterExecution;
import org.springframework.context.MessageSource;
import net.solarnetwork.node.settings.KeyedSettingSpecifier;
import net.solarnetwork.node.settings.SettingSpecifier;
import net.solarnetwork.node.settings.SettingSpecifierProvider;

/**
 * Job to examine charge sessions and make sure their corresponding socket state
 * is valid.
 * 
 * @author matt
 * @version 2.0
 */
@PersistJobDataAfterExecution
@DisallowConcurrentExecution
public class SocketStateJob extends net.solarnetwork.node.job.AbstractJob
		implements SettingSpecifierProvider {

	private SimpleSocketManager socketManager;

	@Override
	protected void executeInternal(JobExecutionContext jobContext) throws Exception {
		socketManager.verifyAllSockets();
	}

	@Override
	public String getSettingUID() {
		return "net.solarnetwork.node.ocpp.socket.control";
	}

	@Override
	public String getDisplayName() {
		return socketManager.getDisplayName();
	}

	@Override
	public MessageSource getMessageSource() {
		return socketManager.getMessageSource();
	}

	@Override
	public List<SettingSpecifier> getSettingSpecifiers() {
		List<SettingSpecifier> result = new ArrayList<SettingSpecifier>();
		for ( SettingSpecifier spec : socketManager.getSettingSpecifiers() ) {
			if ( spec instanceof KeyedSettingSpecifier<?> ) {
				KeyedSettingSpecifier<?> keyedSpec = (KeyedSettingSpecifier<?>) spec;
				result.add(keyedSpec.mappedTo("socketManager."));
			} else {
				result.add(spec);
			}
		}
		return result;
	}

	public void setSocketManager(SimpleSocketManager socketManager) {
		this.socketManager = socketManager;
	}

}
