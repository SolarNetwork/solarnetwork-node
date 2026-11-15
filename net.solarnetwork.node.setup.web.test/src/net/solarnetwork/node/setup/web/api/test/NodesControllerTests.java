/* ==================================================================
 * NodesControllerTests.java - 13 Nov 2026 4:52:09 pm
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

package net.solarnetwork.node.setup.web.api.test;

import static org.assertj.core.api.BDDAssertions.and;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;
import net.solarnetwork.domain.datum.ObjectDatumKind;
import net.solarnetwork.domain.datum.ObjectDatumStreamMetadata;
import net.solarnetwork.node.dao.NodeDatumFilter;
import net.solarnetwork.node.setup.web.api.NodesController;

/**
 * Test cases for the {@link NodesController} class.
 *
 * @author matt
 * @version 1.0
 */
@SuppressWarnings("static-access")
@RunWith(MockitoJUnitRunner.class)
public class NodesControllerTests extends SolarQueryApiTestSupport {

	private static final String NODES_PATH = "/solarquery/api/v1/sec/nodes";
	private static final String SOURCES_PATH = "/solarquery/api/v1/sec/nodes/sources";

	@Captor
	private ArgumentCaptor<NodeDatumFilter> filterCaptor;

	private MockMvc mvc;

	@Before
	public void setup() {
		setupSupport();
		mvc = mockMvc(new NodesController(identityService, historianSelector));
	}

	private void givenSources(String... sourceIds) {
		final ObjectDatumStreamMetadata[] metas = new ObjectDatumStreamMetadata[sourceIds.length];
		for ( int i = 0; i < sourceIds.length; i++ ) {
			metas[i] = nodeMeta(NODE_ID, sourceIds[i], new String[] { "a" }, null, null);
		}
		given(filteredService.service()).willReturn(filtered);
		given(filtered.findDatumStreamMetadata(filterCaptor.capture())).willReturn(List.of(metas));
	}

	@Test
	public void nodes() throws Exception {
		// GIVEN
		given(identityService.getNodeId()).willReturn(NODE_ID);

		// WHEN
		final MockHttpServletResponse res = mvc
				.perform(get(NODES_PATH).accept(MediaType.APPLICATION_JSON)).andReturn().getResponse();

		// THEN
		and.then(res.getStatus()).as("OK").isEqualTo(200);
		and.then(res.getContentAsString()).as("Node ID provided").isEqualTo("""
				{"success":true,"data":[123]}""");
	}

	@Test
	public void nodes_apiPath() throws Exception {
		// GIVEN
		given(identityService.getNodeId()).willReturn(NODE_ID);

		// WHEN
		final MockHttpServletResponse res = mvc
				.perform(get("/api/v1/sec/nodes").accept(MediaType.APPLICATION_JSON)).andReturn()
				.getResponse();

		// THEN
		and.then(res.getStatus()).as("OK").isEqualTo(200);
		and.then(res.getContentAsString()).as("Node ID provided under API path").isEqualTo("""
				{"success":true,"data":[123]}""");
	}

	@Test
	public void nodes_notAssociated() throws Exception {
		// GIVEN
		given(identityService.getNodeId()).willReturn(null);

		// WHEN
		final MockHttpServletResponse res = mvc
				.perform(get(NODES_PATH).accept(MediaType.APPLICATION_JSON)).andReturn().getResponse();

		// THEN
		and.then(res.getStatus()).as("OK").isEqualTo(200);
		and.then(res.getContentAsString()).as("No node IDs provided when not associated").isEqualTo("""
				{"success":true,"data":[]}""");
	}

