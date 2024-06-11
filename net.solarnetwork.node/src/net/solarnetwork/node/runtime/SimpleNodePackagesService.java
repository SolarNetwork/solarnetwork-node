/* ==================================================================
 * SimpleNodePackagesService.java - 12/06/2024 7:12:32 am
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

package net.solarnetwork.node.runtime;

import static java.util.Collections.singletonMap;
import static net.solarnetwork.domain.InstructionStatus.InstructionState.Completed;
import static net.solarnetwork.domain.InstructionStatus.InstructionState.Declined;
import static net.solarnetwork.node.reactor.InstructionUtils.createErrorResultParameters;
import static net.solarnetwork.node.reactor.InstructionUtils.createStatus;
import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import static net.solarnetwork.util.StringUtils.parseBoolean;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import java.util.zip.GZIPOutputStream;
import org.springframework.util.FileCopyUtils;
import net.solarnetwork.codec.JsonUtils;
import net.solarnetwork.node.reactor.Instruction;
import net.solarnetwork.node.reactor.InstructionHandler;
import net.solarnetwork.node.reactor.InstructionStatus;
import net.solarnetwork.node.service.PlatformPackageService;
import net.solarnetwork.node.service.PlatformPackageService.PlatformPackage;
import net.solarnetwork.node.service.support.BaseIdentifiable;

/**
 * Service to support node package management.
 *
 * <p>
 * This {@link InstructionHandler} responds to
 * {@link InstructionHandler#TOPIC_SYSTEM_CONFIGURATION SystemConfiguration}
 * requests where the {@link InstructionHandler#PARAM_SERVICE service} parameter
 * value is {@link #PACKAGES_SERVICE_UID "net.solarnetwork.node.packages"}. The
 * {@link #PARAM_FILTER filter} parameter value can be provided with a regular
 * expression to limit the returned packages, and the {@link #PARAM_STATUS
 * status} parameter value can be provided to limit the returned results to
 * installed, available, or all packages. The result is a JSON array on the
 * {@link InstructionHandler#PARAM_SERVICE_RESULT result} result parameter. The
 * {@link #PARAM_COMPRESSED compressed} parameter can be provided as
 * {@code true} to return a Base64-encoded gzip compressed version of the JSON.
 * If the compressed value is larger than the original JSON then the original
 * JSON will be returned instead, unless the {@code compressed} parameter is
 * provided as {@code force} in which case the compressed value will still be
 * returned.
 * </p>
 *
 * @author matt
 * @version 1.0
 * @since 3.12
 */
public class SimpleNodePackagesService extends BaseIdentifiable implements InstructionHandler {

	/** The default UID for this service. */
	public static final String PACKAGES_SERVICE_UID = "net.solarnetwork.node.packages";

	/** The default package name filter to use if none provided. */
	public static final Pattern DEFAULT_FILTER = Pattern.compile("(^sn-|solarnode)");

	/**
	 * An optional instruction parameter containing an regular expression
	 * pattern to filter the returned package names by.
	 */
	public static final String PARAM_FILTER = "filter";

	/**
	 * An optional instruction parameter containing a package status flag.
	 *
	 * If {@literal installed} then only installed packages will be included. If
	 * {@literal available} then only packages not installed will be included.
	 * If {@literal all} then both installed and available packages will be
	 * included. If not specified then {@literal installed} will be assumed.
	 */
	public static final String PARAM_STATUS = "status";

	/**
	 * An optional instruction parameter containing a boolean flag that, when
	 * {@literal true}, means the result JSON should be gzip-compressed and
	 * encoded into a Base64 string representation.
	 *
	 * <p>
	 * Note that only successful results will be compressed. If the compressed
	 * result is larger than the uncompressed JSON, the uncompressed JSON will
	 * be returned instead.
	 * </p>
	 */
	public static final String PARAM_COMPRESSED = "compress";

	/**
	 * An enumeration of possible {@link SimpleNodePackagesService#PARAM_STATUS}
	 * values.
	 */
	public static enum Status {

		/** Include only installed packages. */
		Installed(true),

		/** Include only available (not installed) packages. */
		Available(false),

		/** Include both installed and available packages. */
		All(null);

		private Boolean flag;

		private Status(Boolean flag) {
			this.flag = flag;
		}

		/**
		 * Get an enumeration for a parameter value.
		 *
		 * <p>
		 * If {@code value} is not a value enumeration constant
		 * (case-insensitive) then {@code Installed} will be returned.
		 * </p>
		 *
		 * @param value
		 *        the parameter value to get the enum instance for
		 * @return the enumeration instance, never {@literal null}
		 */
		public static Status forParameterValue(String value) {
			if ( value != null && !value.isEmpty() ) {
				value = value.toLowerCase();
				if ( "available".equals(value) ) {
					return Available;
				} else if ( "all".equals(value) ) {
					return All;
				}
			}
			return Installed;
		}

