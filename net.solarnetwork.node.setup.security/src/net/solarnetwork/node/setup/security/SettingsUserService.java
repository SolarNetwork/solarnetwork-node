/* ==================================================================
 * SettingsUserService.java - 27/07/2016 8:09:01 AM
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

package net.solarnetwork.node.setup.security;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFilePermission;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.util.FileCopyUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.solarnetwork.codec.JsonUtils;
import net.solarnetwork.dao.BasicBatchOptions;
import net.solarnetwork.dao.BatchableDao.BatchCallback;
import net.solarnetwork.dao.BatchableDao.BatchCallbackResult;
import net.solarnetwork.node.backup.BackupResource;
import net.solarnetwork.node.backup.BackupResourceInfo;
import net.solarnetwork.node.backup.BackupResourceProvider;
import net.solarnetwork.node.backup.BackupResourceProviderInfo;
import net.solarnetwork.node.backup.ResourceBackupResource;
import net.solarnetwork.node.backup.SimpleBackupResourceInfo;
import net.solarnetwork.node.backup.SimpleBackupResourceProviderInfo;
import net.solarnetwork.node.dao.SettingDao;
import net.solarnetwork.node.domain.Setting;
import net.solarnetwork.node.service.IdentityService;
import net.solarnetwork.node.setup.UserAuthenticationInfo;
import net.solarnetwork.node.setup.UserProfile;
import net.solarnetwork.node.setup.UserService;

/**
 * {@link UserDetailsService} that uses {@link SettingDao} for users and roles.
 *
 * @author matt
 * @version 2.3
 */
public class SettingsUserService implements UserService, UserDetailsService, BackupResourceProvider {

	/** The setting type for a user record. */
	public static final String SETTING_TYPE_USER = "solarnode.user";

	/** The setting type for a role record. */
	public static final String SETTING_TYPE_ROLE = "solarnode.role";

	/** The authority name grated to users. */
	public static final String GRANTED_AUTH_USER = "ROLE_USER";

	/**
	 * The default value for the {@code usersFilePath} property.
	 *
	 * @since 2.2
	 */
	public static final String DEFAULT_USERS_FILE_PATH = "conf/users.json";

	private static final String BACKUP_RESOURCE_NAME_USERS_FILE = "users.json";

	private final Logger log = LoggerFactory.getLogger(getClass());

	private final SettingDao settingDao;
	private final IdentityService identityService;
	private final PasswordEncoder passwordEncoder;
	private final ObjectMapper objectMapper;
	private String usersFilePath = DEFAULT_USERS_FILE_PATH;
	private MessageSource messageSource;

	/**
	 * Constructor.
	 *
	 * @param settingDao
	 *        The setting DAO to use.
	 * @param identityService
	 *        The {@link IdentityService} to use.
	 * @param passwordEncoder
	 *        The {@link PasswordEncoder} to use for legacy user support.
	 */
	public SettingsUserService(SettingDao settingDao, IdentityService identityService,
			PasswordEncoder passwordEncoder) {
		super();
		this.settingDao = settingDao;
		this.identityService = identityService;
		this.passwordEncoder = passwordEncoder;
		this.objectMapper = JsonUtils.newObjectMapper();
	}

	@Override
	public synchronized UserDetails loadUserByUsername(String username)
			throws UsernameNotFoundException {
		// first look in users file
		Map<String, UserEntity> users = loadUsersFile();
		UserDetails result = loadUserByUsername(username, users);
		if ( result != null && !users.containsKey(username)
				&& settingDao.getSetting(username, SETTING_TYPE_USER) != null ) {
			// migrate settings user to users file
			final long now = System.currentTimeMillis();
			users.put(username, new UserEntity(now, now, username, result.getPassword(), result
					.getAuthorities().stream().map(a -> a.getAuthority()).collect(Collectors.toSet())));
			saveUsersFile(users);
			// clean up and remove user credentials from settings
			settingDao.deleteSetting(username, SETTING_TYPE_USER);
			settingDao.deleteSetting(username, SETTING_TYPE_ROLE);
		}
		return result;
	}

