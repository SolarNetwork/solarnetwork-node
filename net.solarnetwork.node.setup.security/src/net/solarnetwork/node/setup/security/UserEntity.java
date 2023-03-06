/* ==================================================================
 * UserEntity.java - 6/03/2023 12:31:58 pm
 * 
 * Copyright 2023 SolarNetwork.net Dev Team
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

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * A basic user entity.
 * 
 * @author matt
 * @version 1.0
 * @since 2.1
 */
public final class UserEntity {

	private final long created;
	private final long modified;
	private final String username;
	private final String password;
	private final Set<String> roles;

	/**
	 * Constructor.
	 * 
	 * @param created
	 *        the creation date
	 * @param modified
	 *        the modification date
	 * @param username
	 *        the username
	 * @param password
	 *        the password
	 * @param roles
	 *        the roles
	 */
	@JsonCreator
	public UserEntity(@JsonProperty("created") long created, @JsonProperty("modified") long modified,
			@JsonProperty("username") String username, @JsonProperty("password") String password,
			@JsonProperty("roles") Set<String> roles) {
		super();
		this.created = created;
		this.modified = modified;
		this.username = username;
		this.password = password;
		this.roles = (roles != null ? Collections.unmodifiableSet(new LinkedHashSet<>(roles))
				: Collections.emptySet());
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("UserEntity{created=");
		builder.append(created);
		builder.append(", modified=");
		builder.append(modified);
		builder.append(", ");
		if ( username != null ) {
			builder.append("username=");
			builder.append(username);
			builder.append(", ");
		}
		if ( password != null ) {
			builder.append("password=*****,");
		}
		if ( roles != null ) {
			builder.append("roles=");
			builder.append(roles);
		}
		builder.append("}");
		return builder.toString();
	}

	@Override
	public int hashCode() {
		return Objects.hash(username);
	}

	@Override
	public boolean equals(Object obj) {
		if ( this == obj ) {
			return true;
		}
		if ( !(obj instanceof UserEntity) ) {
			return false;
		}
		UserEntity other = (UserEntity) obj;
		return Objects.equals(username, other.username);
	}

	/**
	 * Create a copy with a new username.
	 * 
	 * @param username
	 *        the username to assign
	 * @return the new instance
	 */
	public UserEntity withUsername(String username) {
		return new UserEntity(created, System.currentTimeMillis(), username, password, roles);
	}

	/**
	 * Create a copy with a new password.
	 * 
	 * @param password
	 *        the password to assign
	 * @return the new instance
	 */
	public UserEntity withPassword(String password) {
		return new UserEntity(created, System.currentTimeMillis(), username, password, roles);
	}

	/**
	 * Get the creation date.
	 * 
	 * @return the created date, as a millisecond epoch
	 */
	public long getCreated() {
		return created;
	}

	/**
	 * Get the last modification date.
	 * 
	 * @return the modified date, as a millisecond epoch
	 */
	public long getModified() {
		return modified;
	}

	/**
	 * Get the username.
	 * 
	 * @return the username
	 */
	public String getUsername() {
		return username;
	}

	/**
	 * Get the password.
	 * 
	 * @return the password
	 */
	public String getPassword() {
		return password;
	}

	/**
	 * Get the roles.
	 * 
	 * @return the roles
	 */
	public Set<String> getRoles() {
		return roles;
	}

}
