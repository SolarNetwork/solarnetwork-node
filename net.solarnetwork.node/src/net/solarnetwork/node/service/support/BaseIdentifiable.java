/* ==================================================================
 * BasicIdentifiable.java - 15/05/2019 3:42:21 pm
 *
 * Copyright 2019 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.service.support;

import static net.solarnetwork.service.OptionalServiceCollection.services;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.expression.ExpressionException;
import net.solarnetwork.domain.datum.MutableDatumSamplesOperations;
import net.solarnetwork.node.service.LocationService;
import net.solarnetwork.node.service.MetadataService;
import net.solarnetwork.node.service.PlaceholderService;
import net.solarnetwork.service.ExpressionService;
import net.solarnetwork.service.Identifiable;
import net.solarnetwork.service.OptionalService;
import net.solarnetwork.service.OptionalServiceCollection;
import net.solarnetwork.service.support.BasicIdentifiable;
import net.solarnetwork.service.support.ExpressionServiceExpression;
import net.solarnetwork.settings.SettingSpecifier;
import net.solarnetwork.settings.support.BasicTextFieldSettingSpecifier;

/**
 * Extension of {@link BasicIdentifiable} with node-specific helpers.
 *
 * <p>
 * This class is meant to be extended by more useful services.
 * </p>
 *
 * @author matt
 * @version 2.2
 * @since 1.67
 */
public abstract class BaseIdentifiable extends BasicIdentifiable implements Identifiable {

	/**
	 * A class-level logger.
	 *
	 * @since 1.4
	 */
	protected final Logger log = LoggerFactory.getLogger(getClass());

	private OptionalService<PlaceholderService> placeholderService;
	private OptionalService<MetadataService> metadataService;
	private OptionalService<LocationService> locationService;
	private OptionalServiceCollection<ExpressionService> expressionServices;

	/**
	 * Default constructor.
	 */
	public BaseIdentifiable() {
		super();
	}

	/**
	 * Get settings for the configurable properties of
	 * {@link BasicIdentifiable}.
	 *
	 * <p>
	 * Empty strings are used for the default {@code uid} and {@code groupUid}
	 * setting values.
	 * </p>
	 *
	 * @param prefix
	 *        an optional prefix to include in all setting keys
	 * @return the settings
	 * @see #baseIdentifiableSettings(String, String, String)
	 */
	public static List<SettingSpecifier> baseIdentifiableSettings(String prefix) {
		return baseIdentifiableSettings(prefix, "", "");
	}

	/**
	 * Get settings for the configurable properties of
	 * {@link BasicIdentifiable}.
	 *
	 * @param prefix
	 *        an optional prefix to include in all setting keys
	 * @param defaultUid
	 *        the default {@code uid} value to use
	 * @param defaultGroupUid
	 *        the default {@code groupUid} value to use
	 * @return the settings
	 * @since 1.1
	 */
	public static List<SettingSpecifier> baseIdentifiableSettings(String prefix, String defaultUid,
			String defaultGroupUid) {
		if ( prefix == null ) {
			prefix = "";
		}
		List<SettingSpecifier> results = new ArrayList<>(8);
		results.add(new BasicTextFieldSettingSpecifier(prefix + "uid", defaultUid));
		results.add(new BasicTextFieldSettingSpecifier(prefix + "groupUid", defaultGroupUid));
		return results;
	}

	/**
	 * Resolve placeholders using the configured {@link PlaceholderService}.
	 *
	 * @param s
	 *        the string to resolve placeholder values on
	 * @return the resolved string, or {@literal null} if {@code s} is
	 *         {@literal null}
	 * @since 1.3
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
	 * @since 1.3
	 */
	protected String resolvePlaceholders(String s, Map<String, ?> parameters) {
		return PlaceholderService.resolvePlaceholders(placeholderService, s, parameters);
	}

