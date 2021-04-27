/* ==================================================================
 * ProtocProtobufCompilerService.java - 26/04/2021 8:23:58 PM
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

package net.solarnetwork.node.io.protobuf;

import java.util.List;
import net.solarnetwork.node.settings.SettingSpecifier;
import net.solarnetwork.node.settings.SettingSpecifierProvider;
import net.solarnetwork.node.settings.support.BasicTextFieldSettingSpecifier;
import net.solarnetwork.node.support.BaseIdentifiable;
import net.solarnetwork.util.JavaCompiler;

/**
 * Service for compiling Protobuf definitions.
 * 
 * @author matt
 * @version 1.0
 */
public class ProtocProtobufCompilerService
		extends net.solarnetwork.common.protobuf.protoc.ProtocProtobufCompilerService
		implements SettingSpecifierProvider {

	/**
	 * Constructor.
	 * 
	 * @param compiler
	 *        the compiler
	 */
	public ProtocProtobufCompilerService(JavaCompiler compiler) {
		super(compiler);
	}

	@Override
	public String getSettingUID() {
		return "net.solarnetwork.node.io.protobuf.protoc";
	}

	@Override
	public List<SettingSpecifier> getSettingSpecifiers() {
		List<SettingSpecifier> result = BaseIdentifiable.baseIdentifiableSettings("");
		result.add(new BasicTextFieldSettingSpecifier("protocPath", DEFAULT_PROTOC_PATH));
		return result;
	}

}
