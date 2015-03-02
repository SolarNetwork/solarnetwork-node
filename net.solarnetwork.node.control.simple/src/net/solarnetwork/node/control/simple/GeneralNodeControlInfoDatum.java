/* ==================================================================
 * GeneralNodeControlInfoDatum.java - Dec 18, 2014 7:05:45 AM
 * 
 * Copyright 2007-2014 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.control.simple;

import java.math.BigDecimal;
import java.util.Date;
import net.solarnetwork.domain.NodeControlInfo;
import net.solarnetwork.node.domain.GeneralNodeDatum;

/**
 * Extension of {@link GeneralNodeDatum} that wraps a {@link NodeControlInfo}
 * instance.
 * 
 * @author matt
 * @version 1.0
 */
public class GeneralNodeControlInfoDatum extends GeneralNodeDatum {

	/** The default property name used if none provided by a given control. */
	public static final String DEFAULT_PROPERTY_NAME = "val";

	/**
	 * The default property name used for instantaneous values when none
	 * provided by a control.
	 */
	public static final String DEFAULT_INSTANT_PROPERTY_NAME = "v";

	/**
	 * Construct from a {@link NodeControlInfo} instance.
	 * 
	 * @param info
	 *        the info to construct from
	 */
	public GeneralNodeControlInfoDatum(NodeControlInfo info) {
		super();
		populateInfo(info);
	}

	/**
	 * Construct from a collection of {@link NodeControlInfo} instances. The
	 * {@code controlId} of only the first value will be used to populate the
	 * {@code sourceId}.
	 * 
	 * @param infos
	 *        the collection of info to construct from
	 */
	public GeneralNodeControlInfoDatum(Iterable<NodeControlInfo> infos) {
		super();
		setCreated(new Date());
		for ( NodeControlInfo info : infos ) {
			populateInfo(info);
		}
	}

	private void populateInfo(NodeControlInfo controlInfo) {
		if ( getSourceId() == null || getSourceId().length() == 0 ) {
			setSourceId(controlInfo.getControlId());
		}
		String propertyName = DEFAULT_PROPERTY_NAME;
		if ( controlInfo.getPropertyName() != null && controlInfo.getPropertyName().length() > 0 ) {
			propertyName = controlInfo.getPropertyName();
		}
		String value = controlInfo.getValue();
		switch (controlInfo.getType()) {
			case Boolean:
				// store boolean flag as a status sample value of 0 or 1
				if ( value.length() > 0
						&& (value.equals("1") || value.equalsIgnoreCase("yes") || value
								.equalsIgnoreCase("true")) ) {
					putStatusSampleValue(propertyName, 1);
				} else {
					putStatusSampleValue(propertyName, 0);
				}
				break;

			case Integer:
				// store as both instant and status
				Integer val = Integer.valueOf(value);
				putStatusSampleValue(propertyName, val);
				if ( propertyName.equals(DEFAULT_PROPERTY_NAME) ) {
					putInstantaneousSampleValue(DEFAULT_INSTANT_PROPERTY_NAME, val);
				}
				break;

			default:
				// store others as floating point instants and status
				BigDecimal decimalValue = new BigDecimal(value);
				putStatusSampleValue(propertyName, decimalValue);
				if ( propertyName.equals(DEFAULT_PROPERTY_NAME) ) {
					putInstantaneousSampleValue(DEFAULT_INSTANT_PROPERTY_NAME, decimalValue);
				}
				break;

		}
	}
}