	private UserDetails loadUserByUsername(String username, Map<String, UserEntity> users)
			throws UsernameNotFoundException {
		UserDetails result = null;

		UserEntity user = users.get(username);
		if ( user != null ) {
			result = new User(username, user.getPassword(), user.getRoles().stream()
					.map(r -> new SimpleGrantedAuthority(r)).collect(Collectors.toList()));
		} else {
			// next try user setting
			String password = settingDao.getSetting(username, SETTING_TYPE_USER);
			if ( password == null && identityService != null && passwordEncoder != null
					&& !someUserExists(users) ) {
				// for backwards-compat with nodes created before user auth, provide a default
				Long nodeId = identityService.getNodeId();
				if ( nodeId != null && nodeId.toString().equalsIgnoreCase(username) ) {
					password = passwordEncoder.encode("solar");
					GrantedAuthority auth = new SimpleGrantedAuthority(GRANTED_AUTH_USER);
					result = new User(username, password, Collections.singleton(auth));
				}
			} else if ( password != null ) {
				Collection<GrantedAuthority> auths;
				String role = settingDao.getSetting(username, SETTING_TYPE_ROLE);
				if ( role != null ) {
					GrantedAuthority auth = new SimpleGrantedAuthority(role);
					auths = Collections.singleton(auth);
				} else {
					auths = Collections.emptySet();
				}
				result = new User(username, password, auths);
			}
		}

		if ( result == null ) {
			throw new UsernameNotFoundException(username);
		}
		return result;
	}

	private synchronized Map<String, UserEntity> loadUsersFile() {
		final File usersFile = new File(usersFilePath);
		final Map<String, UserEntity> result = new LinkedHashMap<>(4);
		if ( usersFile.canRead() ) {
			try {
				UserEntity[] users = objectMapper.readValue(usersFile, UserEntity[].class);
				for ( UserEntity user : users ) {
					result.put(user.getUsername(), user);
				}
			} catch ( IOException e ) {
				log.warn("Error reading users data from {}: {}", usersFilePath, e.getMessage());
			}
		}
		return result;
	}

	private synchronized void saveUsersFile(Map<String, UserEntity> users) {
		Path usersFile = Paths.get(usersFilePath);
		try {
			Path usersFileDir = usersFile.getParent();
			if ( !Files.isDirectory(usersFileDir) ) {
				Files.createDirectories(usersFileDir);
			}
			if ( users == null || users.isEmpty() ) {
				Files.deleteIfExists(usersFile);
			} else {
				objectMapper.writeValue(usersFile.toFile(), users.values());
				try {
					Files.setPosixFilePermissions(usersFile,
							EnumSet.of(PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE));
				} catch ( UnsupportedOperationException e ) {
					// too bad, just ignore
				}
			}
		} catch ( IOException e ) {
			log.error("Error saving users data to {}: {}", usersFilePath, e.getMessage(), e);
		}
	}

	/**
	 * Test if some user exists.
	 *
	 * <p>
	 * This implementation returns {@literal true} only if either:
	 * </p>
	 * <ol>
	 * <li>a user is found in {@code usersFilePath} with a
	 * {@link #GRANTED_AUTH_USER} role</li>
	 * <li>a {@link #SETTING_TYPE_ROLE} record with a value of
	 * {@link #GRANTED_AUTH_USER} exists along with a {@link #SETTING_TYPE_USER}
	 * for the same key</li>
	 * </ol>
	 *
	 * {@inheritDoc}
	 */
	@Override
	public boolean someUserExists() {
		return someUserExists(loadUsersFile());
	}

