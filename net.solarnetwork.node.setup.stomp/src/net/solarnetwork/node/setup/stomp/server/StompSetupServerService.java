/* ==================================================================
 * StompSetupServerService.java - 18/08/2021 11:18:42 AM
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

package net.solarnetwork.node.setup.stomp.server;

import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.util.PathMatcher;
import net.solarnetwork.node.reactor.InstructionExecutionService;
import net.solarnetwork.node.setup.UserService;

/**
 * Main service implementation for STOMP setup.
 * 
 * @author matt
 * @version 2.0
 */
public class StompSetupServerService {

	private final UserService userService;
	private final UserDetailsService userDetailsService;
	private final PathMatcher pathMatcher;
	private final InstructionExecutionService instructionService;

	/**
	 * Constructor.
	 * 
	 * @param userService
	 *        the user service
	 * @param userDetailsService
	 *        the user details service
	 * @param pathMatcher
	 *        the path matcher
	 * @param instructionService
	 *        the instruction service
	 * @throws IllegalArgumentException
	 *         if any argument is {@literal null}
	 */
	public StompSetupServerService(UserService userService, UserDetailsService userDetailsService,
			PathMatcher pathMatcher, InstructionExecutionService instructionService) {
		super();
		if ( userService == null ) {
			throw new IllegalArgumentException("The userService argument must not be null.");
		}
		this.userService = userService;
		if ( userDetailsService == null ) {
			throw new IllegalArgumentException("The userDetailsService argument must not be null.");
		}
		this.userDetailsService = userDetailsService;
		if ( pathMatcher == null ) {
			throw new IllegalArgumentException("The pathMatcher argument must not be null.");
		}
		this.pathMatcher = pathMatcher;
		if ( instructionService == null ) {
			throw new IllegalArgumentException("The instructionService argument must not be null.");
		}
		this.instructionService = instructionService;
	}

	/**
	 * Get the user service.
	 * 
	 * @return the user service, never {@literal null}
	 */
	public UserService getUserService() {
		return userService;
	}

	/**
	 * Get the user details service.
	 * 
	 * @return the user details service, never {@literal null}
	 */
	public UserDetailsService getUserDetailsService() {
		return userDetailsService;
	}

	/**
	 * Get the path matcher.
	 * 
	 * @return the path matcher, never {@literal null}
	 */
	public PathMatcher getPathMatcher() {
		return pathMatcher;
	}

	/**
	 * Get the instruction handlers.
	 * 
	 * @return the instruction handlers, never {@literal null}
	 */
	public InstructionExecutionService getInstructionService() {
		return instructionService;
	}

}
