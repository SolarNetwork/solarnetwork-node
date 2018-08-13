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

import static org.easymock.EasyMock.expect;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.Matchers.hasEntry;
import static org.junit.Assert.assertThat;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.easymock.EasyMock;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import net.solarnetwork.node.datum.os.stat.ActionCommandRunner;
import net.solarnetwork.node.datum.os.stat.OsStatDatumDataSource;
import net.solarnetwork.node.datum.os.stat.ProcessActionCommandRunner;
import net.solarnetwork.node.datum.os.stat.StatAction;
import net.solarnetwork.node.domain.GeneralNodeDatum;

/**
 * Test cases for the {@link OsStatDatumDataSource} class.
 * 
 * @author matt
 * @version 1.0
 */
public class OsStatDatumDataSourceTests {

	private ActionCommandRunner runner;

	@Before
	public void setup() {
		runner = EasyMock.createMock(ActionCommandRunner.class);
	}

	@After
	public void teardown() {
		EasyMock.verify(runner);
	}

	private void replayAll() {
		EasyMock.replay(runner);
	}

	private OsStatDatumDataSource dataSourceInstance(Set<StatAction> actions) {
		OsStatDatumDataSource ds = new OsStatDatumDataSource();
		ds.setCommandRunner(runner);
		ds.setActions(actions);
		return ds;
	}

	@Test
	public void populateFsUse() throws IOException {
		// given
		List<Map<String, String>> rows = ProcessActionCommandRunner
				.parseActionCommandCsvOutput(getClass().getResourceAsStream("fs-use-01.csv"));
		expect(runner.executeAction(StatAction.FilesystemUse)).andReturn(rows);

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
		expect(runner.executeAction(StatAction.CpuUse)).andReturn(rows);

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
		expect(runner.executeAction(StatAction.CpuUse)).andReturn(rows);

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
		expect(runner.executeAction(StatAction.CpuUse)).andReturn(rows);

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
		expect(runner.executeAction(StatAction.NetworkTraffic)).andReturn(rows);

		// when
		replayAll();
		OsStatDatumDataSource ds = dataSourceInstance(EnumSet.of(StatAction.NetworkTraffic));
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
	public void populateSystemLoad() throws IOException {
		// given
		List<Map<String, String>> rows = ProcessActionCommandRunner
				.parseActionCommandCsvOutput(getClass().getResourceAsStream("sys-load-01.csv"));
		expect(runner.executeAction(StatAction.SystemLoad)).andReturn(rows);

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
		expect(runner.executeAction(StatAction.SystemUptime)).andReturn(rows);

		// when
		replayAll();
		OsStatDatumDataSource ds = dataSourceInstance(EnumSet.of(StatAction.SystemUptime));
		GeneralNodeDatum result = ds.readCurrentDatum();

		// then
		assertThat("Result returned", result, notNullValue());
		Map<String, Number> aData = result.getSamples().getAccumulating();
		assertThat("Uptime", aData, hasEntry("sys_up", new BigDecimal("26483.63")));
	}
}
