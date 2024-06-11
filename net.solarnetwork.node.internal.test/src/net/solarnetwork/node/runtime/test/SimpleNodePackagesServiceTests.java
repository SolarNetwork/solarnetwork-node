/* ==================================================================
 * SimpleNodePackagesService.java - 12/06/2024 7:58:59 am
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

package net.solarnetwork.node.runtime.test;

import static java.util.Arrays.asList;
import static java.util.concurrent.CompletableFuture.completedFuture;
import static net.solarnetwork.domain.InstructionStatus.InstructionState.Completed;
import static net.solarnetwork.node.reactor.InstructionHandler.PARAM_SERVICE;
import static net.solarnetwork.node.reactor.InstructionHandler.PARAM_SERVICE_RESULT;
import static net.solarnetwork.node.reactor.InstructionHandler.TOPIC_SET_CONTROL_PARAMETER;
import static net.solarnetwork.node.reactor.InstructionHandler.TOPIC_SYSTEM_CONFIGURATION;
import static net.solarnetwork.node.reactor.InstructionUtils.createLocalInstruction;
import static net.solarnetwork.node.runtime.SimpleNodePackagesService.PACKAGES_SERVICE_UID;
import static net.solarnetwork.node.runtime.SimpleNodePackagesService.PARAM_COMPRESSED;
import static net.solarnetwork.node.runtime.SimpleNodePackagesService.PARAM_FILTER;
import static net.solarnetwork.node.runtime.SimpleNodePackagesService.PARAM_STATUS;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.zip.GZIPInputStream;
import org.easymock.EasyMock;
import org.junit.Test;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.solarnetwork.codec.JsonUtils;
import net.solarnetwork.node.reactor.Instruction;
import net.solarnetwork.node.reactor.InstructionStatus;
import net.solarnetwork.node.runtime.SimpleNodePackagesService;
import net.solarnetwork.node.service.PlatformPackageService;
import net.solarnetwork.node.service.PlatformPackageService.PlatformPackage;
import net.solarnetwork.node.service.support.BasicPlatformPackage;

/**
 * Test cases for the {@link SimpleNodePackagesService} class.
 *
 * @author matt
 * @version 1.0
 */
public class SimpleNodePackagesServiceTests {

	@Test
	public void handlesTopic() {
		// GIVEN
		SimpleNodePackagesService service = new SimpleNodePackagesService(Collections.emptyList());

		// WHEN
		boolean handlesSystemConfigurationTopic = service.handlesTopic(TOPIC_SYSTEM_CONFIGURATION);
		boolean handlesSetControlParameter = service.handlesTopic(TOPIC_SET_CONTROL_PARAMETER);

		// THEN
		assertThat("SystemConfiguration topic handled", handlesSystemConfigurationTopic, is(true));
		assertThat("Other topic not handled", handlesSetControlParameter, is(false));
	}

	private void assertPackage(String desc, PlatformPackage actual, PlatformPackage expected) {
		assertPackage(desc, actual, expected.getName(), expected.getVersion(), expected.isInstalled());
	}

	private void assertPackage(String desc, PlatformPackage actual, String expectedName,
			String expectedVersion, boolean expectedInstalled) {
		assertThat(desc + " not null", actual, is(notNullValue()));
		assertThat(desc + " name", actual.getName(), is(equalTo(expectedName)));
		assertThat(desc + " version", actual.getVersion(), is(equalTo(expectedVersion)));
		assertThat(desc + " installed", actual.isInstalled(), is(equalTo(expectedInstalled)));
	}

