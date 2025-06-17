/* ==================================================================
 * SolarNodeUtils.java - 18/06/2025 6:17:26 am
 *
 * Copyright 2025 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.setup.web.thymeleaf;

/**
 * Helper for SolarNode object utilities.
 *
 * @author matt
 * @version 1.0
 */
public final class SolarNodeUtils {

	/** The instance. */
	public static final SolarNodeUtils INSTANCE = new SolarNodeUtils();

	private SolarNodeUtils() {
		super();
	}

	/**
	 * Return {@literal true} if {@code o} is an instance of the class
	 * {@code className}.
	 *
	 * @param o
	 *        the object to test
	 * @param className
	 *        the class name to test
	 * @return boolean
	 */
	public boolean instanceOf(Object o, String className) {
		if ( o == null || className == null ) {
			return false;
		}
		Class<?> clazz;
		try {
			clazz = Thread.currentThread().getContextClassLoader().loadClass(className);
			return clazz.isInstance(o);
		} catch ( ClassNotFoundException e ) {
			return false;
		}
	}

}
