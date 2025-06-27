/* ==================================================================
 * BaseHttpRequestCustomizerService.java - 3/04/2023 2:26:13 pm
 *
 * Copyright 2023 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.io.http.req;

import java.util.Map;
import net.solarnetwork.node.service.PlaceholderService;
import net.solarnetwork.service.ExpressionService;
import net.solarnetwork.service.OptionalService;
import net.solarnetwork.service.OptionalServiceCollection;
import net.solarnetwork.util.StringUtils;
import net.solarnetwork.web.jakarta.service.support.AbstractHttpRequestCustomizerService;

/**
 * Extension of common {@link AbstractHttpRequestCustomizerService} to include
 * some generally useful node service support.
 *
 * @author matt
 * @version 2.0
 */
public abstract class BaseHttpRequestCustomizerService extends AbstractHttpRequestCustomizerService {

	private OptionalService<PlaceholderService> placeholderService;
	private OptionalServiceCollection<ExpressionService> expressionServices;

	/**
	 * Constructor.
	 */
	public BaseHttpRequestCustomizerService() {
		super();
	}

	/**
	 * Test if a string has any placeholder within in.
	 *
	 * @param s
	 *        the string to test
	 * @return {@literal true} if {@code s} has a placeholder template in it
	 * @see StringUtils#NAMES_PATTERN
	 */
	public static boolean hasPlaceholder(String s) {
		return s != null && StringUtils.NAMES_PATTERN.matcher(s).find();
	}

	/**
	 * Resolve placeholders using the configured {@link PlaceholderService}.
	 *
	 * @param s
	 *        the string to resolve placeholder values on
	 * @return the resolved string, or {@literal null} if {@code s} is
	 *         {@literal null}
	 */
	protected String resolvePlaceholders(String s) {
		return resolvePlaceholders(s, null);
	}

	/**
	 * Resolve placeholders using the configured {@link PlaceholderService}.
	 *
	 * @param s
	 *        the string to resolve placeholder values on
	 * @param parameters
	 *        optional parameters to use, or {@literal null}
	 * @return the resolved string, or {@literal null} if {@code s} is
	 *         {@literal null}
	 */
	protected String resolvePlaceholders(String s, Map<String, ?> parameters) {
		return PlaceholderService.resolvePlaceholders(placeholderService, s, parameters);
	}

	/**
	 * Get the placeholder service to use.
	 *
	 * @return the service
	 */
	public OptionalService<PlaceholderService> getPlaceholderService() {
		return placeholderService;
	}

	/**
	 * Set the placeholder service to use.
	 *
	 * @param placeholderService
	 *        the service to set
	 */
	public void setPlaceholderService(OptionalService<PlaceholderService> placeholderService) {
		this.placeholderService = placeholderService;
	}

	/**
	 * Get an optional collection of {@link ExpressionService}.
	 *
	 * @return the optional {@link ExpressionService} collection to use
	 */
	public OptionalServiceCollection<ExpressionService> getExpressionServices() {
		return expressionServices;
	}

	/**
	 * Configure an optional collection of {@link ExpressionService}.
	 *
	 * @param expressionServices
	 *        the optional {@link ExpressionService} collection to use
	 */
	public void setExpressionServices(OptionalServiceCollection<ExpressionService> expressionServices) {
		this.expressionServices = expressionServices;
	}

}
