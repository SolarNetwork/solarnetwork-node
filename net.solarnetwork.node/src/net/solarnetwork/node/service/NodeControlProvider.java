/* ==================================================================
 * NodeControlProvider.java - Sep 28, 2011 6:48:24 PM
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

package net.solarnetwork.node.service;

import java.util.List;
import net.solarnetwork.domain.NodeControlInfo;
import net.solarnetwork.service.Identifiable;

/**
 * API for control providers to implement.
 * 
 * For many control providers that implement a single control, the
 * {@link Identifiable#getUid()} will return the <em>control ID</em> of that
 * control.
 * 
 * @author matt
 * @version 2.0
 */
public interface NodeControlProvider extends Identifiable {

	/**
	 * An {@link org.osgi.service.event.Event} topic for when a
	 * {@link NodeControlInfo} has been read, sampled, or in some way captured
	 * by a {@link NodeControlProvider}. If the {@code NodeControlInfo} also
	 * implements {@link net.solarnetwork.node.domain.datum.NodeDatum} then the
	 * {@link net.solarnetwork.node.service.DatumEvents#DATUM_PROPERTY} event
	 * property will be populated with the instance. Otherwise, the properties
	 * of the event shall be any of the JavaBean properties of the
	 * NodeControlInfo supported by events (i.e. any simple Java property such
	 * as numbers and strings) with enum values provided as strings.
	 * 
	 * @since 1.2
	 */
	String EVENT_TOPIC_CONTROL_INFO_CAPTURED = "net/solarnetwork/node/service/NodeControlProvider/CONTROL_INFO_CAPTURED";

	/**
	 * An {@link org.osgi.service.event.Event} topic for when a
	 * {@link NodeControlInfo} has in some way been changed. If the
	 * {@code NodeControlInfo} also implements
	 * {@link net.solarnetwork.node.domain.datum.NodeDatum} then the
	 * {@link net.solarnetwork.node.service.DatumEvents#DATUM_PROPERTY} event
	 * property will be populated with the instance. Otherwise, the properties
	 * of the event shall be any of the JavaBean properties of the
	 * NodeControlInfo supported by events (i.e. any simple Java property such
	 * as numbers and strings) with enum values provided as strings.
	 * 
	 * @since 1.3
	 */
	String EVENT_TOPIC_CONTROL_INFO_CHANGED = "net/solarnetwork/node/service/NodeControlProvider/CONTROL_INFO_CHANGED";

	/**
	 * Get a list of available controls this provider supports.
	 * 
	 * @return the components
	 */
	List<String> getAvailableControlIds();

	/**
	 * Get the current instantaneous component value for the given component ID.
	 * 
	 * @param controlId
	 *        the ID of the control to get the info for
	 * @return the current value
	 */
	NodeControlInfo getCurrentControlInfo(String controlId);

}
