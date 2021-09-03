/* ==================================================================
 * DatumQueue.java - 21/08/2021 3:18:48 PM
 * 
 * Copyright 2021 SolarNetwork.net Dev Team
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

import java.util.function.Consumer;
import net.solarnetwork.domain.datum.GeneralDatum;
import net.solarnetwork.node.domain.NodeDatum;

/**
 * Unified queue to process datum across all of SolarNode.
 * 
 * @author matt
 * @version 2.0
 * @since 1.89
 */
public interface DatumQueue {

	/**
	 * An {@link org.osgi.service.event.Event} topic for when a
	 * {@link GeneralDatum} has been acquired by a {@link DatumQueue}.
	 * 
	 * This event happens <b>after</b> any possible queue filtering has been
	 * applied, which might filter out some of the datum offered to the queue or
	 * transform their contents. The
	 * {@link net.solarnetwork.node.service.DatumEvents#DATUM_PROPERTY} property
	 * will be set to the datum instance that was acquired. In addition, the
	 * {@link net.solarnetwork.domain.datum.Datum#DATUM_TYPE_PROPERTY} property
	 * shall be populated with the name of the <em>core</em> class name of the
	 * datum type.
	 * 
	 * @since 2.0
	 */
	static final String EVENT_TOPIC_DATUM_ACQUIRED = "net/solarnetwork/node/DatumQueue/DATUM_ACQUIRED";

	/**
	 * Offer a new datum to the queue, with persistence enabled.
	 * 
	 * @param datum
	 *        the datum to offer
	 * @return {@literal true} if the datum was accepted
	 */
	default boolean offer(NodeDatum datum) {
		return offer(datum, true);
	}

	/**
	 * Offer a new datum to the queue, optionally persisting.
	 * 
	 * @param datum
	 *        the datum to offer
	 * @param persist
	 *        {@literal true} to persist, or {@literal false} to only pass to
	 *        consumers
	 * @return {@literal true} if the datum was accepted
	 */
	boolean offer(NodeDatum datum, boolean persist);

	/**
	 * Register a consumer to receive processed datum.
	 * 
	 * @param consumer
	 *        the consumer to register
	 */
	void addConsumer(Consumer<NodeDatum> consumer);

	/**
	 * De-register a previously registered consumer.
	 * 
	 * @param consumer
	 *        the consumer to remove
	 */
	void removeConsumer(Consumer<NodeDatum> consumer);

}
