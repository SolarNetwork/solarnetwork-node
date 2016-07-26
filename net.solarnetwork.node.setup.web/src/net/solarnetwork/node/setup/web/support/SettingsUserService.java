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

package net.solarnetwork.node.setup.web.support;

import java.util.Collection;
import java.util.Collections;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import net.solarnetwork.node.IdentityService;
import net.solarnetwork.node.dao.SettingDao;

/**
 * {@link UserDetailsService} that uses {@link SettingDao} for users and roles.
 * 
 * @author matt
 * @version 1.0
 */
public class SettingsUserService implements UserDetailsService {

	public static final String SETTING_TYPE_USER = "solarnode.user";
	public static final String SETTING_TYPE_ROLE = "solarnode.role";

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
		if ( password == null && identityService != null && passwordEncoder != null ) {
			// for backwards-compat with nodes created before user auth, provide a default
			Long nodeId = identityService.getNodeId();
			if ( nodeId != null && nodeId.toString().equalsIgnoreCase(username) ) {
				password = passwordEncoder.encode("solar");
				GrantedAuthority auth = new SimpleGrantedAuthority("ROLE_USER");
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
		return result;
	}

}
