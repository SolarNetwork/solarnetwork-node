/* ==================================================================
 * SecurityToken.java - 6/09/2023 2:41:36 pm
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

package net.solarnetwork.node.domain;

import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.time.Instant;
import java.util.Date;
import java.util.function.Consumer;
import java.util.function.Function;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import net.solarnetwork.dao.BasicStringEntity;
import net.solarnetwork.security.AbstractAuthorizationBuilder;

/**
 * A security token, for API access.
 * 
 * <p>
 * The {@link #getId()} value is the security token identifier.
 * </p>
 * 
 * @author matt
 * @version 1.0
 * @since 3.4
 */
@JsonIgnoreProperties({ "createdDate" })
@JsonPropertyOrder({ "tokenId", "created", "name", "description" })
public class SecurityToken extends BasicStringEntity {

	private static final long serialVersionUID = -440817156290937870L;

	/** The token secret. */
	private final String tokenSecret;

	/** The name. */
	private final String name;

	/** The description. */
	private final String description;

	/**
	 * Constructor.
	 * 
	 * @param tokenId
	 *        the token identifier
	 * @param tokenSecret
	 *        the token secret
	 * @throws IllegalArgumentException
	 *         if any argument is {@literal null}
	 */
	public SecurityToken(String tokenId, String tokenSecret) {
		this(tokenId, null, tokenSecret, null, null);
	}

	/**
	 * Constructor.
	 * 
	 * @param tokenId
	 *        the token identifier
	 * @param created
	 *        the creation date
	 * @param tokenSecret
	 *        the token secret
	 * @param name
	 *        the name
	 * @param description
	 *        the description
	 * @throws IllegalArgumentException
	 *         if {@code tokenId} or {@code tokenSecret} are {@literal null}
	 */
	public SecurityToken(String tokenId, Instant created, String tokenSecret, String name,
			String description) {
		super(requireNonNullArgument(tokenId, "tokenId"), created);
		this.tokenSecret = requireNonNullArgument(tokenSecret, "tokenSecret");
		this.name = name;
		this.description = description;
	}

	/**
	 * Create a token from just the detail properties.
	 * 
	 * @param tokenId
	 *        the token ID
	 * @param name
	 *        the name
	 * @param description
	 *        the description
	 * @return the token instance
	 */
	public static SecurityToken tokenDetails(String tokenId, String name, String description) {
		return new SecurityToken(tokenId, null, name, description);
	}

	/**
	 * Create a token from just the detail properties.
	 * 
	 * @param name
	 *        the name
	 * @param description
	 *        the description
	 * @return the token instance
	 */
	public static SecurityToken tokenDetails(String name, String description) {
		return new SecurityToken(null, name, description);
	}

	private SecurityToken(String tokenId, Instant created, String name, String description) {
		super(requireNonNullArgument(tokenId, "tokenId"), created);
		this.tokenSecret = null;
		this.name = name;
		this.description = description;
	}

	private SecurityToken(String tokenId, String name, String description) {
		super(tokenId, null);
		this.tokenSecret = null;
		this.name = name;
		this.description = description;
	}

	/**
	 * Create a copy without the token secret populated.
	 * 
	 * @param newName
	 *        if non-{@literal null} then use for the {@code name} in the new
	 *        copy
	 * @param newDescription
	 *        if non-{@literal null} then use for the {@code description} in the
	 *        new copy
	 * @return the copy
	 */
	public SecurityToken copyWithoutSecret(String newName, String newDescription) {
		return new SecurityToken(getId(), getCreated(), newName != null ? newName : name,
				newDescription != null ? newDescription : description);
	}

	/**
	 * Save the signing key from the token secret.
	 * 
	 * @param <T>
	 *        the builder type
	 * @param builder
	 *        the authorization builder to call
	 *        {@link AbstractAuthorizationBuilder#saveSigningKey(String)} on
	 * @return the {@code builder} instance
	 * @throws IllegalArgumentException
	 *         if any argument is {@literal null}
	 * @throws net.solarnetwork.security.SecurityException
	 *         if any error occurs computing the key
	 */
	public <T extends AbstractAuthorizationBuilder<T>> T saveSigningKey(
			AbstractAuthorizationBuilder<T> builder) {
		return requireNonNullArgument(builder, "builder").saveSigningKey(tokenSecret);
	}

	/**
	 * Copy the secret to a consumer.
	 * 
	 * @param dest
	 *        the consumer
	 */
	public void copySecret(Consumer<String> dest) {
		dest.accept(tokenSecret);
	}

	/**
	 * Apply the secret to a function.
	 * 
	 * @param <T>
	 *        the function return type
	 * @param dest
	 *        the function to apply
	 * @return the function result
	 */
	public <T> T applySecret(Function<String, T> dest) {
		return dest.apply(tokenSecret);
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("SecurityToken{");
		builder.append("id=");
		builder.append(getId());
		if ( name != null ) {
			builder.append(", name=");
			builder.append(name);
		}
		builder.append("}");
		return builder.toString();
	}

	/**
	 * Get the created date as a {@link Date} instance.
	 * 
	 * @return the date
	 */
	public Date getCreatedDate() {
		Instant ts = getCreated();
		return (ts != null ? Date.from(ts) : null);
	}

	/**
	 * Get the name.
	 * 
	 * @return the name
	 */
	public String getName() {
		return name;
	}

	/**
	 * Get the description.
	 * 
	 * @return the description
	 */
	public String getDescription() {
		return description;
	}

}
