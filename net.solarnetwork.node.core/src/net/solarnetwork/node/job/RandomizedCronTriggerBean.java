/* ==================================================================
 * RandomizedCronTriggerBean.java - Oct 8, 2011 7:36:23 PM
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
 * $Id$
 * ==================================================================
 */

package net.solarnetwork.node.job;

import java.text.ParseException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.quartz.CronTriggerBean;

/**
 * Extension of {@link CronTriggerBean} that can randomize specific fields
 * of the cron expression to distribute a job across several nodes over time.
 * 
 * <p>The cron expression is only randomized if the {@link #setCronExpression(String)}
 * method is used to set the cron expression.</p>
 * 
 * <p>The configurable properties of this class are:</p>
 * 
 * <dl class="class-properties">
 *   <dt>randomSecond</dt>
 *   <dd>If <em>true</em> then set the seconds value of the cron expression will
 *   be set to a random value between 0-59. Will only replace the second value if
 *   a simple second value is specified in the cron expression. For example if the
 *   second value is "0" it will be randomized, while if it is "0/5" it will not.
 *   Defaults to <em>true</em>.</dd>
 * </dl>
 * 
 * @author matt
 * @version $Revision$
 */
public class RandomizedCronTriggerBean extends CronTriggerBean {
	
	private static final long serialVersionUID = 4002734580573984828L;

	private boolean randomSecond = true;
	private String baseCronExpression;

	private Logger log = LoggerFactory.getLogger(getClass());
	
	@Override
	public void setCronExpression(String cronExpression) throws ParseException {
		this.baseCronExpression = cronExpression;
		if ( randomSecond ) {
			int seconds = (int)Math.floor(Math.random() * 60);
			String newExpression = cronExpression.replaceAll("^\\s*\\d+(\\s+)", 
					String.valueOf(seconds) +"$1");
			if ( !newExpression.equals(cronExpression) ) {
				log.debug("Randomized seconds of cron expression set to {}", seconds);
				cronExpression = newExpression;
			}
		}
		super.setCronExpression(cronExpression);
	}

	public boolean isRandomSecond() {
		return randomSecond;
	}

	public void setRandomSecond(boolean randomSecond) {
		this.randomSecond = randomSecond;
	}

	public String getBaseCronExpression() {
		return baseCronExpression;
	}

}
