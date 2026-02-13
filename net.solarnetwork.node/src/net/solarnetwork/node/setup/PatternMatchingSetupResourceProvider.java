/* ==================================================================
 * PatternMatchingSetupResourceProvider.java - 23/09/2016 9:27:09 AM
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

package net.solarnetwork.node.setup;

import static net.solarnetwork.node.setup.SetupResourceUtils.baseFilenameForPath;
import static net.solarnetwork.node.setup.SetupResourceUtils.localeScore;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.util.StringUtils;

/**
 * Resolve resources based on pattern matching a set of base names.
 *
 * This can be easier to configure a set of localized resources than using
 * {@link SimpleSetupResourceProvider}.
 *
 * @author matt
 * @version 1.1
 */
public class PatternMatchingSetupResourceProvider
		implements SetupResourceProvider, ApplicationContextAware {

	/**
	 * The content type assigned to resolved resources that are of an unknown
	 * type.
	 */
	public static final String UNKNOWN_CONTENT_TYPE = "application/octet-stream";

	private Locale defaultLocale = Locale.US;
	private Set<String> consumerTypes = SetupResource.WEB_CONSUMER_TYPES;
	private Set<String> roles = SetupResource.USER_ROLES;
	private SetupResourceScope scope = SetupResourceScope.Default;
	private String[] basenames;
	private ResourcePatternResolver resourcePatternResolver;
	private int cacheSeconds = 86400;
	private Map<String, String> fileExtensionContentTypeMapping = net.solarnetwork.node.setup.SetupResourceUtils.DEFAULT_FILENAME_EXTENSION_CONTENT_TYPES;

	private static final Logger LOG = LoggerFactory
			.getLogger(PatternMatchingSetupResourceProvider.class);

	/**
	 * Default constructor.
	 */
	public PatternMatchingSetupResourceProvider() {
		super();
	}

	@Override
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		if ( resourcePatternResolver == null ) {
			resourcePatternResolver = applicationContext;
		}
	}

	@Override
	public SetupResource getSetupResource(String resourceUID, Locale locale) {
		int bestScore = -1;
		SetupResource bestMatch = null;
		for ( String basename : basenames ) {
			List<SetupResource> resources = resolveSetupResourcesForBasename(basename);
			for ( SetupResource rsrc : resources ) {
				if ( resourceUID.equals(rsrc.getResourceUID()) ) {
					int score = localeScore(rsrc, locale, defaultLocale);
					if ( score == Integer.MAX_VALUE ) {
						return rsrc;
					}
					if ( bestMatch == null || score > bestScore ) {
						bestScore = score;
						bestMatch = rsrc;
					}
				}
			}
		}
		return bestMatch;
	}

	@Override
	public Collection<SetupResource> getSetupResourcesForConsumer(String consumerType, Locale locale) {
		if ( !consumerTypes.contains(consumerType) ) {
			return Collections.emptyList();
		}
		List<SetupResource> results = new ArrayList<SetupResource>(basenames.length);
		for ( String basename : basenames ) {
			Map<String, SetupResource> bestMatches = new HashMap<String, SetupResource>();
			List<SetupResource> resources = resolveSetupResourcesForBasename(basename);
			for ( SetupResource rsrc : resources ) {
				Set<String> supported = rsrc.getSupportedConsumerTypes();
				if ( supported == null || supported.contains(consumerType) ) {
					SetupResource currMatch = bestMatches.get(rsrc.getResourceUID());
					if ( localeScore(currMatch, locale, defaultLocale) < localeScore(rsrc, locale,
							defaultLocale) ) {
						bestMatches.put(rsrc.getResourceUID(), rsrc);
					}
				}
			}
			results.addAll(bestMatches.values());
		}
		return results;
	}

	private List<SetupResource> resolveSetupResourcesForBasename(String basename) {
		final String pattern = basename + "*.*";
		List<SetupResource> result = null;
		try {
			Resource[] matches = resourcePatternResolver.getResources(pattern);
			if ( matches != null ) {
				for ( Resource r : matches ) {
					if ( result == null ) {
						result = new ArrayList<SetupResource>(8);
					}
					String filename = r.getFilename();
					String contentType = fileExtensionContentTypeMapping
							.get(StringUtils.getFilenameExtension(filename));
					if ( contentType == null ) {
						contentType = UNKNOWN_CONTENT_TYPE;
					}
					result.add(new ResourceSetupResource(r, baseFilenameForPath(filename), contentType,
							cacheSeconds, consumerTypes, roles, scope));
				}
			}
		} catch ( IOException e ) {
			LOG.error("Error resolving basename [{}]: {}", e);
		}
		if ( result == null ) {
			result = Collections.emptyList();
		}
		return result;
	}

	/**
	 * Set the consumer types assigned to all resolved resources. Defaults to
	 * {@link SetupResource#WEB_CONSUMER_TYPES}.
	 *
	 * @param consumerTypes
	 *        The consumer types.
	 */
	public void setConsumerTypes(Set<String> consumerTypes) {
		this.consumerTypes = consumerTypes;
	}

	/**
	 * Set the base names supported by this factory.
	 *
	 * @param basenames
	 *        The list of base names (file paths without extensions) to use.
	 */
	public void setBasenames(String[] basenames) {
		this.basenames = basenames;
	}

	/**
	 * A pattern resolver to search for resources with.
	 *
	 * @param resourcePatternResolver
	 *        The pattern resolver to use.
	 */
	public void setResourcePatternResolver(ResourcePatternResolver resourcePatternResolver) {
		this.resourcePatternResolver = resourcePatternResolver;
	}

	/**
	 * Set the cache value to use for resolved resources, in seconds.
	 *
	 * @param cacheSeconds
	 *        The cache maximum seconds.
	 */
	public void setCacheSeconds(int cacheSeconds) {
		this.cacheSeconds = cacheSeconds;
	}

	/**
	 * The required roles to assign to resolved resources. Defaults to
	 * {@link SetupResource#USER_ROLES}.
	 *
	 * @param roles
	 *        The required roles to use.
	 */
	public void setRoles(Set<String> roles) {
		this.roles = roles;
	}

	/**
	 * Set the filename to content type mapping. Defaults to
	 * {@link SetupResourceUtils#DEFAULT_FILENAME_EXTENSION_CONTENT_TYPES}.
	 *
	 * @param fileExtensionContentTypeMapping
	 *        The filename to content type mapping to use.
	 */
	public void setFileExtensionContentTypeMapping(Map<String, String> fileExtensionContentTypeMapping) {
		this.fileExtensionContentTypeMapping = fileExtensionContentTypeMapping;
	}

	/**
	 * Set the locale to use for resources that have no locale specified in
	 * their filename.
	 *
	 * @param defaultLocale
	 *        The default locale.
	 */
	public void setDefaultLocale(Locale defaultLocale) {
		this.defaultLocale = defaultLocale;
	}

	/**
	 * Set a scope to use for all resolved resources.
	 *
	 * @param scope
	 *        the scope to set
	 * @since 1.1
	 */
	public void setScope(SetupResourceScope scope) {
		this.scope = scope;
	}

}
