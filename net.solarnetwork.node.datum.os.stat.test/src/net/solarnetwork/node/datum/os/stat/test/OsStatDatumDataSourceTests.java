/* ==================================================================
 * OsStatDatumDataSourceTests.java - 13/08/2018 10:54:40 AM
 * 
 * Copyright 2018 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.datum.os.stat.test;

import static java.util.Collections.singleton;
import static org.easymock.EasyMock.expect;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.hamcrest.Matchers.hasEntry;
import static org.junit.Assert.assertThat;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import org.easymock.Capture;
import org.easymock.EasyMock;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import net.solarnetwork.domain.GeneralDatumMetadata;
import net.solarnetwork.node.NodeMetadataService;
import net.solarnetwork.node.datum.os.stat.ActionCommandRunner;
import net.solarnetwork.node.datum.os.stat.OsStatDatumDataSource;
import net.solarnetwork.node.datum.os.stat.ProcessActionCommandRunner;
import net.solarnetwork.node.datum.os.stat.StatAction;
import net.solarnetwork.node.domain.GeneralNodeDatum;
import net.solarnetwork.util.StaticOptionalService;

/**
 * Test cases for the {@link OsStatDatumDataSource} class.
 * 
 * @author matt
 * @version 1.1
 */
public class OsStatDatumDataSourceTests {

	private ActionCommandRunner runner;
	private NodeMetadataService nodeMetadataService;

	@Before
	public void setup() {
		runner = EasyMock.createMock(ActionCommandRunner.class);
		nodeMetadataService = EasyMock.createMock(NodeMetadataService.class);
	}

	@After
	public void teardown() {
		EasyMock.verify(runner, nodeMetadataService);
	}

	private void replayAll() {
		EasyMock.replay(runner, nodeMetadataService);
	}

	private OsStatDatumDataSource dataSourceInstance(Set<StatAction> actions) {
		OsStatDatumDataSource ds = new OsStatDatumDataSource();
		ds.setCommandRunner(runner);
		ds.setActions(actions);
		return ds;
	}

	private OsStatDatumDataSource dataSourceInstanceCustom(Set<String> actions) {
		OsStatDatumDataSource ds = new OsStatDatumDataSource();
		ds.setCommandRunner(runner);
		ds.setActionSet(actions);
		return ds;
	}

	@Test
	public void populateFsUse() throws IOException {
		// given
		List<Map<String, String>> rows = ProcessActionCommandRunner
				.parseActionCommandCsvOutput(getClass().getResourceAsStream("fs-use-01.csv"));
		expect(runner.executeAction(StatAction.FilesystemUse.getAction())).andReturn(rows);

		// when
		replayAll();
		OsStatDatumDataSource ds = dataSourceInstance(EnumSet.of(StatAction.FilesystemUse));
		GeneralNodeDatum result = ds.readCurrentDatum();

		// then
		assertThat("Result returned", result, notNullValue());
		Map<String, Number> iData = result.getSamples().getInstantaneous();
		assertThat("/run size", iData, hasEntry("fs_size_/run", new BigDecimal(412901376)));
		assertThat("/run used", iData, hasEntry("fs_used_/run", new BigDecimal(12562432)));
		assertThat("/run used percent", iData, hasEntry("fs_used_percent_/run", new BigDecimal(4)));
		assertThat("/ size", iData, hasEntry("fs_size_/", new BigDecimal(20358017024L)));
		assertThat("/ used", iData, hasEntry("fs_used_/", new BigDecimal(12680704000L)));
		assertThat("/ used percent", iData, hasEntry("fs_used_percent_/", new BigDecimal(66)));
	}

	@Test
	public void populateCpuUse() throws IOException {
		// given
		List<Map<String, String>> rows = ProcessActionCommandRunner
				.parseActionCommandCsvOutput(getClass().getResourceAsStream("cpu-use-01.csv"));
		expect(runner.executeAction(StatAction.CpuUse.getAction())).andReturn(rows);

		// when
		replayAll();
		OsStatDatumDataSource ds = dataSourceInstance(EnumSet.of(StatAction.CpuUse));
		GeneralNodeDatum result = ds.readCurrentDatum();

		// then
		assertThat("Result returned", result, notNullValue());
		Map<String, Number> iData = result.getSamples().getInstantaneous();
		assertThat("User", iData, hasEntry("cpu_user", new BigDecimal("0.03")));
		assertThat("User", iData, hasEntry("cpu_system", new BigDecimal("0.03")));
		assertThat("User", iData, hasEntry("cpu_idle", new BigDecimal("99.93")));
	}

