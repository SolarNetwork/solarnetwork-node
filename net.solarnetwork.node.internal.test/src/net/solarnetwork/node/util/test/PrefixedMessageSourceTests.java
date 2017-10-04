/* ==================================================================
 * PrefixedMessageSourceTests.java - 4/10/2017 2:45:32 PM
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

package net.solarnetwork.node.util.test;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.context.MessageSource;
import org.springframework.context.NoSuchMessageException;
import org.springframework.context.support.ResourceBundleMessageSource;
import net.solarnetwork.node.util.PrefixedMessageSource;

/**
 * Test cases for the {@link PrefixedMessageSource} class.
 * 
 * @author matt
 * @version 1.0
 */
public class PrefixedMessageSourceTests {

	private static MessageSource messageSourceA;
	private static MessageSource messageSourceB;
	private static MessageSource messageSourceP;

	private PrefixedMessageSource prefixedMessageSource;

	@BeforeClass
	public static void setupClass() {
		ResourceBundleMessageSource a = new ResourceBundleMessageSource();
		a.setBasename("net.solarnetwork.node.util.test.message-source-A");
		messageSourceA = a;

		ResourceBundleMessageSource b = new ResourceBundleMessageSource();
		b.setBasename("net.solarnetwork.node.util.test.message-source-B");
		messageSourceB = b;

		ResourceBundleMessageSource p = new ResourceBundleMessageSource();
		p.setBasename("net.solarnetwork.node.util.test.message-source-P");
		messageSourceP = p;
	}

	@Before
	public void setup() {
		Map<String, MessageSource> delegates = new HashMap<String, MessageSource>();
		delegates.put("a.", messageSourceA);
		delegates.put("b.", messageSourceB);
		prefixedMessageSource = new PrefixedMessageSource();
		prefixedMessageSource.setDelegates(delegates);
	}

	@Test
	public void singluarPrefixResolvesPrefixSetFirst() {
		PrefixedMessageSource messageSource = new PrefixedMessageSource();
		messageSource.setPrefix("a.");
		messageSource.setDelegate(messageSourceA);
		assertThat(prefixedMessageSource.getMessage("a.title", null, Locale.getDefault()),
				equalTo("Child A"));
	}

	@Test
	public void singluarPrefixResolvesDelegateSetFirst() {
		PrefixedMessageSource messageSource = new PrefixedMessageSource();
		messageSource.setDelegate(messageSourceA);
		messageSource.setPrefix("a.");
		assertThat(prefixedMessageSource.getMessage("a.title", null, Locale.getDefault()),
				equalTo("Child A"));
	}

	@Test
	public void singluarPrefixResolvesWithParent() {
		PrefixedMessageSource messageSource = new PrefixedMessageSource();
		messageSource.setPrefix("a.");
		messageSource.setDelegate(messageSourceA);
		messageSource.setParentMessageSource(messageSourceP);
		assertThat(messageSource.getMessage("title", null, Locale.getDefault()), equalTo("Parent"));
	}

	@Test
	public void multiPrefixResolves() {
		assertThat(prefixedMessageSource.getMessage("a.title", null, Locale.getDefault()),
				equalTo("Child A"));
		assertThat(prefixedMessageSource.getMessage("b.title", null, Locale.getDefault()),
				equalTo("Child B"));
	}

	@Test
	public void multiPrefixWithParentResolves() {
		prefixedMessageSource.setParentMessageSource(messageSourceP);
		assertThat(prefixedMessageSource.getMessage("a.title", null, Locale.getDefault()),
				equalTo("Child A"));
		assertThat(prefixedMessageSource.getMessage("b.title", null, Locale.getDefault()),
				equalTo("Child B"));
		assertThat(prefixedMessageSource.getMessage("title", null, Locale.getDefault()),
				equalTo("Parent"));
	}

	@Test(expected = NoSuchMessageException.class)
	public void multiPrefixWithParentNotFoundInChild() {
		prefixedMessageSource.setParentMessageSource(messageSourceP);
		prefixedMessageSource.getMessage("b.subj", null, Locale.getDefault());
	}

	@Test
	public void multiPrefixWithParentResolvesOnlyInParent() {
		prefixedMessageSource.setParentMessageSource(messageSourceP);
		assertThat(prefixedMessageSource.getMessage("info", null, Locale.getDefault()),
				equalTo("Nothing"));
	}

}
