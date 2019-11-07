/* ==================================================================
 * BasicFileSettingSpecifier.java - 16/09/2019 5:21:33 pm
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

package net.solarnetwork.node.settings.support;

import java.util.Set;
import org.springframework.core.io.Resource;
import net.solarnetwork.node.settings.FileSettingSpecifier;
import net.solarnetwork.node.settings.MappableSpecifier;
import net.solarnetwork.node.settings.SettingSpecifier;

/**
 * Basic implementation of {@link FileSettingSpecifier}.
 * 
 * @author matt
 * @version 1.0
 */
public class BasicFileSettingSpecifier extends BaseKeyedSettingSpecifier<Resource>
		implements FileSettingSpecifier {

	private final Set<String> acceptableFileTypeSpecifiers;
	private final boolean multiple;

	/**
	 * Constructor.
	 * 
	 * <p>
	 * This sets {@code acceptableFileTypeSpecifiers} to {@literal null} and
	 * {@code multiple} to {@literal false}.
	 * </p>
	 * 
	 * @param key
	 *        the key
	 * @param defaultValue
	 *        the default value
	 */
	public BasicFileSettingSpecifier(String key, Resource defaultValue) {
		this(key, defaultValue, null, false);
	}

	/**
	 * Constructor.
	 * 
	 * @param key
	 *        the key
	 * @param defaultValue
	 *        the default value
	 * @param acceptableFileTypeSpecifiers
	 *        the acceptable file type specifiers, or {@literal null} if all
	 *        files are acceptable
	 * @param multiple
	 *        {@literal true} if multiple files can be accepted,
	 *        {@literal false} if only a single file is acceptable
	 */
	public BasicFileSettingSpecifier(String key, Resource defaultValue,
			Set<String> acceptableFileTypeSpecifiers, boolean multiple) {
		super(key, defaultValue);
		this.acceptableFileTypeSpecifiers = acceptableFileTypeSpecifiers;
		this.multiple = multiple;
	}

	@Override
	public SettingSpecifier mappedWithPlaceholer(String template) {
		BasicFileSettingSpecifier spec = new BasicFileSettingSpecifier(String.format(template, getKey()),
				getDefaultValue(), getAcceptableFileTypeSpecifiers(), isMultiple());
		spec.setTitle(getTitle());
		spec.setDescriptionArguments(getDescriptionArguments());
		return spec;
	}

	@SuppressWarnings("deprecation")
	@Override
	public SettingSpecifier mappedWithMapper(
			net.solarnetwork.node.settings.KeyedSettingSpecifier.Mapper mapper) {
		return mappedWithMapper((MappableSpecifier.Mapper) mapper);
	}

	@Override
	public SettingSpecifier mappedWithMapper(MappableSpecifier.Mapper mapper) {
		BasicFileSettingSpecifier spec = new BasicFileSettingSpecifier(mapper.mapKey(getKey()),
				getDefaultValue(), getAcceptableFileTypeSpecifiers(), isMultiple());
		spec.setTitle(getTitle());
		spec.setDescriptionArguments(getDescriptionArguments());
		return spec;
	}

	@Override
	public Set<String> getAcceptableFileTypeSpecifiers() {
		return acceptableFileTypeSpecifiers;
	}

	@Override
	public boolean isMultiple() {
		return multiple;
	}

}