	@Test
	public void handleSystemConfigurationTopic() {
		PlatformPackageService p1 = EasyMock.createMock(PlatformPackageService.class);
		PlatformPackageService p2 = EasyMock.createMock(PlatformPackageService.class);

		List<PlatformPackage> l1 = Arrays.asList(new BasicPlatformPackage("sn-foo", "1.0", true),
				new BasicPlatformPackage("some-other-thing", "1.0", true));
		CompletableFuture<Iterable<PlatformPackage>> r1 = completedFuture(l1);

		List<PlatformPackage> l2 = Arrays.asList(new BasicPlatformPackage("solarnode-bar", "1.0", true));
		CompletableFuture<Iterable<PlatformPackage>> r2 = completedFuture(l2);

		// pass TRUE for only defaulted Installed status
		expect(p1.listNamedPackages(null, Boolean.TRUE)).andReturn(r1);
		expect(p2.listNamedPackages(null, Boolean.TRUE)).andReturn(r2);

		SimpleNodePackagesService service = new SimpleNodePackagesService(asList(p1, p2));

		// WHEN
		replay(p1, p2);
		Instruction instr = createLocalInstruction(TOPIC_SYSTEM_CONFIGURATION, PARAM_SERVICE,
				PACKAGES_SERVICE_UID);
		InstructionStatus result = service.processInstruction(instr);

		// THEN
		assertThat("Status result returned", result, is(notNullValue()));
		assertThat("State is Completed", result.getInstructionState(), is(equalTo(Completed)));
		assertThat("Result param provided as list", result.getResultParameters(),
				hasEntry(equalTo(PARAM_SERVICE_RESULT), instanceOf(List.class)));

		@SuppressWarnings("unchecked")
		List<PlatformPackage> resultPackages = (List<PlatformPackage>) result.getResultParameters()
				.get(PARAM_SERVICE_RESULT);
		assertThat("Two packages matching default filter are returned", resultPackages, hasSize(2));

		assertPackage("Package 1 from service 1", resultPackages.get(0), l1.get(0));
		assertPackage("Package 1 from service 2", resultPackages.get(1), l2.get(0));

		verify(p1, p2);
	}

	@Test
	public void handleSystemConfigurationTopic_compressed() throws IOException {
		PlatformPackageService p1 = EasyMock.createMock(PlatformPackageService.class);
		PlatformPackageService p2 = EasyMock.createMock(PlatformPackageService.class);

		List<PlatformPackage> l1 = Arrays.asList(new BasicPlatformPackage("sn-foo", "1.0", true),
				new BasicPlatformPackage("some-other-thing", "1.0", true));
		CompletableFuture<Iterable<PlatformPackage>> r1 = completedFuture(l1);

		List<PlatformPackage> l2 = Arrays.asList(new BasicPlatformPackage("solarnode-bar", "1.0", true));
		CompletableFuture<Iterable<PlatformPackage>> r2 = completedFuture(l2);

		// pass TRUE for only defaulted Installed status
		expect(p1.listNamedPackages(null, Boolean.TRUE)).andReturn(r1);
		expect(p2.listNamedPackages(null, Boolean.TRUE)).andReturn(r2);

		SimpleNodePackagesService service = new SimpleNodePackagesService(asList(p1, p2));

		// WHEN
		replay(p1, p2);
		Map<String, String> instrParams = new LinkedHashMap<>(2);
		instrParams.put(PARAM_SERVICE, PACKAGES_SERVICE_UID);
		instrParams.put(PARAM_COMPRESSED, "force");
		Instruction instr = createLocalInstruction(TOPIC_SYSTEM_CONFIGURATION, instrParams);
		InstructionStatus result = service.processInstruction(instr);

		// THEN
		assertThat("Status result returned", result, is(notNullValue()));
		assertThat("State is Completed", result.getInstructionState(), is(equalTo(Completed)));
		assertThat("Result param provided as string", result.getResultParameters(),
				hasEntry(equalTo(PARAM_SERVICE_RESULT), instanceOf(String.class)));

		String resultParameterValue = (String) result.getResultParameters().get(PARAM_SERVICE_RESULT);

		byte[] gzipResults = Base64.getDecoder().decode(resultParameterValue);
		ObjectMapper mapper = JsonUtils.newObjectMapper();
		List<BasicPlatformPackage> resultPackages = mapper.readerForListOf(BasicPlatformPackage.class)
				.readValue(new GZIPInputStream(new ByteArrayInputStream(gzipResults)));

		assertThat("Two packages matching default filter are returned", resultPackages, hasSize(2));

		assertPackage("Package 1 from service 1", resultPackages.get(0), l1.get(0));
		assertPackage("Package 1 from service 2", resultPackages.get(1), l2.get(0));

		verify(p1, p2);
	}

