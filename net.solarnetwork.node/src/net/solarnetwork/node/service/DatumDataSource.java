/* ===================================================================
 * DatumDataSource.java
 *
 * Created Nov 30, 2009 4:52:53 PM
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
 */

package net.solarnetwork.node.service;

import java.util.Collection;
import java.util.Collections;
import net.solarnetwork.node.domain.datum.NodeDatum;
import net.solarnetwork.service.Identifiable;

/**
 * API for collecting {@link NodeDatum} objects from some device.
 *
 * @author matt
 * @version 2.1
 */
public interface DatumDataSource extends Identifiable, DeviceInfoProvider, DatumSourceIdProvider {

	/**
	 * An {@link org.osgi.service.event.Event} topic for when a
	 * {@link NodeDatum} has been read, sampled, or in some way captured by a
	 * {@link DatumDataSource}. The
	 * {@link net.solarnetwork.node.service.DatumEvents#DATUM_PROPERTY} property
	 * will be set to the datum instance that was captured. In addition, the
	 * {@link NodeDatum#DATUM_TYPE_PROPERTY} property shall be populated with
	 * the name of the <em>core</em> class name of the datum type.
	 *
	 * @since 1.2
	 */
	public static final String EVENT_TOPIC_DATUM_CAPTURED = "net/solarnetwork/node/service/DatumDataSource/DATUM_CAPTURED";

	/**
	 * Get the class supported by this DataSource.
	 *
	 * @return class
	 */
	Class<? extends NodeDatum> getDatumType();

	/**
	 * Read the current value from the data source, returning as an unpersisted
	 * {@link NodeDatum} object.
	 *
	 * @return Datum
	 */
	NodeDatum readCurrentDatum();

	/**
	 * Get the collection of source IDs produced by this datum data source.
	 *
	 * <p>
	 * This implementation returns an empty list.
	 * </p>
	 *
	 * @return the collection of published source IDs, never {@literal null}
	 * @since 2.1
	 */
	@Override
	default Collection<String> publishedSourceIds() {
		return Collections.emptyList();
	}

}