		/**
		 * Get the flag value for this status, suitable for passing to
		 * {@link PlatformPackageService#listNamedPackages(String, Boolean)}.
		 *
		 * @return the flag value
		 */
		public Boolean flagValue() {
			return flag;
		}

	}

	private final Collection<PlatformPackageService> providers;

	/**
	 * Constructor.
	 *
	 * @param providers
	 *        the collection of package service providers
	 * @throws IllegalArgumentException
	 *         if any argument is {@literal null}
	 */
	public SimpleNodePackagesService(Collection<PlatformPackageService> providers) {
		super();
		this.providers = requireNonNullArgument(providers, "providers");
	}

	@Override
	public boolean handlesTopic(String topic) {
		return InstructionHandler.TOPIC_SYSTEM_CONFIGURATION.equals(topic);
	}

	private Pattern filter(Instruction instruction) {
		final String filter = instruction.getParameterValue(PARAM_FILTER);
		if ( filter == null ) {
			return DEFAULT_FILTER;
		}

		if ( filter.isEmpty() ) {
			return null;
		}

		return Pattern.compile(filter, Pattern.CASE_INSENSITIVE);
	}

	private Status status(Instruction instruction) {
		final String status = instruction.getParameterValue(PARAM_STATUS);
		return Status.forParameterValue(status);
	}

	@Override
	public InstructionStatus processInstruction(Instruction instruction) {
		if ( instruction == null || !handlesTopic(instruction.getTopic()) ) {
			return null;
		}
		final String uid = getUid() != null ? getUid() : PACKAGES_SERVICE_UID;
		final String serviceId = instruction.getParameterValue(PARAM_SERVICE);
		if ( !uid.equals(serviceId) ) {
			return null;
		}

		final Pattern filter;
		try {
			filter = filter(instruction);
		} catch ( PatternSyntaxException e ) {
			return createStatus(instruction, Declined, createErrorResultParameters(
					"Invalid filter pattern: " + e.getMessage(), "NPS.0001"));
		}

		final Status status = status(instruction);

		// return list of all available control IDs
		final List<PlatformPackage> packages = new ArrayList<>(32);
		for ( PlatformPackageService provider : providers ) {
			// note name filter not used so we can use regular expression matching
			Future<Iterable<PlatformPackage>> result = provider.listNamedPackages(null,
					status.flagValue());
			try {
				Iterable<PlatformPackage> list = result.get(1, TimeUnit.MINUTES);
				if ( list != null ) {
					for ( PlatformPackage pkg : list ) {
						if ( filter != null ) {
							Matcher m = filter.matcher(pkg.getName());
							if ( !m.find() ) {
								continue;
							}
						}
						packages.add(pkg);
					}
				}
			} catch ( TimeoutException e ) {
				return createStatus(instruction, Declined,
						createErrorResultParameters("Timeout waiting for package listing.", "NPS.0002"));
			} catch ( ExecutionException e ) {
				Throwable cause = e.getCause();
				return createStatus(instruction, Declined, createErrorResultParameters(
						"Error listing packages: " + cause.getMessage(), "NPS.0003"));
			} catch ( CancellationException | InterruptedException e ) {
				return createStatus(instruction, Declined,
						createErrorResultParameters("Package listing cancelled.", "NPS.0004"));
			}
		}

		return createStatus(instruction, Completed,
				singletonMap(PARAM_SERVICE_RESULT, resultValue(instruction, packages)));
	}

	private Object resultValue(Instruction instruction, Object result) {
		final String compressParamValue = instruction.getParameterValue(PARAM_COMPRESSED);
		final boolean force = "force".equalsIgnoreCase(compressParamValue);
		final boolean compress = force || parseBoolean(compressParamValue);
		if ( !compress ) {
			return result;
		}
		final String defaultJson = "[]";
		String json = JsonUtils.getJSONString(result, defaultJson);
		if ( json == null || defaultJson.equals(json) ) {
			return json;
		}

		try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
				GZIPOutputStream gzip = new GZIPOutputStream(baos);
				OutputStreamWriter out = new OutputStreamWriter(gzip, StandardCharsets.UTF_8)) {
			FileCopyUtils.copy(json, out);
			String b64 = Base64.getEncoder().encodeToString(baos.toByteArray());
			return !force && b64.length() > json.length() ? json : b64;
		} catch ( IOException e ) {
			// ignore and return uncompressed
			return json;
		}
	}
}
