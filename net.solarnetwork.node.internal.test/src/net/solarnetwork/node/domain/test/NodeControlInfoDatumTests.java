/* ==================================================================
 * NodeControlInfoDatumTests.java - 27/09/2017 8:59:29 AM
 * 
 * Copyright 2017 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.domain.test;

import static org.hamcrest.Matchers.arrayContaining;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;
import java.util.Date;
import java.util.Map;
import org.junit.Test;
import net.solarnetwork.domain.NodeControlPropertyType;
import net.solarnetwork.node.domain.NodeControlInfoDatum;

/**
 * Test cases for the {@link NodeControlInfoDatum} class.
 * 
 * @author matt
 * @version 1.0
 */
public class NodeControlInfoDatumTests {

	@Test
	public void asSimpleMap() {
		NodeControlInfoDatum datum = new NodeControlInfoDatum();
		datum.setCreated(new Date());
		datum.setPropertyName("test-property");
		datum.setReadonly(false);
		datum.setSourceId("test-source");
		datum.setType(NodeControlPropertyType.Boolean);
		datum.setValue("true");

		Map<String, ?> map = datum.asSimpleMap();
		assertThat(map, hasEntry("_DatumType", (Object) "net.solarnetwork.domain.NodeControlInfo"));
		assertThat((String[]) map.get("_DatumTypes"), arrayContaining(
				"net.solarnetwork.domain.NodeControlInfo", "net.solarnetwork.node.domain.Datum"));
		assertThat(map, hasEntry("created", (Object) datum.getCreated().getTime()));
		assertThat(map, hasEntry("propertyName", (Object) "test-property"));
		assertThat(map, hasEntry("readonly", (Object) false));
		assertThat(map, hasEntry("sourceId", (Object) "test-source"));
		assertThat(map, hasEntry("type", (Object) "Boolean"));
		assertThat(map, hasEntry("value", (Object) "true"));
		assertThat(map, hasEntry("controlId", (Object) "test-source"));
		assertThat("Map size", map.keySet(), hasSize(9));
	}

}
