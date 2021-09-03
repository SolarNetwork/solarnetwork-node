/* ==================================================================
 * FileSettingSpecifier.java - 16/09/2019 5:04:13 pm
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

package net.solarnetwork.node.settings;

import java.util.Set;
import org.springframework.core.io.Resource;
import net.solarnetwork.settings.KeyedSettingSpecifier;

/**
 * A file-upload setting specifier.
 * 
 * <p>
 * This API is designed to work with the {@link SettingResourceHandler} API. A
 * {@link SettingSpecifierProvider} would expose a {@link FileSettingSpecifier}
 * while also implementing {@link SettingResourceHandler} to accept external
 * configuration resources.
 * </p>
 * 
 * @author matt
 * @version 1.1
 * @since 1.70
 */
public interface FileSettingSpecifier extends KeyedSettingSpecifier<Resource> {

	/**
	 * Get a set of file type specifiers this specifier can accept.
	 * 
	 * <p>
	 * A file type specifier can be either a file name extension, starting with
	 * a period character, or a MIME type, including those with a wildcard
	 * subtype. For example {@literal .jpg}, {@literal image/jpg}, and
	 * {@literal image/*} are valid values.
	 * </p>
	 * 
	 * <p>
	 * If the returned value is {@literal null} or empty, then all file types
	 * are acceptable.
	 * </p>
	 * 
	 * @return the set of acceptable types
	 */
	Set<String> getAcceptableFileTypeSpecifiers();

	/**
	 * Flag indicating if multiple files can be accepted by this setting.
	 * 
	 * @return {@literal true} if multiple files are acceptable
	 */
	boolean isMultiple();

}
