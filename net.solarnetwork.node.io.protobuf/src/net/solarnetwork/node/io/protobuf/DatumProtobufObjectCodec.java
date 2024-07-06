/* ==================================================================
 * DatumProtobufObjectCodec.java - 26/04/2021 3:20:18 PM
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
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import org.springframework.beans.BeansException;
import org.springframework.beans.PropertyAccessor;
import org.springframework.beans.PropertyAccessorFactory;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.util.FileCopyUtils;
import net.solarnetwork.common.protobuf.ProtobufCompilerService;
import net.solarnetwork.common.protobuf.ProtobufMessagePopulator;
import net.solarnetwork.domain.datum.Datum;
import net.solarnetwork.domain.datum.DatumSamplesType;
import net.solarnetwork.node.domain.datum.SimpleDatum;
import net.solarnetwork.node.service.support.BaseIdentifiable;
import net.solarnetwork.node.settings.SettingResourceHandler;
import net.solarnetwork.node.settings.SettingValueBean;
import net.solarnetwork.node.settings.SettingsCommand;
import net.solarnetwork.node.settings.SettingsUpdates;
import net.solarnetwork.node.settings.support.BasicFileSettingSpecifier;
import net.solarnetwork.settings.SettingSpecifier;
import net.solarnetwork.settings.SettingSpecifierProvider;
import net.solarnetwork.settings.support.BasicGroupSettingSpecifier;
import net.solarnetwork.settings.support.BasicTextFieldSettingSpecifier;
import net.solarnetwork.settings.support.BasicTitleSettingSpecifier;
import net.solarnetwork.settings.support.SettingUtils;
import net.solarnetwork.util.ArrayUtils;

/**
 * Service for encoding datum into Protobuf messages.
 *
 * <p>
 * This service supports encoding {@link Map} and {@link Datum} objects.
 * {@code Map} objects are used directly as input while {@code Datum} will use
 * {@link Datum#asSimpleMap()}. The input {@code Map} is then transformed into a
 * new {@code Map} according to the configured property configurations. The
 * transformed {@code Map} is what will be passed to
 * {@link ProtobufMessagePopulator#setMessageProperties(Map, boolean)} to be
 * encoded into a Protobuf message.
 * </p>
 *
 * <p>
 * The {@link #decodeFromBytes(byte[], Map)} method always returns a
 * {@link net.solarnetwork.node.domain.datum.SimpleDatum} instance.
 * </p>
 *
 * @author matt
 * @version 2.1
 */
