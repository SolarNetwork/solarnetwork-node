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

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import net.solarnetwork.node.dao.BasicBatchOptions;
import net.solarnetwork.node.dao.BatchableDao.BatchCallback;
import net.solarnetwork.node.dao.BatchableDao.BatchCallbackResult;
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
 * @version 2.0
 */
public class SettingsUserService implements UserService, UserDetailsService {

	public static final String SETTING_TYPE_USER = "solarnode.user";
	public static final String SETTING_TYPE_ROLE = "solarnode.role";
	public static final String GRANTED_AUTH_USER = "ROLE_USER";

	private final SettingDao settingDao;
	private final IdentityService identityService;
	private final PasswordEncoder passwordEncoder;

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
	}

	@Override
	public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
		UserDetails result = null;
		String password = settingDao.getSetting(username, SETTING_TYPE_USER);
		if ( password == null && identityService != null && passwordEncoder != null
				&& !someUserExists() ) {
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
		if ( result == null ) {
			throw new UsernameNotFoundException(username);
		}
		return result;
	}

	@Override
	public boolean someUserExists() {
		final AtomicBoolean result = new AtomicBoolean(false);
		settingDao.batchProcess(new BatchCallback<Setting>() {

			@Override
			public BatchCallbackResult handle(Setting domainObject) {
				if ( domainObject.getType().equals(SETTING_TYPE_USER) ) {
					result.set(true);
					return BatchCallbackResult.STOP;
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
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		UserDetails activeUser = (auth == null ? null : (UserDetails) auth.getPrincipal());
		if ( activeUser == null ) {
			throw new InsufficientAuthenticationException("Active user not found.");
		}
		UserDetails dbUser = loadUserByUsername(activeUser.getUsername());
		if ( dbUser == null ) {
			throw new UsernameNotFoundException("User not found");
		}
		if ( passwordEncoder != null ) {
			if ( !passwordEncoder.matches(existingPassword, dbUser.getPassword()) ) {
				throw new BadCredentialsException("Existing password does not match.");
			}
		} else if ( !existingPassword.equals(dbUser.getPassword()) ) {
			throw new BadCredentialsException("Existing password does not match.");
		}
		if ( newPassword == null || newPasswordAgain == null || !newPassword.equals(newPasswordAgain) ) {
			throw new IllegalArgumentException(
					"New password not provided or does not match repeated password.");
		}
		String password;
		if ( passwordEncoder != null ) {
			password = passwordEncoder.encode(newPassword);
		} else {
			password = newPassword;
		}
		settingDao.storeSetting(dbUser.getUsername(), SETTING_TYPE_USER, password);
		settingDao.storeSetting(dbUser.getUsername(), SETTING_TYPE_ROLE, GRANTED_AUTH_USER);
	}

	@Override
	public void changeUsername(final String newUsername, final String newUsernameAgain) {
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		UserDetails activeUser = (auth == null ? null : (UserDetails) auth.getPrincipal());
		if ( activeUser == null ) {
			throw new InsufficientAuthenticationException("Active user not found.");
		}
		final UserDetails dbUser = loadUserByUsername(activeUser.getUsername());
		if ( dbUser == null ) {
			throw new UsernameNotFoundException("User not found");
		}
		if ( newUsername == null || newUsernameAgain == null || !newUsername.equals(newUsernameAgain) ) {
			throw new IllegalArgumentException(
					"New username not provided or does not match repeated username.");
		}
		final AtomicBoolean updatedUsername = new AtomicBoolean(false);
		final AtomicBoolean updatedRole = new AtomicBoolean(false);
		settingDao.batchProcess(new BatchCallback<Setting>() {

			@Override
			public BatchCallbackResult handle(Setting domainObject) {
				if ( domainObject.getType().equals(SETTING_TYPE_USER)
						&& domainObject.getKey().equals(dbUser.getUsername()) ) {
					updatedUsername.set(true);
					domainObject.setKey(newUsername);
					return (updatedRole.get() ? BatchCallbackResult.UPDATE_STOP
							: BatchCallbackResult.UPDATE);
				} else if ( domainObject.getType().equals(SETTING_TYPE_ROLE)
						&& domainObject.getKey().equals(dbUser.getUsername()) ) {
					updatedRole.set(true);
					domainObject.setKey(newUsername);
					return (updatedUsername.get() ? BatchCallbackResult.UPDATE_STOP
							: BatchCallbackResult.UPDATE);
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
			storeUserProfile(newProfile);
		}

		// update active user details to new usenrame
		User newUser = new User(newUsername, "",
				Collections.singleton(new SimpleGrantedAuthority(GRANTED_AUTH_USER)));
		UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
				newUser, null, newUser.getAuthorities());
		SecurityContextHolder.getContext().setAuthentication(authentication);
	}

	@Override
	public void storeUserProfile(UserProfile profile) {
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
		UserDetails user = loadUserByUsername(username);
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

}
