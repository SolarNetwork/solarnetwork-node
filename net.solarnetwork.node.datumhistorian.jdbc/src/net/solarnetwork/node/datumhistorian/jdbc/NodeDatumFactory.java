/* ==================================================================
 * NodeDatumFactory.java - 11 Nov 2026 6:46:47 am
 * 
 * Copyright 2026 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.datumhistorian.jdbc;

import java.util.Set;
import org.jspecify.annotations.Nullable;
import net.solarnetwork.node.domain.datum.NodeDatum;

/**
 * API for creating {@link NodeDatum} instances from {@link StoredDatum}.
 * 
 * @author matt
 * @version 1.0
 */
public interface NodeDatumFactory {

	/**
	 * Get the names of all classes and interfaces a class can be assigned to.
	 *
	 * <p>
	 * The first name is the class itself, followed by its superclasses (not
	 * including {@link Object}) and then all the interfaces they implement.
	 * </p>
	 *
	 * @param type
	 *        the class to get the type names for
	 * @return the type names, never empty
	 */
	Set<String> typeNames(Class<?> type);

	/**
	 * Create a datum.
	 *
	 * @param <T>
	 *        the type of datum to create
	 * @param datum
	 *        the stored datum
	 * @param type
	 *        the type of datum to create, or {@code null} for any type
	 * @return the datum, or {@code null} if no datum of the given type could be
	 *         created
	 */
	<T extends NodeDatum> @Nullable T createDatum(StoredDatum datum, @Nullable Class<T> type);

}
