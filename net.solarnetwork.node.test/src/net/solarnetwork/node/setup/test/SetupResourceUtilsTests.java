/* ==================================================================
 * SetupResourceUtilsTests.java - 24/09/2016 6:23:39 AM
 * 
 * Copyright 2007-2016 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.setup.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;
import org.junit.Test;
import net.solarnetwork.node.setup.BaseStaticSetupResource;
import net.solarnetwork.node.setup.SetupResourceUtils;

/**
 * Test cases for the {@link SetupResourceUtils} class.
 * 
 * @author matt
 * @version 1.0
 */
public class SetupResourceUtilsTests {

	@Test
	public void scoreForNull() {
		assertEquals("Null resource has minimum score", Integer.MIN_VALUE,
				SetupResourceUtils.localeScore(null, Locale.UK, Locale.US));
	}

	@Test
	public void scoreForResourceLocale() {
		assertEquals("Exact match on locale", Integer.MAX_VALUE, SetupResourceUtils
				.localeScore(new TestSetupResource(new Locale("en", "GB")), Locale.UK, Locale.US));
	}

	@Test
	public void scoreForNullResourceDefaultLocale() {
		assertEquals("Desired locale language matches default locale language", 1,
				SetupResourceUtils.localeScore(new TestSetupResource(null), Locale.UK, Locale.US));
	}

	@Test
	public void scoreForResourceDefaultLocaleLanguageMatch() {
		assertEquals("Desired locale language matches resource locale language", 1, SetupResourceUtils
				.localeScore(new TestSetupResource(new Locale("en", "CA")), Locale.UK, Locale.US));
	}

	@Test
	public void scoreForNonMatching() {
		assertEquals("Desired locale language different", -1, SetupResourceUtils
				.localeScore(new TestSetupResource(new Locale("ja", "JP")), Locale.UK, Locale.US));
	}

	@Test
	public void baseFilenameForPathWithLanguage() {
		assertEquals("bar.txt", SetupResourceUtils.baseFilenameForPath("for/bar_en.txt"));
	}

	@Test
	public void baseFilenameForPathWithLanguageAndCountry() {
		assertEquals("bar.txt", SetupResourceUtils.baseFilenameForPath("for/bar_en_US.txt"));
	}

	@Test
	public void baseFilenameForNullPath() {
		assertNull(SetupResourceUtils.baseFilenameForPath(null));
	}

	@Test
	public void baseFilenameForPathWithNoExtension() {
		assertEquals("bar", SetupResourceUtils.baseFilenameForPath("for/bar"));
	}

	@Test
	public void baseFilenameForPathWithTrailingDot() {
		assertEquals("bar.", SetupResourceUtils.baseFilenameForPath("for/bar."));
	}

	@Test
	public void baseFilenameForPathWithExtension() {
		assertEquals("bar.bam", SetupResourceUtils.baseFilenameForPath("for/bar.bam"));
	}

	@Test
	public void baseFilenameForPathWithJustUnderscore() {
		assertEquals("bar_.bam", SetupResourceUtils.baseFilenameForPath("for/bar_.bam"));
	}

	@Test
	public void baseFilenameForPathWithUnderscores() {
		assertEquals("file_with_stuff.bam",
				SetupResourceUtils.baseFilenameForPath("for/file_with_stuff.bam"));
	}

	private static class TestSetupResource extends BaseStaticSetupResource {

		private TestSetupResource(Locale locale) {
			super("test", "test", locale, -1, 0, 0, null, null);
		}

		@Override
		public InputStream getInputStream() throws IOException {
			return null;
		}

	}

}
