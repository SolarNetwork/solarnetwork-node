/* ==================================================================
 * JaxbSnKcdParserTests.java - 14/09/2019 7:47:38 am
 *
 * Copyright 2019 SolarNetwork.net Dev Team
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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.notNullValue;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.GZIPOutputStream;
import javax.xml.transform.stream.StreamSource;
import org.junit.Test;
import org.springframework.util.FileCopyUtils;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBElement;
import jakarta.xml.bind.Unmarshaller;
import net.solarnetwork.node.io.canbus.kcd.NetworkDefinitionType;
import net.solarnetwork.node.io.canbus.support.JaxbSnKcdParser;

/**
 * Test cases for the {@link JaxbSnKcdParser} class.
 *
 * @author matt
 * @version 2.0
 */
public class JaxbSnKcdParserTests {

	@Test
	public void parseGzipStream() throws IOException {
		NetworkDefinitionType d = null;
		// compress test file as GZIP, then read GZIP stream
		try (InputStream rawIn = getClass().getResourceAsStream("kcd-test-01.xml");
				ByteArrayOutputStream gzipData = new ByteArrayOutputStream();
				GZIPOutputStream gzipOut = new GZIPOutputStream(gzipData)) {
			FileCopyUtils.copy(rawIn, gzipOut);
			try (InputStream compressedIn = new ByteArrayInputStream(gzipData.toByteArray())) {
				d = new JaxbSnKcdParser().parseKcd(compressedIn, false);
			}
		}
		assertTest01Document(d);
	}

	@Test
	public void parseStream() throws IOException {
		NetworkDefinitionType d = null;
		try (InputStream in = getClass().getResourceAsStream("kcd-test-01.xml")) {
			d = new JaxbSnKcdParser().parseKcd(in, false);
		}
		assertTest01Document(d);
	}

	private void assertTest01Document(NetworkDefinitionType d) {
		assertThat("Document parsed", d, notNullValue());
		// TODO
	}

	@Test
	public void unmarshallSolarNodeNetworkDefinition() throws Exception {
		JAXBContext context = JAXBContext.newInstance(new Class[] { NetworkDefinitionType.class });
		Unmarshaller umarshall = context.createUnmarshaller();
		JAXBElement<NetworkDefinitionType> object = umarshall.unmarshal(
				new StreamSource(getClass().getResourceAsStream("kcd-test-03.xml")),
				NetworkDefinitionType.class);
		assertThat("NetworkDefinition unmarshalled", object, notNullValue());
	}

}
