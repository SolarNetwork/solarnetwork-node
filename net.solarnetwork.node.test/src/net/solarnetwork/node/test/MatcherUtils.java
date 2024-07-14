/* ==================================================================
 * MatcherUtils.java - 15/07/2024 7:12:26 am
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

package net.solarnetwork.node.test;

import java.util.regex.Pattern;
import org.hamcrest.Matcher;
import org.hamcrest.Matchers;
import net.solarnetwork.util.ClassUtils;

/**
 * Test matcher utilities.
 *
 * @author matt
 * @version 1.0
 * @since 1.17
 */
public final class MatcherUtils {

	private MatcherUtils() {
		// not available
	}

	/**
	 * Create a {@link Matcher} for a string that compares to the contents of a
	 * text resource, ignoring whitespace.
	 *
	 * @param resource
	 *        the name of the resource
	 * @param clazz
	 *        the class to load the resource from
	 * @param skip
	 *        an optional pattern that will be used to match against lines;
	 *        matches will be left out of the string used to match
	 * @return the matcher
	 * @throws RuntimeException
	 *         if the resource cannot be loaded
	 */
	public static Matcher<String> equalToTextResource(String resource, Class<?> clazz, Pattern skip) {
		String txt = ClassUtils.getResourceAsString(resource, clazz, skip);
		return Matchers.equalToIgnoringWhiteSpace(txt);
	}

}
