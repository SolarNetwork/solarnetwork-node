/* ==================================================================
 * SettingSpecifierProviderFactoryMessageComparatorTests.java - 23/11/2018 11:30:06 AM
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

package net.solarnetwork.node.settings.support.test;

import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.lessThanOrEqualTo;
import java.util.Locale;
import org.easymock.EasyMock;
import org.junit.Test;
import org.springframework.context.MessageSource;
import net.solarnetwork.node.settings.support.SettingSpecifierProviderFactoryMessageComparator;
import net.solarnetwork.settings.SettingSpecifierProviderFactory;

/**
 * Test cases for the {@link SettingSpecifierProviderFactoryMessageComparator}
 * class.
 * 
 * @author matt
 * @version 1.0
 */
public class SettingSpecifierProviderFactoryMessageComparatorTests {

	private SettingSpecifierProviderFactory setupMockFactory(String displayName, Locale locale,
			String messageKey, String messageValue) {
		SettingSpecifierProviderFactory f1 = EasyMock.createMock(SettingSpecifierProviderFactory.class);
		expect(f1.getDisplayName()).andReturn(displayName).anyTimes();

		MessageSource ms = null;
		if ( messageKey != null ) {
			ms = EasyMock.createMock(MessageSource.class);
			expect(ms.getMessage(messageKey, null, displayName, locale)).andReturn(messageValue);
			replay(ms);
		}

		expect(f1.getMessageSource()).andReturn(ms).anyTimes();

		replay(f1);

		return f1;
	}

	@Test
	public void sort() {
		Locale locale = Locale.US;

		SettingSpecifierProviderFactory f1 = setupMockFactory("A", locale, "title", "Flarg barg");
		SettingSpecifierProviderFactory f2 = setupMockFactory("B", locale, "title", "Bling bang");

		assertThat("Message value order",
				new SettingSpecifierProviderFactoryMessageComparator(locale).compare(f1, f2),
				greaterThanOrEqualTo(1));
	}

	@Test
	public void sortReversed() {
		Locale locale = Locale.US;

		SettingSpecifierProviderFactory f1 = setupMockFactory("A", locale, "title", "Flarg barg");
		SettingSpecifierProviderFactory f2 = setupMockFactory("B", locale, "title", "Bling bang");

		assertThat("Message value reverse order",
				new SettingSpecifierProviderFactoryMessageComparator(locale).compare(f2, f1),
				lessThanOrEqualTo(-1));
	}

	@Test
	public void sortEqually() {
		Locale locale = Locale.US;

		SettingSpecifierProviderFactory f1 = setupMockFactory("A", locale, "title", "Flarg barg");
		SettingSpecifierProviderFactory f2 = setupMockFactory("B", locale, "title", "flarg Barg");

		assertThat("Message value order",
				new SettingSpecifierProviderFactoryMessageComparator(locale).compare(f1, f2),
				equalTo(0));
	}

	@Test
	public void sortWithoutMessageSource() {
		Locale locale = Locale.US;

		SettingSpecifierProviderFactory f1 = setupMockFactory("A", locale, null, null);
		SettingSpecifierProviderFactory f2 = setupMockFactory("B", locale, null, null);

		assertThat("Message value order",
				new SettingSpecifierProviderFactoryMessageComparator(locale).compare(f1, f2),
				lessThanOrEqualTo(-1));
	}

}
