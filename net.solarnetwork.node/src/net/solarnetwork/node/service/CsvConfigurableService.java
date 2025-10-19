/* ==================================================================
 * CsvConfigurableService.java - 19/10/2025 6:54:19 am
 *
 * Copyright 2025 SolarNetwork.net Dev Team
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

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;

/**
 * API for a service that can import/export CSV configuration.
 *
 * @author matt
 * @version 1.0
 * @since 4.1
 */
public interface CsvConfigurableService {

	/**
	 * Get a simple identifier for this service.
	 *
	 * <p>
	 * This identifier should be simple enough to be used in filenames and help
	 * uniquely identify, like a nickname for the service.
	 * </p>
	 *
	 * @return the identifier, never {@code null}
	 */
	String getCsvConfigurationIdentifier();

	/**
	 * Import configuration from a CSV formatted text stream, optionally
	 * replacing all existing configuration.
	 *
	 * @param in
	 *        the CSV data to import
	 * @param replace
	 *        {@code true} to delete all existing configuration before
	 *        importing; {@code false} to add or update existing configuration
	 * @throws IOException
	 *         if any IO error occurs
	 */
	void importCsvConfiguration(Reader in, boolean replace) throws IOException;

	/**
	 * Export the current configuration as CSV.
	 *
	 * @param out
	 *        the writer to generate the CSV to
	 * @throws IOException
	 *         if any IO error occurs
	 */
	void exportCsvConfiguration(Writer out) throws IOException;

}