public class DatumProtobufObjectCodec extends net.solarnetwork.common.protobuf.ProtobufObjectCodec
		implements SettingSpecifierProvider, SettingResourceHandler {

	/** The setting UID for this service. */
	public static final String SETTING_UID = "net.solarnetwork.node.io.protobuf.enc.datum";

	/** The default {@code protoDir} property value. */
	public static final String DEFAULT_PROTO_DIR = "var/" + SETTING_UID;

	/** The setting resource key for proto files. */
	public static final String RESOURCE_KEY_PROTO_FILES = "protoFiles";

	private Path protoDir = Paths.get(DEFAULT_PROTO_DIR);
	private String[] protoFileNames;
	private DatumFieldConfig[] propConfigs;

	@Override
	protected Map<String, ?> convertToMap(Object obj, Map<String, ?> parameters) {
		DatumFieldConfig[] confs = getPropConfigs();
		if ( confs == null || confs.length < 1 ) {
			return null;
		}
		Map<String, ?> data = dataForObject(obj);
		if ( data == null ) {
			return null;
		}
		Map<String, Object> result = new LinkedHashMap<>(confs.length);
		for ( DatumFieldConfig conf : confs ) {
			String key = conf.getDatumProperty();
			String field = conf.getFieldProperty();
			if ( key == null || key.isEmpty() || field == null || field.isEmpty() ) {
				continue;
			}
			Object val = data.get(key);
			if ( val != null ) {
				result.put(field, val);
			}
		}
		return (result.isEmpty() ? null : result);
	}

	@Override
	public Object decodeFromBytes(byte[] data, Map<String, ?> parameters) throws IOException {
		final String className = getMessageClassName();
		if ( data == null ) {
			throw new IOException(format("No data provided to decode %s message.", className));
		}
		DatumFieldConfig[] confs = getPropConfigs();
		if ( confs == null || confs.length < 1 ) {
			throw new IOException(format("No property configurations provided to decode %s message.",
					getMessageClassName()));
		}
		Object msg = super.decodeFromBytes(data, parameters);
		PropertyAccessor accessor = PropertyAccessorFactory.forBeanPropertyAccess(msg);
		SimpleDatum result = SimpleDatum.nodeDatum(null);
		for ( DatumFieldConfig conf : confs ) {
			String key = conf.getDatumProperty();
			DatumSamplesType type = conf.getPropertyType();
			String field = conf.getFieldProperty();
			if ( key == null || key.isEmpty() || field == null || field.isEmpty() ) {
				continue;
			}
			try {
				Object val = accessor.getPropertyValue(field);
				if ( val != null ) {
					result.putSampleValue(type, key, val);
				}
			} catch ( BeansException e ) {
				log.debug("Error reading Protobuf message {} field {}: {}", className, field,
						e.getMessage());
			}
		}
		if ( result.getSamples() == null || result.getSamples().isEmpty() ) {
			throw new IOException(
					format("No datum properties populated from Protobuf message %s", className));
		}
		return result;
	}

	@SuppressWarnings("unchecked")
	private Map<String, ?> dataForObject(Object obj) {
		if ( obj instanceof Map ) {
			return (Map<String, ?>) obj;
		} else if ( obj instanceof Datum ) {
			Datum d = (Datum) obj;
			return d.asSimpleMap();
		}
		log.debug("Can not convert object that does not implement Datum or Map: {}", obj);
		return null;
	}

	@Override
	protected ClassLoader compileProtobufResources(ProtobufCompilerService compiler) throws IOException {
		Iterable<Resource> resources = currentSettingResources(RESOURCE_KEY_PROTO_FILES);
		return compiler.compileProtobufResources(resources, null);
	}

	@Override
	public String getSettingUid() {
		return SETTING_UID;
	}

	@Override
	public List<SettingSpecifier> getSettingSpecifiers() {
		List<SettingSpecifier> result = BaseIdentifiable.baseIdentifiableSettings("");
		result.add(new BasicTextFieldSettingSpecifier("compilerServiceUidFilter", null, false,
				"(objectClass=net.solarnetwork.common.protobuf.ProtobufCompilerService)"));
		result.add(new BasicTextFieldSettingSpecifier("messageClassName", ""));
		result.add(new BasicFileSettingSpecifier(RESOURCE_KEY_PROTO_FILES, null,
				new LinkedHashSet<>(asList(".proto", "text/*")), true));
		String[] files = getProtoFileNames();
		if ( files != null && files.length > 0 ) {
			for ( String name : files ) {
				result.add(new BasicTitleSettingSpecifier("protoFileNames", name, true));
			}
		}

		DatumFieldConfig[] confs = getPropConfigs();
		List<DatumFieldConfig> confsList = (confs != null ? Arrays.asList(confs)
				: Collections.<DatumFieldConfig> emptyList());
		result.add(SettingUtils.dynamicListSettingSpecifier("propConfigs", confsList,
				new SettingUtils.KeyedListCallback<DatumFieldConfig>() {

					@Override
					public Collection<SettingSpecifier> mapListSettingKey(DatumFieldConfig value,
							int index, String key) {
						BasicGroupSettingSpecifier configGroup = new BasicGroupSettingSpecifier(
								DatumFieldConfig.settings(key + "."));
						return Collections.<SettingSpecifier> singletonList(configGroup);
					}
				}));

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

	/**
	 * Get the property configurations.
	 *
	 * @return the property configurations
	 */
	public DatumFieldConfig[] getPropConfigs() {
		return propConfigs;
	}

	/**
	 * Set the property configurations to use.
	 *
	 * @param propConfigs
	 *        the configs to use
	 */
	public void setPropConfigs(DatumFieldConfig[] propConfigs) {
		this.propConfigs = propConfigs;
	}

	/**
	 * Get the number of configured {@code propConfigs} elements.
	 *
	 * @return the number of {@code propConfigs} elements
	 */
	public int getPropConfigsCount() {
		DatumFieldConfig[] confs = this.propConfigs;
		return (confs == null ? 0 : confs.length);
	}

	/**
	 * Adjust the number of configured {@code propConfigs} elements.
	 *
	 * <p>
	 * Any newly added element values will be set to new
	 * {@link DatumFieldConfig} instances.
	 * </p>
	 *
	 * @param count
	 *        The desired number of {@code propConfigs} elements.
	 */
	public void setPropConfigsCount(int count) {
		this.propConfigs = ArrayUtils.arrayWithLength(this.propConfigs, count, DatumFieldConfig.class,
				null);
	}

}
