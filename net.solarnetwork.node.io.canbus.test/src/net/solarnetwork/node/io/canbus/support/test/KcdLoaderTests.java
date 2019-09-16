/* ==================================================================
 * KcdLoaderTests.java - 14/09/2019 7:47:38 am
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

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.GZIPOutputStream;
import org.junit.Test;
import org.springframework.util.FileCopyUtils;
import com.github.kayak.core.description.Document;
import net.solarnetwork.node.io.canbus.support.KcdLoader;

/**
 * Test cases for the {@link KcdLoader} class.
 * 
 * @author matt
 * @version 1.0
 */
public class KcdLoaderTests {

	@Test
	public void parseGzipStream() throws IOException {
		Document d = null;
		// compress test file as GZIP, then read GZIP stream
		try (InputStream rawIn = getClass().getResourceAsStream("kcd-test-01.xml");
				ByteArrayOutputStream gzipData = new ByteArrayOutputStream();
				GZIPOutputStream gzipOut = new GZIPOutputStream(gzipData)) {
			FileCopyUtils.copy(rawIn, gzipOut);
			try (InputStream compressedIn = new ByteArrayInputStream(gzipData.toByteArray())) {
				d = new KcdLoader().parse(compressedIn, "test.kcd.gz");
			}
		}
		assertTest01Document(d, "test.kcd.gz");
	}

	@Test
	public void parseStream() throws IOException {
		Document d = null;
		try (InputStream in = getClass().getResourceAsStream("kcd-test-01.xml")) {
			d = new KcdLoader().parse(in, "test.kcd");
		}
		assertTest01Document(d, "test.kcd");
	}

	private void assertTest01Document(Document d, String fileName) {
		assertThat("Document parsed", d, notNullValue());
		assertThat("File name", d.getFileName(), equalTo(fileName));
	}

}
