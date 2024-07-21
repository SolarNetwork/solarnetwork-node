/* ==================================================================
 * CsvExportBatchCallback.java - 21/07/2024 7:26:08 am
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

package net.solarnetwork.node.metrics.service;

import java.io.Closeable;
import java.io.IOException;
import java.io.Writer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.supercsv.io.CsvListWriter;
import org.supercsv.io.ICsvListWriter;
import org.supercsv.prefs.CsvPreference;
import net.solarnetwork.dao.BatchableDao.BatchCallback;
import net.solarnetwork.dao.BatchableDao.BatchCallbackResult;
import net.solarnetwork.node.metrics.domain.Metric;
import net.solarnetwork.util.ObjectUtils;

/**
 * {@link BatchCallback} for exporting metrics as a CSV document.
 *
 * @author matt
 * @version 1.0
 */
public class CsvExportBatchCallback implements BatchCallback<Metric>, Closeable {

	private static final String[] CSV_HEADERS = new String[] { "Date", "Type", "Name", "Value" };

	private static final Logger log = LoggerFactory.getLogger(CsvExportBatchCallback.class);

	private final ICsvListWriter writer;

	/**
	 * Constructor.
	 *
	 * @param out
	 *        the output writer
	 * @throws IllegalArgumentException
	 *         if any argument is {@literal null}
	 */
	public CsvExportBatchCallback(Writer out) {
		super();
		writer = new CsvListWriter(ObjectUtils.requireNonNullArgument(out, "out"),
				CsvPreference.STANDARD_PREFERENCE);

	}

	@Override
	public void close() throws IOException {
		writer.close();
	}

	@Override
	public BatchCallbackResult handle(Metric metric) {
		try {
			if ( writer.getLineNumber() == 0 ) {
				writer.writeHeader(CSV_HEADERS);
			}
			writer.write(metric.getTimestamp().toString(), metric.getType(), metric.getName(),
					String.valueOf(metric.getValue()));
			return BatchCallbackResult.CONTINUE;
		} catch ( IOException e ) {
			log.warn("IO error generating metric CSV output: {}", e.getMessage(), e);
			return BatchCallbackResult.STOP;
		}
	}

}
