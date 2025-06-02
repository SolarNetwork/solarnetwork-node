/* ==================================================================
 * SocketcandCanbusNetworkTests.java - 9/05/2022 1:55:25 pm
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

package net.solarnetwork.node.io.canbus.socketcand.test;

import static java.lang.String.format;
import static org.easymock.EasyMock.expect;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.isEmptyOrNullString;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertThat;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;
import org.easymock.EasyMock;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.util.FileCopyUtils;
import net.solarnetwork.domain.InstructionStatus.InstructionState;
import net.solarnetwork.node.io.canbus.CanbusConnection;
import net.solarnetwork.node.io.canbus.CanbusFrameListener;
import net.solarnetwork.node.io.canbus.socketcand.CanbusSocketProvider;
import net.solarnetwork.node.io.canbus.socketcand.SocketcandCanbusConnection;
import net.solarnetwork.node.io.canbus.socketcand.SocketcandCanbusNetwork;
import net.solarnetwork.node.io.canbus.socketcand.msg.FrameMessageImpl;
import net.solarnetwork.node.io.canbus.support.LoggingCanbusFrameListener;
import net.solarnetwork.node.reactor.InstructionHandler;
import net.solarnetwork.node.reactor.InstructionStatus;
import net.solarnetwork.node.reactor.InstructionUtils;
import net.solarnetwork.util.ByteUtils;

/**
 * Test cases for the {@link SocketcandCanbusNetwork} class.
 *
 * @author matt
 * @version 1.0
 */
public class SocketcandCanbusNetworkTests {

	private static final String TEST_CAN_BUS_NAME = "can0";

	private static final Pattern DEBUG_LOG_PAT = Pattern.compile(
			"\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}\\.\\d{3,6}Z \\((\\d+)\\.(\\d{6})\\) can0 (\\d+)#([0-9A-F]*)");

	private CanbusSocketProvider socketProvider;
	private Executor executor;
	private SocketcandCanbusNetwork service;

	private TestCanbusSocket socket;

	@Before
	public void setup() {
		socketProvider = EasyMock.createMock(CanbusSocketProvider.class);
		executor = Executors.newCachedThreadPool();
		service = new SocketcandCanbusNetwork(socketProvider, executor);
		socket = new TestCanbusSocket();
	}

	@After
	public void teardown() {
		EasyMock.verify(socketProvider);
	}

	private void replayAll() {
		EasyMock.replay(socketProvider);
	}

	@Test
	public void frameReceived_captureStartStop() throws Exception {
		// GIVEN
		/*-dataSource.setSourceId(TEST_SOURCE);
		dataSource.setBusName("can0");
		dataSource.setDebug(true);
		dataSource.setDebugLogPath(tmpFile.toAbsolutePath().toString());
		*/

		expect(socketProvider.createCanbusSocket()).andReturn(socket);

		// WHEN
		replayAll();
		Map<String, String> startParams = new LinkedHashMap<>(2);
		startParams.put(SocketcandCanbusNetwork.CANBUS_CAPTURE_SIGNAL_PARAM, TEST_CAN_BUS_NAME);
		startParams.put(SocketcandCanbusNetwork.CANBUS_CAPTURE_ACTION_PARAM,
				SocketcandCanbusNetwork.CanbusCaptureAction.Start.toString());
		InstructionStatus startStatus = service.processInstruction(
				InstructionUtils.createLocalInstruction(InstructionHandler.TOPIC_SIGNAL, startParams));

		FrameMessageImpl f = new FrameMessageImpl(1, false, 1, 2,
				new byte[] { (byte) 0x11, (byte) 0x00, (byte) 0xFD });
		socket.respondMessage(f);

		Thread.sleep(500);

		CanbusConnection conn = service.capturingConnection(TEST_CAN_BUS_NAME);
		assertThat("Capturing connection is available", conn,
				is(instanceOf(SocketcandCanbusConnection.class)));
		CanbusFrameListener listener = ((SocketcandCanbusConnection) conn).getMonitoringListener();
		assertThat("Logging listener configured", listener,
				is(instanceOf(LoggingCanbusFrameListener.class)));
		Path logFile = ((LoggingCanbusFrameListener) listener).getLogFile();

		try {
			Map<String, String> stopParams = new LinkedHashMap<>(2);
			stopParams.put(SocketcandCanbusNetwork.CANBUS_CAPTURE_SIGNAL_PARAM, TEST_CAN_BUS_NAME);
			stopParams.put(SocketcandCanbusNetwork.CANBUS_CAPTURE_ACTION_PARAM,
					SocketcandCanbusNetwork.CanbusCaptureAction.Stop.toString());
			InstructionStatus stopStatus = service.processInstruction(InstructionUtils
					.createLocalInstruction(InstructionHandler.TOPIC_SIGNAL, stopParams));

			// THEN
			assertThat("Start processed OK", startStatus.getInstructionState(),
					is(InstructionState.Completed));
			assertThat("Stop processed OK", stopStatus.getInstructionState(),
					is(InstructionState.Completed));

			String logData = FileCopyUtils.copyToString(new InputStreamReader(
					new GZIPInputStream(Files.newInputStream(logFile)), ByteUtils.UTF8));
			assertThat("Log data captured one line", logData, not(isEmptyOrNullString()));
			Matcher m = DEBUG_LOG_PAT.matcher(logData.trim());
			assertThat("Log line formatted with comment and timestamp, address, hex data", m.matches(),
					equalTo(true));
			assertThat("Log seconds", m.group(1), is(String.valueOf(f.getSeconds())));
			assertThat("Log seconds", m.group(2), is(format("%06d", f.getMicroseconds())));
			assertThat("Log line address", m.group(3), equalTo("1"));
			assertThat("Log line hex data", m.group(4), equalTo("1100FD"));
		} finally {
			Files.deleteIfExists(logFile);
		}
	}

}
