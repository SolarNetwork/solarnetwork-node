/* ==================================================================
 * NodeS3ResourceStorageService.java - 16/10/2019 2:17:07 pm
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

package net.solarnetwork.node.io.s3;

import static java.util.stream.Collectors.toList;
import static java.util.stream.StreamSupport.stream;
import static net.solarnetwork.settings.support.SettingUtils.mappedWithPrefix;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Function;
import org.springframework.context.MessageSource;
import org.springframework.core.io.Resource;
import net.solarnetwork.common.s3.S3ResourceStorageService;
import net.solarnetwork.common.s3.sdk.SdkS3Client;
import net.solarnetwork.node.service.IdentityService;
import net.solarnetwork.node.service.support.BaseIdentifiable;
import net.solarnetwork.service.OptionalService;
import net.solarnetwork.service.ProgressListener;
import net.solarnetwork.service.ResourceStorageService;
import net.solarnetwork.settings.SettingSpecifier;
import net.solarnetwork.settings.SettingSpecifierProvider;
import net.solarnetwork.settings.SettingsChangeObserver;
import net.solarnetwork.settings.support.BasicToggleSettingSpecifier;

/**
 * Adaptation of {@link S3ResourceStorageService} to work with node settings.
 *
 * @author matt
 * @version 2.0
 */
public class NodeS3ResourceStorageService extends BaseIdentifiable
		implements ResourceStorageService, SettingsChangeObserver, SettingSpecifierProvider {

	private final OptionalService<IdentityService> identityService;
	private final S3ResourceStorageService delegate;
	private boolean nodeIdPrefix;

	/**
	 * Constructor.
	 *
	 * @param identityService
	 *        service the identity service
	 * @param executor
	 *        the executor
	 */
	public NodeS3ResourceStorageService(OptionalService<IdentityService> identityService,
			Executor executor) {
		super();
		this.identityService = identityService;
		this.delegate = new S3ResourceStorageService(executor);
		this.delegate.setS3Client(new SdkS3Client());
		this.nodeIdPrefix = true;
	}

	/**
	 * Set up once after fully configured.
	 */
	public void startup() {
		configurationChanged(null);
	}

	@Override
	public void configurationChanged(Map<String, Object> properties) {
		this.delegate.configurationChanged(properties);
	}

	private String pathPrefix() {
		String prefix = delegate.getObjectKeyPrefix();
		if ( nodeIdPrefix ) {
			IdentityService s = identityService.service();
			Long nodeId = (s != null ? s.getNodeId() : null);
			if ( nodeId == null ) {
				throw new RuntimeException(
						"Node ID required but not available. Perhaps this node is not set up yet?");
			}
			if ( prefix == null || prefix.isEmpty() ) {
				prefix = nodeId + "/";
			} else {
				prefix += nodeId + "/";
			}
		}
		return prefix;
	}

	private String mapPathPrefix(String prefix, String path) {
		if ( path != null && prefix != null && !path.startsWith(prefix) ) {
			return prefix + path;
		}
		return path;
	}

	private Function<String, String> pathPrefixMapper() {
		final String prefix = pathPrefix();
		return s -> mapPathPrefix(prefix, s);
	}

	// ResourceStorageService

	@Override
	public String toString() {
		final S3ResourceStorageService s = getDelegate();
		StringBuilder buf = new StringBuilder("NodeS3ResourceStorageService{uid=");
		buf.append(getUid());
		if ( s != null && s.isConfigured() ) {
			buf.append(",client=").append(s.getS3Client());
			try {
				String prefix = pathPrefix();
				buf.append(",path=").append(prefix);
			} catch ( Exception e ) {
				// ignore (missing node ID)
			}
		}
		buf.append("}");
		return buf.toString();
	}

	@Override
	public boolean isConfigured() {
		return delegate.isConfigured();
	}

	@Override
	public CompletableFuture<Iterable<Resource>> listResources(String pathPrefix) {
		String prefix = pathPrefixMapper().apply(pathPrefix);
		return delegate.listResources(prefix);
	}

	@Override
	public URL resourceStorageUrl(String path) {
		String p = pathPrefixMapper().apply(path);
		return delegate.resourceStorageUrl(p);
	}

	@Override
	public CompletableFuture<Boolean> saveResource(String path, Resource resource, boolean replace,
			ProgressListener<Resource> progressListener) {
		String p = pathPrefixMapper().apply(path);
		return delegate.saveResource(p, resource, replace, progressListener);
	}

	@Override
	public CompletableFuture<Set<String>> deleteResources(Iterable<String> paths) {
		List<String> p = stream(paths.spliterator(), false).map(pathPrefixMapper()).collect(toList());
		return delegate.deleteResources(p);
	}

	// SettingsSpecifierProvider

	@Override
	public String getSettingUid() {
		return "net.solarnetwork.node.io.s3";
	}

	@Override
	public String getDisplayName() {
		return "S3 Storage Service";
	}

	@Override
	public List<SettingSpecifier> getSettingSpecifiers() {
		List<SettingSpecifier> results = new ArrayList<SettingSpecifier>(12);
		results.addAll(baseIdentifiableSettings("", "", ""));
		results.addAll(mappedWithPrefix(delegate.getSettingSpecifiers(), "delegate."));
		results.add(new BasicToggleSettingSpecifier("nodeIdPrefix", Boolean.TRUE));
		return results;
	}

	// Accessors

	@Override
	public void setMessageSource(MessageSource messageSource) {
		super.setMessageSource(messageSource);
		this.delegate.setMessageSource(messageSource);
	}

	/**
	 * Get the configured identity service.
	 *
	 * @return the identity service, never {@literal null}
	 */
	public OptionalService<IdentityService> getIdentityService() {
		return identityService;
	}

	/**
	 * Get the delegate.
	 *
	 * @return the delegate
	 */
	public S3ResourceStorageService getDelegate() {
		return delegate;
	}

	/**
	 * Get the node ID prefix setting.
	 *
	 * <p>
	 * Note this prefix is added <b>after</b> any prefix configured in
	 * {@link S3ResourceStorageService#getObjectKeyPrefix()}.
	 * </p>
	 *
	 * @return {@literal true} if the node ID should be included in the S3 path
	 *         of all objects
	 */
	public boolean isNodeIdPrefix() {
		return nodeIdPrefix;
	}

	/**
	 * Toggle the ndoe ID prefix setting.
	 *
	 * @param nodeIdPrefix
	 *        {@literal true} if the node ID should be included in the S3 path
	 *        of all objects
	 */
	public void setNodeIdPrefix(boolean nodeIdPrefix) {
		this.nodeIdPrefix = nodeIdPrefix;
	}

}
