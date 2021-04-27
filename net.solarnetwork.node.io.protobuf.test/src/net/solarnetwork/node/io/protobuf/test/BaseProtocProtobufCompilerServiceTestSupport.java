/* ==================================================================
 * BaseProtocProtobufCompilerServiceTestSupport.java - 28/04/2021 9:36:57 AM
 * 
 * Copyright 2021 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.io.protobuf.test;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import net.solarnetwork.common.jdt.JdtJavaCompiler;
import net.solarnetwork.common.protobuf.protoc.ProtocProtobufCompilerService;
import net.solarnetwork.test.SystemPropertyMatchTestRule;
import net.solarnetwork.util.JavaCompiler;

/**
 * Testing support for {@literal protoc} based tests.
 * 
 * @author matt
 * @version 1.0
 */
public class BaseProtocProtobufCompilerServiceTestSupport {

	/** Only run when the {@code protoc-int} system property is defined. */
	@ClassRule
	public static SystemPropertyMatchTestRule PROFILE_RULE = new SystemPropertyMatchTestRule(
			"protoc-int");

	private static Properties TEST_PROPS;

	protected JavaCompiler compiler;
	protected ProtocProtobufCompilerService protocService;

	@BeforeClass
	public static void setupClass() {
		Properties p = new Properties();
		try {
			InputStream in = BaseProtocProtobufCompilerServiceTestSupport.class.getClassLoader()
					.getResourceAsStream("protobuf.properties");
			if ( in != null ) {
				p.load(in);
				in.close();
			}
		} catch ( IOException e ) {
			throw new RuntimeException(e);
		}
		TEST_PROPS = p;
	}

	@Before
	public void setup() throws IOException {
		compiler = new JdtJavaCompiler();
		protocService = new ProtocProtobufCompilerService(compiler);
		if ( TEST_PROPS.containsKey("protoc.path") ) {
			protocService.setProtocPath(TEST_PROPS.getProperty("protoc.path"));
		}
	}

}
