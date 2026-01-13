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

package net.solarnetwork.node.domain.datum;

import static net.solarnetwork.domain.datum.DatumSamplesType.Instantaneous;
import static net.solarnetwork.domain.datum.DatumSamplesType.Status;
import static net.solarnetwork.util.NumberUtils.parseNumber;
import java.time.Instant;
import net.solarnetwork.domain.NodeControlInfo;
import net.solarnetwork.domain.NodeControlPropertyType;
import net.solarnetwork.domain.datum.DatumId;
import net.solarnetwork.domain.datum.DatumSamples;
import net.solarnetwork.domain.datum.MutableDatumSamplesOperations;
import net.solarnetwork.util.NumberUtils;

/**
 * Extension of {@link SimpleDatum} that wraps a {@link NodeControlInfo}
 * instance.
 *
 * @author matt
 * @version 1.2
 * @since 2.0
 */
public class SimpleNodeControlInfoDatum extends SimpleDatum implements NodeControlInfo {

	private static final long serialVersionUID = -8695996894629256515L;

	/** The default property name used if none provided by a given control. */
	public static final String DEFAULT_PROPERTY_NAME = "val";

	/**
	 * The default property name used for instantaneous values when none
	 * provided by a control.
	 */
	public static final String DEFAULT_INSTANT_PROPERTY_NAME = "v";

	private static final String[] DATUM_TYPES = new String[] { NodeControlInfo.class.getName() };

	/** The node control info. */
	private final NodeControlInfo info;

	/**
	 * Constructor.
	 *
	 * <p>
	 * This constructs a node datum.
	 * </p>
	 *
	 * @param info
	 *        the control info
	 * @param timestamp
	 *        the timestamp
	 */
	public SimpleNodeControlInfoDatum(NodeControlInfo info, Instant timestamp) {
		super(DatumId.nodeId(null, info.getControlId(), timestamp), new DatumSamples());
		this.info = info;
		populateInfo(info);
	}

	/**
	 * Construct from a collection of {@link NodeControlInfo} instances.
	 *
	 * <p>
	 * The {@code info} argument will serve as the primary delegate provide of
	 * the {@link NodeControlInfo} API methods. All available properties from
	 * {@code infos} will be added as datum properties.
	 * </p>
	 *
	 * @param info
	 *        the primary info
	 * @param timestamp
	 *        the timestamp
	 * @param infos
	 *        a collection of info to construct from
	 */
	public SimpleNodeControlInfoDatum(NodeControlInfo info, Instant timestamp,
			Iterable<NodeControlInfo> infos) {
		this(info, timestamp);
		for ( NodeControlInfo n : infos ) {
			populateInfo(n);
		}
	}

	@Override
	public SimpleNodeControlInfoDatum clone() {
		return (SimpleNodeControlInfoDatum) super.clone();
	}

	private void populateInfo(NodeControlInfo controlInfo) {
		String propertyName = DEFAULT_PROPERTY_NAME;
		if ( controlInfo.getPropertyName() != null && controlInfo.getPropertyName().length() > 0 ) {
			propertyName = controlInfo.getPropertyName();
		}
		String value = controlInfo.getValue();
		MutableDatumSamplesOperations ops = asMutableSampleOperations();
		switch (controlInfo.getType()) {
			case Boolean:
				// store boolean flag as a status sample value of 0 or 1
				if ( value.length() > 0 && (value.equals("1") || value.equalsIgnoreCase("yes")
						|| value.equalsIgnoreCase("true")) ) {
					ops.putSampleValue(Status, propertyName, 1);
				} else {
					ops.putSampleValue(Status, propertyName, 0);
				}
				break;

			case Integer: {
				// store as both instant and status
				Number val = NumberUtils.narrow(NumberUtils.bigIntegerForNumber(parseNumber(value)), 2);
				ops.putSampleValue(Status, propertyName, val);
				if ( propertyName.equals(DEFAULT_PROPERTY_NAME) ) {
					ops.putSampleValue(Instantaneous, DEFAULT_INSTANT_PROPERTY_NAME, val);
				}
			}
				break;

			case Float:
			case Percent: {
				Number val = parseNumber(value);
				ops.putSampleValue(Status, propertyName, val);
				if ( propertyName.equals(DEFAULT_PROPERTY_NAME) ) {
					ops.putSampleValue(Instantaneous, DEFAULT_INSTANT_PROPERTY_NAME, val);
				}
			}
				break;

			default:
				// for rest, put as string directly
				ops.putSampleValue(Status, propertyName, value);
				break;

		}
	}

	@Override
	protected String[] datumTypes() {
		return DATUM_TYPES;
	}

	@Override
	public String getControlId() {
		return info.getControlId();
	}

	@Override
	public String getPropertyName() {
		return info.getPropertyName();
	}

	@Override
	public NodeControlPropertyType getType() {
		return info.getType();
	}

	@Override
	public String getValue() {
		return info.getValue();
	}

	@Override
	public Boolean getReadonly() {
		return info.getReadonly();
	}

	@Override
	public String getUnit() {
		return info.getUnit();
	}

}