	@Test
	public void nodes_cbor() throws Exception {
		// GIVEN
		given(identityService.getNodeId()).willReturn(NODE_ID);

		// WHEN
		final MockHttpServletResponse res = mvc
				.perform(get(NODES_PATH).accept(MediaType.APPLICATION_CBOR)).andReturn().getResponse();

		// THEN
		and.then(res.getStatus()).as("OK").isEqualTo(200);
		and.then(res.getContentType()).as("CBOR content").isEqualTo(MediaType.APPLICATION_CBOR_VALUE);
		and.then(cborJson(res.getContentAsByteArray())).as("Node ID provided as CBOR").isEqualTo("""
				{"success":true,"data":[123]}""");
	}

	@Test
	public void nodes_xml() throws Exception {
		// WHEN
		final MockHttpServletResponse res = mvc
				.perform(get(NODES_PATH).accept(MediaType.APPLICATION_XML)).andReturn().getResponse();

		// THEN
		and.then(res.getStatus()).as("XML is not acceptable").isEqualTo(406);
		then(identityService).shouldHaveNoInteractions();
	}

	@Test
	public void sources() throws Exception {
		// GIVEN
		given(identityService.getNodeId()).willReturn(NODE_ID);
		givenSources("b", "a/2", "a/1");

		// WHEN
		final MockHttpServletResponse res = mvc
				.perform(get(SOURCES_PATH).accept(MediaType.APPLICATION_JSON)).andReturn().getResponse();

		// THEN
		and.then(res.getStatus()).as("OK").isEqualTo(200);
		and.then(res.getContentAsString()).as("Sources provided, ordered by source ID").isEqualTo("""
				{"success":true,"data":[\
				{"nodeId":123,"sourceId":"a/1"},\
				{"nodeId":123,"sourceId":"a/2"},\
				{"nodeId":123,"sourceId":"b"}\
				]}""");
		final NodeDatumFilter filter = filterCaptor.getValue();
		and.then(filter.getObjectKind()).as("Node streams queried").isEqualTo(ObjectDatumKind.Node);
		and.then(filter.getObjectIds()).as("The node's own ID queried").containsExactly(NODE_ID);
	}

	@Test
	public void sources_ownNodeAmongOthers() throws Exception {
		// GIVEN
		given(identityService.getNodeId()).willReturn(NODE_ID);
		givenSources("a");

		// WHEN
		final MockHttpServletResponse res = mvc.perform(get(SOURCES_PATH).param("nodeIds", "1")
				.param("nodeIds", NODE_ID.toString()).accept(MediaType.APPLICATION_JSON)).andReturn()
				.getResponse();

		// THEN
		and.then(res.getStatus()).as("OK").isEqualTo(200);
		and.then(res.getContentAsString()).as("Sources of the node provided").isEqualTo("""
				{"success":true,"data":[{"nodeId":123,"sourceId":"a"}]}""");
		and.then(filterCaptor.getValue().getObjectIds()).as("Only the node's own ID queried")
				.containsExactly(NODE_ID);
	}

	@Test
	public void sources_otherNodesOnly() throws Exception {
		// GIVEN
		given(identityService.getNodeId()).willReturn(NODE_ID);

		// WHEN
		final MockHttpServletResponse res = mvc
				.perform(get(SOURCES_PATH).param("nodeIds", "1,2").accept(MediaType.APPLICATION_JSON))
				.andReturn().getResponse();

		// THEN
		and.then(res.getStatus()).as("OK").isEqualTo(200);
		and.then(res.getContentAsString()).as("No sources provided for other nodes").isEqualTo("""
				{"success":true,"data":[]}""");
		then(filteredService).shouldHaveNoInteractions();
		then(unfilteredService).shouldHaveNoInteractions();
	}

	@Test
	public void sources_notAssociated() throws Exception {
		// GIVEN
		given(identityService.getNodeId()).willReturn(null);

		// WHEN
		final MockHttpServletResponse res = mvc
				.perform(get(SOURCES_PATH).accept(MediaType.APPLICATION_JSON)).andReturn().getResponse();

		// THEN
		and.then(res.getStatus()).as("OK").isEqualTo(200);
		and.then(res.getContentAsString()).as("No sources provided when not associated").isEqualTo("""
				{"success":true,"data":[]}""");
		then(filteredService).shouldHaveNoInteractions();
	}

