/* ==================================================================
 * DefaultSecurityTokenService.java - 7/09/2023 6:18:46 am
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

package net.solarnetwork.node.runtime;

import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Collection;
import net.solarnetwork.domain.KeyValuePair;
import net.solarnetwork.node.dao.SecurityTokenDao;
import net.solarnetwork.node.domain.SecurityToken;
import net.solarnetwork.node.service.SecurityTokenService;
import net.solarnetwork.node.service.support.BaseIdentifiable;
import net.solarnetwork.service.OptionalService;

/**
 * Default implementation of {@link SecurityTokenService}
 * 
 * @author matt
 * @version 1.0
 * @since 3.4
 */
public class DefaultSecurityTokenService extends BaseIdentifiable implements SecurityTokenService {

	private static final int TOKEN_ID_LEN = 20;
	private static final int TOKEN_SEC_LEN = 32;

	private static final char[] TOKEN_ALPHABET = new char[] { '0', '1', '2', '3', '4', '5', '6', '7',
			'8', '9', 'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M', 'N', 'O', 'P',
			'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z', 'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h',
			'i', 'j', 'k', 'l', 'm', 'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z',
			'-', '.', '_', };

	private final SecureRandom rng;
	private final OptionalService<SecurityTokenDao> securityTokenDao;

	/**
	 * Constructor.
	 * 
	 * @param securityTokenDao
	 *        the DAO to use
	 * @throws IllegalArgumentException
	 *         if any argument is {@literal null}
	 */
	public DefaultSecurityTokenService(OptionalService<SecurityTokenDao> securityTokenDao) {
		this(defaultRng(), securityTokenDao);
	}

	/**
	 * Constructor.
	 * 
	 * @param rng
	 *        the random number generator
	 * @param securityTokenDao
	 *        the DAO to use
	 * @throws IllegalArgumentException
	 *         if any argument is {@literal null}
	 */
	public DefaultSecurityTokenService(SecureRandom rng,
			OptionalService<SecurityTokenDao> securityTokenDao) {
		super();
		this.rng = requireNonNullArgument(rng, "rng");
		this.securityTokenDao = requireNonNullArgument(securityTokenDao, "securityTokenDao");
	}

	private static SecureRandom defaultRng() {
		try {
			return SecureRandom.getInstanceStrong();
		} catch ( NoSuchAlgorithmException e ) {
			return new SecureRandom();
		}
	}

	private SecurityTokenDao dao() {
		SecurityTokenDao dao = OptionalService.service(securityTokenDao);
		if ( dao != null ) {
			return dao;
		}
		throw new UnsupportedOperationException("SecurityTokenDao not available.");
	}

	@Override
	public Collection<SecurityToken> getAvailableTokens() {
		return dao().getAll(null);
	}

	@Override
	public SecurityToken tokenForId(String tokenId) {
		return dao().get(requireNonNullArgument(tokenId, "tokenId"));
	}

	@Override
	public KeyValuePair createToken(SecurityToken details) {
		final SecurityTokenDao dao = dao();
		final String tokenId = generateRandomToken(TOKEN_ID_LEN);
		final String tokenSecret = generateRandomToken(TOKEN_SEC_LEN);
		final SecurityToken token = new SecurityToken(tokenId, Instant.now(), tokenSecret,
				details != null ? details.getName() : null,
				details != null ? details.getDescription() : null);
		dao.save(token);
		return new KeyValuePair(tokenId, tokenSecret);
	}

	private String generateRandomToken(final int length) {
		char[] data = new char[length];
		for ( int i = 0; i < length; i++ ) {
			data[i] = TOKEN_ALPHABET[rng.nextInt(TOKEN_ALPHABET.length)];
		}
		return new String(data);
	}

	@Override
	public void updateToken(SecurityToken token) {
		requireNonNullArgument(token, "token");

		// ensure a token secret is not provided; we cannot insert tokens via this method
		String[] holder = new String[1];
		token.copySecret(s -> holder[0] = s);
		if ( holder[0] != null ) {
			throw new IllegalArgumentException("A token secret cannot be provided.");
		}

		dao().save(token);
	}

	@Override
	public void deleteToken(String tokenId) {
		requireNonNullArgument(tokenId, "tokenId");
		SecurityToken token = SecurityToken.tokenDetails(tokenId, null, null);
		dao().delete(token);
	}

}
