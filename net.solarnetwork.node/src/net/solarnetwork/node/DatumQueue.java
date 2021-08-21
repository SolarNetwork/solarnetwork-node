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

package net.solarnetwork.node;

import java.util.function.Consumer;
import net.solarnetwork.node.domain.Datum;

/**
 * Unified queue to process datum across all of SolarNode.
 * 
 * @param <T>
 *        the datum type
 * @author matt
 * @version 1.0
 * @since 1.89
 */
public interface DatumQueue<T extends Datum> {

	/**
	 * Offer a new datum to the queue.
	 * 
	 * @param datum
	 *        the datum to offer
	 * @return
	 */
	boolean offer(T datum);

	/**
	 * Register a consumer to receive processed datum.
	 * 
	 * @param consumer
	 *        the consumer to register
	 */
	void addConsumer(Consumer<T> consumer);

	/**
	 * De-register a previously registered consumer.
	 * 
	 * @param consumer
	 *        the consumer to remove
	 */
	void removeConsumer(Consumer<T> consumer);

}