	@Test
	public void sources_historianNotAvailable() throws Exception {
		// GIVEN
		given(identityService.getNodeId()).willReturn(NODE_ID);

		// WHEN
		final MockHttpServletResponse res = mvc
				.perform(get(SOURCES_PATH).accept(MediaType.APPLICATION_JSON)).andReturn().getResponse();

		// THEN
		and.then(res.getStatus()).as("OK").isEqualTo(200);
		and.then(res.getContentAsString()).as("No sources provided without a historian").isEqualTo("""
				{"success":true,"data":[]}""");
	}

	@Test
	public void sources_unfiltered() throws Exception {
		// GIVEN
		given(identityService.getNodeId()).willReturn(NODE_ID);
		given(unfilteredService.service()).willReturn(unfiltered);
		given(unfiltered.findDatumStreamMetadata(any()))
				.willReturn(List.of(nodeMeta(NODE_ID, "u", new String[] { "a" }, null, null)));

		// WHEN
		final MockHttpServletResponse res = mvc
				.perform(get("/unfiltered" + SOURCES_PATH).accept(MediaType.APPLICATION_JSON))
				.andReturn().getResponse();

		// THEN
		and.then(res.getStatus()).as("OK").isEqualTo(200);
		and.then(res.getContentAsString()).as("Sources of unfiltered historian provided").isEqualTo("""
				{"success":true,"data":[{"nodeId":123,"sourceId":"u"}]}""");
		then(filteredService).shouldHaveNoInteractions();
	}

	@Test
	public void sources_cbor() throws Exception {
		// GIVEN
		given(identityService.getNodeId()).willReturn(NODE_ID);
		givenSources("a");

		// WHEN
		final MockHttpServletResponse res = mvc
				.perform(get(SOURCES_PATH).accept(MediaType.APPLICATION_CBOR)).andReturn().getResponse();

		// THEN
		and.then(res.getStatus()).as("OK").isEqualTo(200);
		and.then(cborJson(res.getContentAsByteArray())).as("Sources provided as CBOR").isEqualTo("""
				{"success":true,"data":[{"nodeId":123,"sourceId":"a"}]}""");
	}

	@Test
	public void sources_csv() throws Exception {
		// GIVEN
		given(identityService.getNodeId()).willReturn(NODE_ID);
		givenSources("b", "a");

		// WHEN
		final MockHttpServletResponse res = mvc
				.perform(get(SOURCES_PATH).accept(MediaType.valueOf("text/csv"))).andReturn()
				.getResponse();

		// THEN
		and.then(res.getStatus()).as("OK").isEqualTo(200);
		and.then(res.getContentAsString()).as("Sources provided as CSV").isEqualTo("""
				nodeId,sourceId\r
				123,a\r
				123,b\r
				""");
	}

	@Test
	public void sources_ignoredParameters() throws Exception {
		// GIVEN
		given(identityService.getNodeId()).willReturn(NODE_ID);
		givenSources("a/1");

		// WHEN
		final MockHttpServletResponse res = mvc
				.perform(get(SOURCES_PATH).param("sourceIds", "a/*").param("combiningType", "Sum")
						.param("metadataFilter", "(foo=bar)").param("aggregation", "Fortnightly")
						.param("foo", "bar").accept(MediaType.APPLICATION_JSON))
				.andReturn().getResponse();

		// THEN
		and.then(res.getStatus()).as("OK with unsupported parameters").isEqualTo(200);
		and.then(filterCaptor.getValue().getSourceIds()).as("Source IDs criteria applied")
				.containsExactly("a/*");
		and.then(filterCaptor.getValue().getAggregation()).as("Unknown aggregation ignored").isNull();
	}

}
