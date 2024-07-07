/* ==================================================================
 * LoggingCanbusFrameListener.java - 8/05/2022 4:48:09 pm
 *
 * Copyright 2022 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.io.canbus.support;

import static java.nio.file.StandardOpenOption.APPEND;
import static java.nio.file.StandardOpenOption.CREATE;
import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.io.BufferedWriter;
import java.io.Closeable;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.zip.GZIPOutputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.solarnetwork.node.io.canbus.CanbusFrame;
import net.solarnetwork.node.io.canbus.CanbusFrameListener;
import net.solarnetwork.node.io.canbus.util.CanbusUtils;
import net.solarnetwork.util.ByteUtils;

/**
 * Listener that logs messages to a file.
 *
 * @author matt
 * @version 1.0
 * @since 2.1
 */
public class LoggingCanbusFrameListener implements CanbusFrameListener, Closeable {

	private static final Logger log = LoggerFactory.getLogger(LoggingCanbusFrameListener.class);

	private final String busName;
	private final Path logFile;
	private final boolean inlineDate;
	private final boolean gzip;
	private final PrintWriter out;
	private boolean closed;
	private boolean autoFlush;

	/**
	 * Constructor.
	 *
	 * @param busName
	 *        a busName to use for debug log messages
	 * @param logFile
	 *        the log file to write to
	 * @param includeDate
	 *        {@literal true} to include a time stamp as the first field in
	 *        every message line
	 * @param autoFlush
	 *        {@literal true} to flush the output stream after every message
	 * @param gzip
	 *        {@literal true} to compress the output with gzip
	 * @throws IOException
	 *         if the logging can not be set up successfully
	 */
	public LoggingCanbusFrameListener(String busName, Path logFile, boolean includeDate,
			boolean autoFlush, boolean gzip) throws IOException {
		super();
		this.busName = requireNonNullArgument(busName, "busName");
		this.logFile = requireNonNullArgument(logFile, "logFile");
		this.inlineDate = includeDate;
		this.gzip = gzip;
		this.out = createOutputStream();
		this.closed = false;
	}

	private PrintWriter createOutputStream() throws IOException {
		if ( !Files.exists(logFile.getParent()) ) {
			try {
				Files.createDirectories(logFile.getParent());
			} catch ( Exception e ) {
				log.warn("Error creating debug log directory {}: {}", logFile.getParent(), e.toString());
			}
		}
		PrintWriter out;
		if ( gzip ) {
			out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(
					new GZIPOutputStream(Files.newOutputStream(logFile, CREATE, APPEND), 4096),
					ByteUtils.UTF8)));
		} else {
			out = new PrintWriter(Files.newBufferedWriter(logFile, ByteUtils.UTF8, CREATE, APPEND));
		}
		log.info("Capturing CAN frames for {} (gzip = {}) to {}", busName, gzip,
				logFile.toAbsolutePath());
		return out;
	}

	@Override
	public synchronized void close() throws IOException {
		if ( !closed ) {
			try {
				out.flush();
				out.close();
			} finally {
				closed = true;
			}
		}
	}

	@Override
	public void canbusFrameReceived(CanbusFrame frame) {
		if ( closed ) {
			return;
		}
		final Instant now = Instant.now();
		if ( !inlineDate ) {
			out.print("# ");
		}
		out.print(DateTimeFormatter.ISO_INSTANT.format(now));
		if ( inlineDate ) {
			out.print(' ');
		} else {
			out.println();
		}
		final String f = CanbusUtils.encodeCandumpLog(frame, busName);
		out.println(f);
		if ( autoFlush ) {
			out.flush();
		}
	}

	/**
	 * Get the bus name.
	 *
	 * @return the bus name
	 */
	public String getBusName() {
		return busName;
	}

	/**
	 * Get the configured log file.
	 *
	 * @return the logFile
	 */
	public Path getLogFile() {
		return logFile;
	}

	/**
	 * Get the gzip compression setting.
	 *
	 * @return {@literal true} if the output is compressed with gzip
	 */
	public boolean isGzip() {
		return gzip;
	}

}