	private boolean someUserExists(final Map<String, UserEntity> users) {
		if ( users.values().stream().filter(u -> u.getRoles().contains(GRANTED_AUTH_USER)).findAny()
				.isPresent() ) {
			return true;
		}
		final AtomicBoolean result = new AtomicBoolean(false);
		final Map<String, Boolean> userMap = new HashMap<>(2);
		settingDao.batchProcess(new BatchCallback<Setting>() {

			@Override
			public BatchCallbackResult handle(Setting setting) {
				if ( setting.getType().equals(SETTING_TYPE_ROLE)
						&& GRANTED_AUTH_USER.equals(setting.getValue()) ) {
					if ( userMap.containsKey(setting.getKey()) ) {
						// found role + user
						result.set(true);
						return BatchCallbackResult.STOP;
					}
					userMap.put(setting.getKey(), Boolean.TRUE);
				} else if ( setting.getType().equals(SETTING_TYPE_USER) ) {
					if ( Boolean.TRUE.equals(userMap.get(setting.getKey())) ) {
						// found role + user
						result.set(true);
						return BatchCallbackResult.STOP;
					}
					userMap.put(setting.getKey(), Boolean.FALSE);
				}
				return BatchCallbackResult.CONTINUE;
			}
		}, new BasicBatchOptions("FindUser"));
		return result.get();
	}

	/**
	 * Update the active user's password.
	 *
	 * @param existingPassword
	 *        The existing password.
	 * @param newPassword
	 *        The new password to set.
	 * @param newPasswordAgain
	 *        The new password, repeated.
	 * @throws InsufficientAuthenticationException
	 *         If an active user is not available.
	 * @throws UsernameNotFoundException
	 *         If the active user cannot be found in settings.
	 * @throws BadCredentialsException
	 *         If {@code existingPassword} does not match the password in
	 *         settings.
	 * @throws IllegalArgumentException
	 *         if the {@code newPassword} and {@code newPasswordAgain} values do
	 *         not match, or are {@literal null}
	 */
	@Override
	public void changePassword(final String existingPassword, final String newPassword,
			final String newPasswordAgain) {
		if ( newPassword == null || newPasswordAgain == null || !newPassword.equals(newPasswordAgain) ) {
			throw new IllegalArgumentException(
					"New password not provided or does not match repeated password.");
		}
		final Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		final UserDetails activeUser = (auth == null ? null : (UserDetails) auth.getPrincipal());
		if ( activeUser == null ) {
			throw new InsufficientAuthenticationException("Active user not found.");
		}

		final Map<String, UserEntity> users = loadUsersFile();
		final UserDetails dbUser = loadUserByUsername(activeUser.getUsername(), users);
		if ( dbUser == null ) {
			throw new UsernameNotFoundException("User not found");
		}

		final boolean userFromUsersFile = users.containsKey(activeUser.getUsername());

		if ( passwordEncoder != null ) {
			if ( !passwordEncoder.matches(existingPassword, dbUser.getPassword()) ) {
				throw new BadCredentialsException("Existing password does not match.");
			}
		} else if ( !existingPassword.equals(dbUser.getPassword()) ) {
			throw new BadCredentialsException("Existing password does not match.");
		}

		String password;
		if ( passwordEncoder != null ) {
			password = passwordEncoder.encode(newPassword);
		} else {
			password = newPassword;
		}

		users.compute(dbUser.getUsername(), (k, v) -> {
			if ( v != null ) {
				return v.withPassword(password);
			}
			final long now = System.currentTimeMillis();
			return new UserEntity(now, now, dbUser.getUsername(), password,
					Collections.singleton(GRANTED_AUTH_USER));
		});

		saveUsersFile(users);

		if ( !userFromUsersFile ) {
			// clean up and remove user credentials from settings
			settingDao.deleteSetting(dbUser.getUsername(), SETTING_TYPE_USER);
			settingDao.deleteSetting(dbUser.getUsername(), SETTING_TYPE_ROLE);
		}
	}

