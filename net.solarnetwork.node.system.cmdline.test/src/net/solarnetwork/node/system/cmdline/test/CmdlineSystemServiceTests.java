/* ==================================================================
 * CmdlineSystemServiceTests.java - 23/06/2020 2:43:24 PM
 * 
 * Copyright 2020 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.system.cmdline.test;

import static org.hamcrest.Matchers.hasItems;
import static org.junit.Assert.assertThat;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.FileCopyUtils;
import net.solarnetwork.node.system.cmdline.CmdlineSystemService;

/**
 * Test cases for the {@link CmdlineSystemService} class.
 * 
 * @author matt
 * @version 1.0
 */
public class CmdlineSystemServiceTests {

	private final Logger log = LoggerFactory.getLogger(getClass());

	private TestCmdlineSystemService service;

	private static final class TestCmdlineSystemService extends CmdlineSystemService {

		private final List<String> stdout;
		private final List<String> stderr;

		private TestCmdlineSystemService() {
			super();
			this.stdout = new ArrayList<>(8);
			this.stderr = new ArrayList<>(8);
		}

		@Override
		protected void handleInputStreamLine(boolean errorStream, String line) {
			if ( errorStream ) {
				stderr.add(line);
			} else {
				stdout.add(line);
			}
		}

	}

	private File resetScript;
	private File resetAppScript;

	@Before
	public void setup() throws IOException {
		service = new TestCmdlineSystemService();
		// could make script name configurable... i.e. support other OSes?
		// copy script to file so test can run via JAR (i.e. Ant)
		resetScript = File.createTempFile("reset-", ".sh");
		resetScript.setExecutable(true, true);
		FileCopyUtils.copy(getClass().getResourceAsStream("reset.sh"),
				new FileOutputStream(resetScript));
		service.setResetCommand(resetScript.getAbsolutePath());

		resetAppScript = File.createTempFile("reset-app-", ".sh");
		resetAppScript.setExecutable(true, true);
		FileCopyUtils.copy(getClass().getResourceAsStream("reset-app.sh"),
				new FileOutputStream(resetAppScript));
		service.setResetAppCommand(resetAppScript.getAbsolutePath());
	}

	@After
	public void teardown() {
		if ( resetScript != null ) {
			resetScript.delete();
		}
		if ( resetAppScript != null ) {
			resetAppScript.delete();
		}
	}

	@Test
	public void reset() throws Exception {
		// GIVEN

		// WHEN
		service.reset(false);
		Thread.sleep(1500);

		// THEN
		assertThat("Command exectued as expected", service.stdout, hasItems("Reset"));
	}

	@Test
	public void reset_applicationOnly() throws Exception {
		// GIVEN

		// WHEN
		service.reset(true);
		Thread.sleep(1500);

		// THEN
		log.debug("STDOUT: " + service.stdout);
		log.debug("STDERR: " + service.stderr);
		assertThat("Command exectued as expected", service.stdout, hasItems("ResetApp"));
	}
}