	/**
	 * Evaluate a set of expression configurations and store the results as
	 * properties on a datum.
	 *
	 * @param d
	 *        the datum to store the results of expression evaluations on
	 * @param expressionConfs
	 *        the expression configurations
	 * @param root
	 *        the expression root object
	 * @since 1.4
	 */
	protected void populateExpressionDatumProperties(final MutableDatumSamplesOperations d,
			final ExpressionConfig[] expressionConfs, final Object root) {
		Iterable<ExpressionService> services = services(expressionServices);
		if ( services == null || expressionConfs == null || expressionConfs.length < 1
				|| root == null ) {
			return;
		}
		for ( ExpressionConfig config : expressionConfs ) {
			if ( config.getName() == null || config.getName().isEmpty() || config.getExpression() == null
					|| config.getExpression().isEmpty() ) {
				continue;
			}
			final ExpressionServiceExpression expr;
			try {
				expr = config.getExpression(services);
			} catch ( ExpressionException e ) {
				log.warn("Error parsing property [{}] expression `{}`: {}", config.getName(),
						config.getExpression(), e.getMessage());
				return;
			}

			Object propValue = null;
			if ( expr != null ) {
				try {
					propValue = expr.getService().evaluateExpression(expr.getExpression(), null, root,
							null, Object.class);
					if ( log.isTraceEnabled() ) {
						log.trace(
								"Service [{}] evaluated datum property [{}] expression `{}` \u2192 {}\n\nExpression root: {}",
								getUid(), config.getName(), config.getExpression(), propValue, root);
					} else if ( log.isDebugEnabled() ) {
						log.debug("Service [{}] evaluated datum property [{}] expression `{}` \u2192 {}",
								getUid(), config.getName(), config.getExpression(), propValue);
					}
				} catch ( ExpressionException e ) {
					log.warn(
							"Error evaluating service [{}] datum property [{}] expression `{}`: {}\n\nExpression root: {}",
							getUid(), config.getName(), config.getExpression(), e.getMessage(), root);
				}
			}
			if ( propValue != null ) {
				d.putSampleValue(config.getDatumPropertyType(), config.getName(), propValue);
			}
		}
	}

	/**
	 * Get the placeholder service to use.
	 *
	 * @return the service
	 * @since 1.3
	 */
	public OptionalService<PlaceholderService> getPlaceholderService() {
		return placeholderService;
	}

	/**
	 * Set the placeholder service to use.
	 *
	 * @param placeholderService
	 *        the service to set
	 * @since 1.3
	 */
	public void setPlaceholderService(OptionalService<PlaceholderService> placeholderService) {
		this.placeholderService = placeholderService;
	}

	/**
	 * Get an optional collection of {@link ExpressionService}.
	 *
	 * @return the optional {@link ExpressionService} collection to use
	 * @since 1.4
	 */
	public OptionalServiceCollection<ExpressionService> getExpressionServices() {
		return expressionServices;
	}

	/**
	 * Configure an optional collection of {@link ExpressionService}.
	 *
	 * @param expressionServices
	 *        the optional {@link ExpressionService} collection to use
	 * @since 1.4
	 */
	public void setExpressionServices(OptionalServiceCollection<ExpressionService> expressionServices) {
		this.expressionServices = expressionServices;
	}

	/**
	 * Get an optional {@link MetadataService}.
	 *
	 * @return the service
	 * @since 2.1
	 */
	public OptionalService<MetadataService> getMetadataService() {
		return metadataService;
	}

	/**
	 * Configure an optional {@link MetadataService}.
	 *
	 * @param metadataService
	 *        the service to set
	 * @since 2.1
	 */
	public void setMetadataService(OptionalService<MetadataService> metadataService) {
		this.metadataService = metadataService;
	}

	/**
	 * Get an optional {@link LocationService}.
	 *
	 * @return the service
	 * @since 2.2
	 */
	public final OptionalService<LocationService> getLocationService() {
		return locationService;
	}

	/**
	 * Configure an optional {@link LocationService}.
	 *
	 * @param locationService
	 *        the service to set
	 * @since 2.2
	 */
	public final void setLocationService(OptionalService<LocationService> locationService) {
		this.locationService = locationService;
	}

}
