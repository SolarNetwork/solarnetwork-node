/* ===================================================================
 * JobAndTrigger.java
 * 
 * Created Dec 2, 2009 10:31:01 AM
 * 
 * Copyright 2007-2009 SolarNetwork.net Dev Team
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
 * ===================================================================
 * $Id$
 * ===================================================================
 */

package net.solarnetwork.node.job;

import org.quartz.JobDetail;
import org.quartz.Trigger;
import org.springframework.context.MessageSource;

/**
 * A bean that combines a trigger and a job.
 * 
 * <p>The primary motivation for this class is to support the
 * {@link net.solarnetwork.node.runtime.JobServiceRegistrationListener}
 * for registering/un-registering jobs published as services in
 * bundles within a single "core" {@code Scheduler}.</p>
 *
 * @author matt
 * @version $Revision$ $Date$
 */
public interface TriggerAndJobDetail {

	/**
	 * Get the Trigger.
	 * 
	 * @return the trigger
	 */
	Trigger getTrigger();
	
	/**
	 * Get the JobDetail.
	 * 
	 * @return the jobDetail
	 */
	JobDetail getJobDetail();
	
	/**
	 * Get a MessageSource to localize the setting text.
	 * 
	 * <p>
	 * This method can return {@literal null} if the provider does not have any
	 * localized resources.
	 * </p>
	 * 
	 * @return the MessageSource, or {@literal null}
	 */
	MessageSource getMessageSource();
}
