/* ==================================================================
 * SetupResourceScope.java - 10/04/2017 2:49:04 PM
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

package net.solarnetwork.node.setup;

/**
 * A scope for setup resources.
 * 
 * Some resources should only be applied once across the entire application,
 * while others might need different scope.
 * 
 * @author matt
 * @version 1.0
 * @since 1.49
 */
public enum SetupResourceScope {

	/** A default scope. */
	Default,

	/**
	 * Application wide scope, meaning the resource should be applied once at
	 * the entire application's level.
	 */
	Application;

}