	@Test
	public void populateCpuUseOsX() throws IOException {
		// given
		List<Map<String, String>> rows = ProcessActionCommandRunner
				.parseActionCommandCsvOutput(getClass().getResourceAsStream("cpu-use-02.csv"));
		expect(runner.executeAction(StatAction.CpuUse.getAction())).andReturn(rows);

		// when
		replayAll();
		OsStatDatumDataSource ds = dataSourceInstance(EnumSet.of(StatAction.CpuUse));
		GeneralNodeDatum result = ds.readCurrentDatum();

		// then
		assertThat("Result returned", result, notNullValue());
		Map<String, Number> iData = result.getSamples().getInstantaneous();
		assertThat("User", iData, hasEntry("cpu_user", new BigDecimal("20.8")));
	}

	@Test
	public void populateCpuUseMultipleRecords() throws IOException {
		// given
		List<Map<String, String>> rows = ProcessActionCommandRunner
				.parseActionCommandCsvOutput(getClass().getResourceAsStream("cpu-use-03.csv"));
		expect(runner.executeAction(StatAction.CpuUse.getAction())).andReturn(rows);

		// when
		replayAll();
		OsStatDatumDataSource ds = dataSourceInstance(EnumSet.of(StatAction.CpuUse));
		GeneralNodeDatum result = ds.readCurrentDatum();

		// then
		assertThat("Result returned", result, notNullValue());
		Map<String, Number> iData = result.getSamples().getInstantaneous();
		assertThat("User", iData, hasEntry("cpu_user", new BigDecimal("0.03")));
		assertThat("User", iData, hasEntry("cpu_system", new BigDecimal("0.03")));
		assertThat("User", iData, hasEntry("cpu_idle", new BigDecimal("99.93")));
	}

	@Test
	public void populateNetTraffic() throws IOException {
		// given
		List<Map<String, String>> rows = ProcessActionCommandRunner
				.parseActionCommandCsvOutput(getClass().getResourceAsStream("net-traffic-01.csv"));
		expect(runner.executeAction(StatAction.NetworkTraffic.getAction())).andReturn(rows);

		// when
		replayAll();
		OsStatDatumDataSource ds = dataSourceInstance(EnumSet.of(StatAction.NetworkTraffic));
		ds.setNetDevices(new HashSet<>(Arrays.asList("eth0", "wlan0")));
		GeneralNodeDatum result = ds.readCurrentDatum();

		// then
		assertThat("Result returned", result, notNullValue());
		Map<String, Number> aData = result.getSamples().getAccumulating();
		assertThat("eth0 bytes-in", aData, hasEntry("net_bytes_in_eth0", new BigDecimal(9348)));
		assertThat("eth0 bytes-out", aData, hasEntry("net_bytes_out_eth0", new BigDecimal(10031)));
		assertThat("eth0 packets-in", aData, hasEntry("net_packets_in_eth0", new BigDecimal(80)));
		assertThat("eth0 packets-out", aData, hasEntry("net_packets_out_eth0", new BigDecimal(81)));
		assertThat("wlan0 bytes-in", aData, hasEntry("net_bytes_in_wlan0", new BigDecimal(9161)));
		assertThat("wlan0 bytes-out", aData, hasEntry("net_bytes_out_wlan0", new BigDecimal(5970)));
		assertThat("wlan0 packets-in", aData, hasEntry("net_packets_in_wlan0", new BigDecimal(86)));
		assertThat("wlan0 packets-out", aData, hasEntry("net_packets_out_wlan0", new BigDecimal(38)));
	}

	@Test
	public void populateMemoryUse() throws IOException {
		// given
		List<Map<String, String>> rows = ProcessActionCommandRunner
				.parseActionCommandCsvOutput(getClass().getResourceAsStream("mem-use-01.csv"));
		expect(runner.executeAction(StatAction.MemoryUse.getAction())).andReturn(rows);

		// when
		replayAll();
		OsStatDatumDataSource ds = dataSourceInstance(EnumSet.of(StatAction.MemoryUse));
		GeneralNodeDatum result = ds.readCurrentDatum();

		// then
		assertThat("Result returned", result, notNullValue());
		Map<String, Number> iData = result.getSamples().getInstantaneous();
		assertThat("Total", iData, hasEntry("ram_total", new BigDecimal("34359738368")));
		assertThat("Avail", iData, hasEntry("ram_avail", new BigDecimal("9733586944")));
		assertThat("Used percent", iData, hasEntry("ram_used_percent", new BigDecimal("71.7")));
	}

	@Test
	public void populateSystemLoad() throws IOException {
		// given
		List<Map<String, String>> rows = ProcessActionCommandRunner
				.parseActionCommandCsvOutput(getClass().getResourceAsStream("sys-load-01.csv"));
		expect(runner.executeAction(StatAction.SystemLoad.getAction())).andReturn(rows);

		// when
		replayAll();
		OsStatDatumDataSource ds = dataSourceInstance(EnumSet.of(StatAction.SystemLoad));
		GeneralNodeDatum result = ds.readCurrentDatum();

		// then
		assertThat("Result returned", result, notNullValue());
		Map<String, Number> iData = result.getSamples().getInstantaneous();
		assertThat("1min load", iData, hasEntry("sys_load_1min", new BigDecimal("0.09")));
		assertThat("5min load", iData, hasEntry("sys_load_5min", new BigDecimal("0.10")));
		assertThat("15min load", iData, hasEntry("sys_load_15min", new BigDecimal("0.07")));
	}