	@Override
	public void changeUsername(final String newUsername, final String newUsernameAgain) {
		if ( newUsername == null || newUsernameAgain == null || !newUsername.equals(newUsernameAgain) ) {
			throw new IllegalArgumentException(
					"New username not provided or does not match repeated username.");
		}

		final Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		final UserDetails activeUser = (auth == null ? null : (UserDetails) auth.getPrincipal());
		if ( activeUser == null ) {
			throw new InsufficientAuthenticationException("Active user not found.");
		}

		final Map<String, UserEntity> users = loadUsersFile();
		final UserDetails dbUser = loadUserByUsername(activeUser.getUsername(), users);
		if ( dbUser == null ) {
			throw new UsernameNotFoundException("User not found");
		}

		final UserEntity userFromUsersFile = users.remove(activeUser.getUsername());
		if ( userFromUsersFile != null ) {
			users.put(newUsername, userFromUsersFile.withUsername(newUsername));
		} else {
			final AtomicBoolean updatedUsername = new AtomicBoolean(false);
			final AtomicBoolean updatedRole = new AtomicBoolean(false);
			settingDao.batchProcess(new BatchCallback<Setting>() {

				@Override
				public BatchCallbackResult handle(Setting domainObject) {
					if ( domainObject.getType().equals(SETTING_TYPE_USER)
							&& domainObject.getKey().equals(dbUser.getUsername()) ) {
						updatedUsername.set(true);
						return (updatedRole.get() ? BatchCallbackResult.UPDATE_STOP
								: BatchCallbackResult.DELETE);
					} else if ( domainObject.getType().equals(SETTING_TYPE_ROLE)
							&& domainObject.getKey().equals(dbUser.getUsername()) ) {
						updatedRole.set(true);
						return (updatedUsername.get() ? BatchCallbackResult.UPDATE_STOP
								: BatchCallbackResult.DELETE);
					}
					return BatchCallbackResult.CONTINUE;
				}
			}, new BasicBatchOptions("UpdateUser", BasicBatchOptions.DEFAULT_BATCH_SIZE, true, null));
			if ( !updatedUsername.get() ) {
				// no username exists, treat as a legacy node whose password was "solar"
				UserProfile newProfile = new UserProfile();
				newProfile.setUsername(newUsername);
				newProfile.setPassword("solar");
				newProfile.setPasswordAgain("solar");
				storeUserProfile(newProfile, users);
			}
		}
		saveUsersFile(users);

		// update active user details to new username
		User newUser = new User(newUsername, "",
				Collections.singleton(new SimpleGrantedAuthority(GRANTED_AUTH_USER)));
		UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
				newUser, null, newUser.getAuthorities());
		SecurityContextHolder.getContext().setAuthentication(authentication);
	}

	@Override
	public void storeUserProfile(UserProfile profile) {
		storeUserProfile(profile, loadUsersFile());
	}

	private void storeUserProfile(UserProfile profile, Map<String, UserEntity> users) {
		if ( profile.getUsername() == null || profile.getPassword() == null
				|| !profile.getPassword().equals(profile.getPasswordAgain()) ) {
			throw new IllegalArgumentException(
					"Username, password, and repeated password must be provided.");
		}

		String password;
		if ( passwordEncoder != null ) {
			password = passwordEncoder.encode(profile.getPassword());
		} else {
			password = profile.getPassword();
		}
		settingDao.storeSetting(profile.getUsername(), SETTING_TYPE_USER, password);
		settingDao.storeSetting(profile.getUsername(), SETTING_TYPE_ROLE, GRANTED_AUTH_USER);
	}

	@Override
	public UserAuthenticationInfo authenticationInfo(String username) {
		UserDetails user = null;
		try {
			user = loadUserByUsername(username);
		} catch ( AuthenticationException e ) {
			return null;
		}
		if ( user == null ) {
			return null;
		}
		String pw = user.getPassword();
		Map<String, Object> params = new LinkedHashMap<String, Object>(2);
		String alg = hashAlgorithmFromPassword(pw, params);
		return new UserAuthenticationInfo(alg, params);
	}

	private static final Pattern BCRYPT_PAT = Pattern.compile("(\\$2[abxy]\\$\\d{2}\\$.{22}).+");

	private String hashAlgorithmFromPassword(String pw, Map<String, Object> params) {
		Matcher m = BCRYPT_PAT.matcher(pw);
		if ( m.matches() ) {
			params.put("salt", m.group(1));
			return "bcrypt";
		}
		return null;
	}

	/*-
	 * BackupResourceProvider implementation
	 */

	@Override
	public String getKey() {
		return "net.solarnetwork.node.setup.security.SettingsUserService";
	}

	@Override
	public Iterable<BackupResource> getBackupResources() {
		File file = new File(usersFilePath);
		if ( !(file.isFile() && file.canRead()) ) {
			return Collections.emptyList();
		}
		List<BackupResource> result = new ArrayList<>(1);
		result.add(new ResourceBackupResource(new FileSystemResource(file),
				BACKUP_RESOURCE_NAME_USERS_FILE, getKey()));
		return result;
	}

	@Override
	public boolean restoreBackupResource(BackupResource resource) {
		if ( resource != null
				&& BACKUP_RESOURCE_NAME_USERS_FILE.equalsIgnoreCase(resource.getBackupPath()) ) {
			final File destFile = new File(usersFilePath);
			final File destDir = destFile.getParentFile();
			if ( !destDir.isDirectory() ) {
				if ( !destDir.mkdirs() ) {
					log.warn("Error creating users database directory {}", destDir.getAbsolutePath());
					return false;
				}
			}
			synchronized ( this ) {
				File tmpFile = null;
				try {
					tmpFile = File.createTempFile(".users-", ".json", destDir);
					FileCopyUtils.copy(resource.getInputStream(), new FileOutputStream(tmpFile));
					tmpFile.setLastModified(resource.getModificationDate());
					if ( destFile.exists() ) {
						destFile.delete();
					}
					return tmpFile.renameTo(destFile);
				} catch ( IOException e ) {
					log.error("IO error restoring user database resource {}: {}",
							destFile.getAbsolutePath(), e.getMessage());
					return false;
				} finally {
					if ( tmpFile != null && tmpFile.exists() ) {
						tmpFile.delete();
					}
				}
			}
		}
		return false;
	}

	@Override
	public BackupResourceProviderInfo providerInfo(Locale locale) {
		String name = "User Database Provider";
		String desc = "Backs up the SolarNode user database.";
		MessageSource ms = messageSource;
		if ( ms != null ) {
			name = ms.getMessage("title", null, name, locale);
			desc = ms.getMessage("desc", null, desc, locale);
		}
		return new SimpleBackupResourceProviderInfo(getKey(), name, desc);
	}

	@Override
	public BackupResourceInfo resourceInfo(BackupResource resource, Locale locale) {
		String desc = "Node login user information.";
		MessageSource ms = messageSource;
		if ( ms != null ) {
			desc = ms.getMessage("users.desc", null, desc, locale);
		}
		return new SimpleBackupResourceInfo(resource.getProviderKey(), resource.getBackupPath(), desc);
	}

	/*-
	 * Accessors
	 */

	/**
	 * Get the configured {@link SettingDao}.
	 *
	 * @return The DAO.
	 */
	public SettingDao getSettingDao() {
		return settingDao;
	}

	/**
	 * Get the configured password encoder.
	 *
	 * @return The password encoder.
	 */
	public PasswordEncoder getPasswordEncoder() {
		return passwordEncoder;
	}

	/**
	 * Get the users file path.
	 *
	 * @return the users file path
	 * @since 2.2
	 */
	public String getUsersFilePath() {
		return usersFilePath;
	}

	/**
	 * Set the users file path.
	 *
	 * @param usersFilePath
	 *        the users file path to set
	 * @since 2.2
	 */
	public void setUsersFilePath(String usersFilePath) {
		this.usersFilePath = (usersFilePath == null || usersFilePath.isEmpty() ? DEFAULT_USERS_FILE_PATH
				: usersFilePath);
	}

	/**
	 * Set a message source for backup localized messages.
	 *
	 * @param messageSource
	 *        the message source
	 * @since 2.2
	 */
	public void setMessageSource(MessageSource messageSource) {
		this.messageSource = messageSource;
	}

}
