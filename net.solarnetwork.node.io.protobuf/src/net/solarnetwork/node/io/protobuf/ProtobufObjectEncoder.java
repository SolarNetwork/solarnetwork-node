/* ==================================================================
 * ProtobufObjectEncoder.java - 26/04/2021 3:20:18 PM
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

import static java.lang.String.format;
import static java.util.Arrays.asList;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.util.FileCopyUtils;
import net.solarnetwork.common.protobuf.ProtobufCompilerService;
import net.solarnetwork.node.settings.SettingResourceHandler;
import net.solarnetwork.node.settings.SettingSpecifier;
import net.solarnetwork.node.settings.SettingSpecifierProvider;
import net.solarnetwork.node.settings.SettingValueBean;
import net.solarnetwork.node.settings.SettingsCommand;
import net.solarnetwork.node.settings.SettingsUpdates;
import net.solarnetwork.node.settings.support.BasicFileSettingSpecifier;
import net.solarnetwork.node.settings.support.BasicTextFieldSettingSpecifier;
import net.solarnetwork.node.settings.support.BasicTitleSettingSpecifier;
import net.solarnetwork.node.support.BaseIdentifiable;

/**
 * Service for encoding datum into Protobuf messages.
 * 
 * @author matt
 * @version 1.0
 */
public class ProtobufObjectEncoder extends net.solarnetwork.common.protobuf.ProtobufObjectEncoder
		implements SettingSpecifierProvider, SettingResourceHandler {

	/** The setting UID for this service. */
	public static final String SETTING_UID = "net.solarnetwork.node.io.protobuf.enc";

	/** The default {@code protoDir} property value. */
	public static final String DEFAULT_PROTO_DIR = "var/" + SETTING_UID;

	/** The setting resource key for proto files. */
	public static final String RESOURCE_KEY_PROTO_FILES = "protoFiles";

	private Path protoDir = Paths.get(DEFAULT_PROTO_DIR);
	private String[] protoFileNames;

	@Override
	protected Map<String, ?> convertToMap(Object obj, Map<String, ?> parameters) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected ClassLoader compileProtobufResources(ProtobufCompilerService compiler) throws IOException {
		Iterable<Resource> resources = currentSettingResources(RESOURCE_KEY_PROTO_FILES);
		return compiler.compileProtobufResources(resources, null);
	}

	@Override
	public String getSettingUID() {
		return SETTING_UID;
	}

	@Override
	public List<SettingSpecifier> getSettingSpecifiers() {
		List<SettingSpecifier> result = BaseIdentifiable.baseIdentifiableSettings("");
		result.add(new BasicTextFieldSettingSpecifier("compilerServiceUidFilter", ""));
		result.add(new BasicTextFieldSettingSpecifier("messageClassName", ""));
		result.add(new BasicFileSettingSpecifier(RESOURCE_KEY_PROTO_FILES, null,
				new LinkedHashSet<>(asList(".proto", "text/*")), true));
		String[] files = getProtoFileNames();
		if ( files != null && files.length > 0 ) {
			for ( String name : files ) {
				result.add(new BasicTitleSettingSpecifier("protoFileNames", name, true));
			}
		}
		return result;
	}

	@Override
	public Iterable<Resource> currentSettingResources(String settingKey) {
		if ( !RESOURCE_KEY_PROTO_FILES.equals(settingKey) ) {
			return Collections.emptyList();
		}
		Path dir = getProtoDir();
		String[] fileNames = getProtoFileNames();
		if ( dir == null || fileNames == null || fileNames.length < 1 ) {
			return Collections.emptyList();
		}
		List<Resource> result = new ArrayList<>(fileNames.length);
		for ( String fileName : fileNames ) {
			result.add(new FileSystemResource(dir.resolve(fileName).toFile()));
		}
		return result;
	}

	@Override
	public SettingsUpdates applySettingResources(String settingKey, Iterable<Resource> resources)
			throws IOException {
		if ( !RESOURCE_KEY_PROTO_FILES.equals(settingKey) ) {
			return null;
		}
		Path dir = getProtoDir();
		if ( dir == null ) {
			return null;
		}

		// delete existing resources
		String[] fileNames = getProtoFileNames();
		if ( fileNames != null && fileNames.length > 0 ) {
			for ( String name : fileNames ) {
				Path f = dir.resolve(name);
				try {
					Files.delete(f);
				} catch ( IOException e ) {
					log.debug("Error deleting [{}]: {}", f, e.toString());
					// ignore this exception
				}
			}
		}

		List<String> names = new ArrayList<>(4);
		SettingsCommand updates = new SettingsCommand(null,
				asList(Pattern.compile("protoFileNames\\[.*")));
		if ( resources != null ) {
			if ( !Files.isDirectory(dir) ) {
				Files.createDirectories(dir);
			}
			int i = 0;
			for ( Resource r : resources ) {
				String name = r.getFilename();
				if ( !name.toLowerCase().endsWith(".proto") ) {
					continue;
				}
				Path f = dir.resolve(name);
				try (OutputStream out = Files.newOutputStream(f)) {
					FileCopyUtils.copy(r.getInputStream(), out);
				}
				updates.getValues().add(new SettingValueBean(format("protoFileNames[%d]", i), name));
				names.add(name);
				i++;
			}
		}
		setProtoFileNames(names.toArray(new String[names.size()]));
		return updates;

	}

	/**
	 * Get the proto directory path.
	 * 
	 * @return the directory path, never {@literal null}; defaults to
	 *         {@link #DEFAULT_PROTO_DIR}
	 */
	public Path getProtoDir() {
		return protoDir;
	}

	/**
	 * Set the proto directory path.
	 * 
	 * @param protoDir
	 *        the path to set
	 * @throws IllegalArgumentException
	 *         if {@code protoDir} is {@literal null}
	 */
	public void setProtoDir(Path protoDir) {
		if ( protoDir == null ) {
			throw new IllegalArgumentException("The protoDir path must not be null.");
		}
		this.protoDir = protoDir;
	}

	/**
	 * Get the configured proto file names.
	 * 
	 * <p>
	 * These are relative to {@link #getProtoDir()}.
	 * </p>
	 * 
	 * @return the file names
	 */
	public String[] getProtoFileNames() {
		return protoFileNames;
	}

	/**
	 * Set the proto file names.
	 * 
	 * @param protoFileNames
	 *        the file names to set
	 */
	public void setProtoFileNames(String[] protoFileNames) {
		this.protoFileNames = protoFileNames;
	}

}