	@Test
	public void populateSystemUptime() throws IOException {
		// given
		List<Map<String, String>> rows = ProcessActionCommandRunner
				.parseActionCommandCsvOutput(getClass().getResourceAsStream("sys-up-01.csv"));
		expect(runner.executeAction(StatAction.SystemUptime.getAction())).andReturn(rows);

		// when
		replayAll();
		OsStatDatumDataSource ds = dataSourceInstance(EnumSet.of(StatAction.SystemUptime));
		GeneralNodeDatum result = ds.readCurrentDatum();

		// then
		assertThat("Result returned", result, notNullValue());
		Map<String, Number> aData = result.getSamples().getAccumulating();
		assertThat("Uptime", aData, hasEntry("sys_up", new BigDecimal("26483.63")));
	}

	@Test
	public void cachedSample() throws IOException, InterruptedException {
		// given
		List<Map<String, String>> rows = ProcessActionCommandRunner
				.parseActionCommandCsvOutput(getClass().getResourceAsStream("sys-up-01.csv"));
		expect(runner.executeAction(StatAction.SystemUptime.getAction())).andReturn(rows);

		// when
		replayAll();
		OsStatDatumDataSource ds = dataSourceInstance(EnumSet.of(StatAction.SystemUptime));
		ds.setSampleCacheMs(TimeUnit.DAYS.toMillis(7));
		GeneralNodeDatum result = ds.readCurrentDatum();

		Thread.sleep(20);

		GeneralNodeDatum cachedResult = ds.readCurrentDatum();

		// then
		assertThat("Result returned", result, notNullValue());
		Map<String, Number> aData = result.getSamples().getAccumulating();
		assertThat("Uptime", aData, hasEntry("sys_up", new BigDecimal("26483.63")));

		assertThat("Cached result returned", cachedResult, sameInstance(result));
	}

	@Test
	public void metadataPostedOncePerSample() throws IOException, InterruptedException {
		// given
		List<Map<String, String>> rows = ProcessActionCommandRunner
				.parseActionCommandCsvOutput(getClass().getResourceAsStream("sys-up-01.csv"));
		expect(runner.executeAction(StatAction.SystemUptime.getAction())).andReturn(rows);

		Capture<GeneralDatumMetadata> metadataCaptor = new Capture<>();
		nodeMetadataService.addNodeMetadata(EasyMock.capture(metadataCaptor));

		// when
		replayAll();
		OsStatDatumDataSource ds = dataSourceInstance(EnumSet.of(StatAction.SystemUptime));
		ds.setSampleCacheMs(TimeUnit.DAYS.toMillis(7));
		ds.setNodeMetadataService(new StaticOptionalService<NodeMetadataService>(nodeMetadataService));
		ds.readCurrentDatum();

		Thread.sleep(20);

		// to verify metadata posted just once
		ds.readCurrentDatum();

		// then
		assertThat("Metadata posted", metadataCaptor.hasCaptured(), equalTo(true));
		GeneralDatumMetadata meta = metadataCaptor.getValue();
		Map<String, Map<String, Object>> pm = meta.getPropertyInfo();
		assertThat("OS metadata", pm.get("os"), notNullValue(Map.class));
		Map<String, Object> os = pm.get("os");
		assertThat("OS name", os, hasEntry("name", System.getProperty("os.name")));
		assertThat("OS arch", os, hasEntry("arch", System.getProperty("os.arch")));
		assertThat("OS version", os, hasEntry("version", System.getProperty("os.version")));
	}

	@Test
	public void populateCustomStats() throws IOException, InterruptedException {
		// given
		List<Map<String, String>> rows = ProcessActionCommandRunner
				.parseActionCommandCsvOutput(getClass().getResourceAsStream("custom-01.csv"));
		expect(runner.executeAction("custom")).andReturn(rows);

		// when
		replayAll();
		OsStatDatumDataSource ds = dataSourceInstanceCustom(singleton("custom"));
		GeneralNodeDatum result = ds.readCurrentDatum();

		// then
		assertThat("Result returned", result, notNullValue());
		Map<String, Number> iData = result.getSamples().getInstantaneous();
		assertThat("Instantaneous cpu_temp", iData, hasEntry("cpu_temp", new BigDecimal("30.1")));
		Map<String, Number> aData = result.getSamples().getAccumulating();
		assertThat("Accumulating cpu_energy", aData, hasEntry("cpu_energy", new BigDecimal("12345678")));
		Map<String, Object> sData = result.getSamples().getStatus();
		assertThat("Status cpu_state", sData, hasEntry("cpu_state", "ok"));
	}
}
