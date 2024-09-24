/* ==================================================================
 * DatumQueueProcessHook.java - 24/09/2024 6:31:59 am
 *
 * Copyright 2024 SolarNetwork.net Dev Team
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

import net.solarnetwork.node.domain.datum.NodeDatum;

/**
 * API for a datum queue process observer.
 *
 * @author matt
 * @version 1.0
 * @since 3.19
 */
@FunctionalInterface
public interface DatumQueueProcessObserver {

	/**
	 * A queue process stage.
	 */
	enum Stage {

		/** Unfiltered datum, before any datum filters have been applied. */
		PreFilter,

		/** Datum after filtering has been applied. */
		PostFilter,

		;
	}

	/**
	 * Callback during a queue processing stage.
	 *
	 * @param queue
	 *        the queue
	 * @param datum
	 *        the datum being processed
	 * @param stage
	 *        the processing stage
	 * @param persist
	 *        {@literal true} if the datum will be persisted
	 */
	void datumQueueWillProcess(DatumQueue queue, NodeDatum datum, Stage stage, boolean persist);

}
