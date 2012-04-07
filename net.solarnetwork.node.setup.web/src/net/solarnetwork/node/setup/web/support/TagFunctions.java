/* ==================================================================
 * TagFunctions.java - Mar 12, 2012 8:36:40 PM
 * 
 * Copyright 2007-2012 SolarNetwork.net Dev Team
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
 * $Id$
 * ==================================================================
 */

package net.solarnetwork.node.setup.web.support;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Static JSP function methods.
 * 
 * @author matt
 * @version $Revision$
 */
public final class TagFunctions {

	private TagFunctions() {
		// can't create me
	}

	public static boolean instanceOf(Object o, String className) {
		if ( o == null || className == null ) {
			return false;
		}
		Class<?> clazz;
		try {
			clazz = Thread.currentThread().getContextClassLoader().loadClass(className);
			return clazz.isInstance(o);
		} catch (ClassNotFoundException e) {
			return false;
		}
	}

	private static final Pattern JS_ESCAPE = Pattern.compile("(')");

	public static String jsString(String input) {
		if ( input == null || input.length() < 1 ) {
			return "";
		}
		Matcher m = JS_ESCAPE.matcher(input);
		return m.replaceAll("\\\\$1");
	}

}
