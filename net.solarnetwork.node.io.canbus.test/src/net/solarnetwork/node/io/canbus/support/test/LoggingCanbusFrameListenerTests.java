/* ==================================================================
 * LoggingCanbusFrameListenerTests.java - 9/05/2022 2:22:53 pm
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

package net.solarnetwork.node.io.canbus.support.test;

import static java.lang.String.format;
import static org.hamcrest.Matchers.arrayWithSize;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.isEmptyOrNullString;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertThat;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;
import org.junit.Test;
import org.springframework.util.FileCopyUtils;
import net.solarnetwork.node.io.canbus.socketcand.FrameMessage;
import net.solarnetwork.node.io.canbus.socketcand.msg.FrameMessageImpl;
import net.solarnetwork.node.io.canbus.support.LoggingCanbusFrameListener;
import net.solarnetwork.util.ByteUtils;

/**
 * Test cases for the {@link LoggingCanbusFrameListener} class.
 *
 * @author matt
 * @version 1.0
 */
public class LoggingCanbusFrameListenerTests {

	private static final String TEST_BUS_NAME = "can0";

	@Test
	public void log_withoutDate() throws IOException {
		// GIVEN
		Path tmpFile = Files.createTempFile("canbus-debug-out-test-", ".log");

		// WHEN
		try (LoggingCanbusFrameListener listener = new LoggingCanbusFrameListener(TEST_BUS_NAME, tmpFile,
				false, true, false)) {
			FrameMessageImpl f = new FrameMessageImpl(1, false, 1, 2,
					new byte[] { (byte) 0x11, (byte) 0x00, (byte) 0xFD });
			listener.canbusFrameReceived(f);
		}

		// THEN
		String logData = FileCopyUtils.copyToString(Files.newBufferedReader(tmpFile));
		assertThat("Log data captured one line", logData, not(isEmptyOrNullString()));

		Pattern logPat = Pattern.compile(
				"# \\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}\\.\\d{3,6}Z\n\\(\\d+\\.\\d{6}\\) can0 (\\d+)#([0-9A-F]*)");
		Matcher m = logPat.matcher(logData.trim());
		assertThat("Log line formatted with comment and timestamp, address, hex data", m.matches(),
				equalTo(true));
		assertThat("Log line address", m.group(1), equalTo("1"));
		assertThat("Log line hex data", m.group(2), equalTo("1100FD"));

		Files.deleteIfExists(tmpFile);
	}

	@Test
	public void log_withDate() throws IOException {
		// GIVEN
		Path tmpFile = Files.createTempFile("canbus-debug-out-test-", ".log");

		// WHEN
		try (LoggingCanbusFrameListener listener = new LoggingCanbusFrameListener(TEST_BUS_NAME, tmpFile,
				true, true, false)) {
			FrameMessageImpl f = new FrameMessageImpl(1, false, 1, 2,
					new byte[] { (byte) 0x11, (byte) 0x00, (byte) 0xFD });
			listener.canbusFrameReceived(f);
		}

		// THEN
		String logData = FileCopyUtils.copyToString(Files.newBufferedReader(tmpFile));
		assertThat("Log data captured one line", logData, not(isEmptyOrNullString()));
		Pattern logPat = Pattern.compile(
				"\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}\\.\\d{3,6}Z \\(\\d+\\.\\d{6}\\) can0 (\\d+)#([0-9A-F]*)");
		Matcher m = logPat.matcher(logData.trim());
		assertThat("Log line formatted with comment and timestamp, address, hex data", m.matches(),
				equalTo(true));
		assertThat("Log line address", m.group(1), equalTo("1"));
		assertThat("Log line hex data", m.group(2), equalTo("1100FD"));

		Files.deleteIfExists(tmpFile);
	}

	@Test
	public void log_withDate_gzip() throws IOException {
		// GIVEN
		Path tmpFile = Files.createTempFile("canbus-debug-out-test-", ".log");

		// WHEN
		Instant start = Instant.now();
		List<FrameMessage> msgs = new ArrayList<>(16);
		try (LoggingCanbusFrameListener listener = new LoggingCanbusFrameListener(TEST_BUS_NAME, tmpFile,
				true, true, true)) {
			for ( int i = 0; i < 16; i++ ) {
				Instant now = Instant.now();
				int s = (int) ChronoUnit.SECONDS.between(start, now);
				int us = (int) (ChronoUnit.MICROS.between(start, now) - TimeUnit.SECONDS.toMicros(s));
				FrameMessageImpl f = new FrameMessageImpl(1, false, s, us,
						new byte[] { (byte) 0x11, (byte) 0x00, (byte) 0xFD });
				msgs.add(f);
				listener.canbusFrameReceived(f);
				try {
					Thread.sleep(100);
				} catch ( InterruptedException e ) {
					// ignore
				}
			}
		}

		// THEN
		String[] logData = FileCopyUtils
				.copyToString(new InputStreamReader(new GZIPInputStream(Files.newInputStream(tmpFile)),
						ByteUtils.UTF8))
				.split("\n");
		assertThat("Log data compressed and captured all lines", logData, arrayWithSize(16));
		Pattern logPat = Pattern.compile(
				"\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}\\.\\d{3,6}Z \\((\\d+)\\.(\\d{6})\\) can0 (\\d+)#([0-9A-F]*)");
		for ( int i = 0; i < logData.length; i++ ) {
			Matcher m = logPat.matcher(logData[i].trim());
			assertThat(
					String.format("Log line formatted with comment and timestamp, address, hex data: %s",
							logData[i].trim()),
					m.matches(), is(true));
			assertThat("Log seconds", m.group(1), is(String.valueOf(msgs.get(i).getSeconds())));
			assertThat("Log seconds", m.group(2), is(format("%06d", msgs.get(i).getMicroseconds())));
			assertThat("Log line address", m.group(3), is("1"));
			assertThat("Log line hex data", m.group(4), is("1100FD"));
		}
		Files.deleteIfExists(tmpFile);
	}

}