	@Test
	public void handleSystemConfigurationTopic_filter() {
		PlatformPackageService p1 = EasyMock.createMock(PlatformPackageService.class);
		PlatformPackageService p2 = EasyMock.createMock(PlatformPackageService.class);

		List<PlatformPackage> l1 = Arrays.asList(new BasicPlatformPackage("sn-foo", "1.0", true),
				new BasicPlatformPackage("foo-thing", "1.0", true));
		CompletableFuture<Iterable<PlatformPackage>> r1 = completedFuture(l1);

		List<PlatformPackage> l2 = Arrays.asList(new BasicPlatformPackage("solarnode-bar", "1.0", true));
		CompletableFuture<Iterable<PlatformPackage>> r2 = completedFuture(l2);

		// pass TRUE for only defaulted Installed status
		expect(p1.listNamedPackages(null, Boolean.TRUE)).andReturn(r1);
		expect(p2.listNamedPackages(null, Boolean.TRUE)).andReturn(r2);

		SimpleNodePackagesService service = new SimpleNodePackagesService(asList(p1, p2));

		// WHEN
		replay(p1, p2);
		Map<String, String> instrParams = new LinkedHashMap<>(2);
		instrParams.put(PARAM_SERVICE, PACKAGES_SERVICE_UID);
		instrParams.put(PARAM_FILTER, "^foo");
		Instruction instr = createLocalInstruction(TOPIC_SYSTEM_CONFIGURATION, instrParams);
		InstructionStatus result = service.processInstruction(instr);

		// THEN
		assertThat("Status result returned", result, is(notNullValue()));
		assertThat("State is Completed", result.getInstructionState(), is(equalTo(Completed)));
		assertThat("Result param provided as list", result.getResultParameters(),
				hasEntry(equalTo(PARAM_SERVICE_RESULT), instanceOf(List.class)));

		@SuppressWarnings("unchecked")
		List<PlatformPackage> resultPackages = (List<PlatformPackage>) result.getResultParameters()
				.get(PARAM_SERVICE_RESULT);
		assertThat("One package matching filter is returned", resultPackages, hasSize(1));

		assertPackage("Package 2 from service 1", resultPackages.get(0), l1.get(1));

		verify(p1, p2);
	}

	@Test
	public void handleSystemConfigurationTopic_status() {
		PlatformPackageService p1 = EasyMock.createMock(PlatformPackageService.class);
		PlatformPackageService p2 = EasyMock.createMock(PlatformPackageService.class);

		List<PlatformPackage> l1 = Arrays.asList(new BasicPlatformPackage("sn-foo", "1.0", true),
				new BasicPlatformPackage("foo-thing", "1.0", true));
		CompletableFuture<Iterable<PlatformPackage>> r1 = completedFuture(l1);

		List<PlatformPackage> l2 = Arrays.asList(new BasicPlatformPackage("solarnode-bar", "1.0", true));
		CompletableFuture<Iterable<PlatformPackage>> r2 = completedFuture(l2);

		// pass FALSE for only Avaialble status
		expect(p1.listNamedPackages(null, Boolean.FALSE)).andReturn(r1);
		expect(p2.listNamedPackages(null, Boolean.FALSE)).andReturn(r2);

		SimpleNodePackagesService service = new SimpleNodePackagesService(asList(p1, p2));

		// WHEN
		replay(p1, p2);
		Map<String, String> instrParams = new LinkedHashMap<>(2);
		instrParams.put(PARAM_SERVICE, PACKAGES_SERVICE_UID);
		instrParams.put(PARAM_STATUS, SimpleNodePackagesService.Status.Available.toString());
		Instruction instr = createLocalInstruction(TOPIC_SYSTEM_CONFIGURATION, instrParams);
		InstructionStatus result = service.processInstruction(instr);

		// THEN
		assertThat("Status result returned", result, is(notNullValue()));
		assertThat("State is Completed", result.getInstructionState(), is(equalTo(Completed)));
		assertThat("Result param provided as list", result.getResultParameters(),
				hasEntry(equalTo(PARAM_SERVICE_RESULT), instanceOf(List.class)));

		@SuppressWarnings("unchecked")
		List<PlatformPackage> resultPackages = (List<PlatformPackage>) result.getResultParameters()
				.get(PARAM_SERVICE_RESULT);
		assertThat("Two packages matching default filter are returned", resultPackages, hasSize(2));

		assertPackage("Package 1 from service 1", resultPackages.get(0), l1.get(0));
		assertPackage("Package 1 from service 2", resultPackages.get(1), l2.get(0));

		verify(p1, p2);
	}

}
