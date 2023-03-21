/* ==================================================================
 * LoginKeyHelper.java - 30/10/2019 9:39:39 am
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

package net.solarnetwork.node.setup.web.support;

import static org.apache.commons.codec.binary.Base64.decodeBase64;
import static org.apache.commons.codec.binary.Base64.encodeBase64String;
import java.nio.charset.Charset;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import javax.crypto.Cipher;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.digest.HmacAlgorithms;
import org.apache.commons.codec.digest.HmacUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

/**
 * Helper class for performing login key-based logins.
 * 
 * @author matt
 * @version 1.1
 * @since 1.41
 * @see LoginKeyAuthenticationFilter
 */
public class LoginKeyHelper {

	private final AtomicReference<Secret> SECRET = new AtomicReference<>();

	private static final int AES_IV_LENGTH = 16;
	private static final int SALT_LENGTH = 12;
	private static final int SECRET_DATA_LENGTH = 32;
	private static final long SECRET_EXPIRATION_MS = TimeUnit.MINUTES.toMillis(5L);
	private static final HmacAlgorithms HMAC_ALG = HmacAlgorithms.HMAC_SHA_256;
	private static final SecureRandom RNG = new SecureRandom();
	private static final Charset UTF8 = Charset.forName("UTF-8");

	private static final Logger log = LoggerFactory.getLogger(LoginKeyHelper.class);

	private static final class Secret {

		private final Secret previousSecret;
		private final byte[] iv;
		private final byte[] data;
		private final long expireDate;

		private Secret(Secret previousSecret) {
			super();
			this.previousSecret = previousSecret;
			this.data = new byte[SECRET_DATA_LENGTH];
			RNG.nextBytes(data);
			this.iv = new byte[AES_IV_LENGTH];
			RNG.nextBytes(iv);
			this.expireDate = System.currentTimeMillis() + SECRET_EXPIRATION_MS;
		}

	}

	/**
	 * Default constructor.
	 */
	public LoginKeyHelper() {
		super();
	}

	private Secret secret() {
		return SECRET.updateAndGet(curr -> {
			if ( curr == null || curr.expireDate < System.currentTimeMillis() ) {
				return new Secret(curr);
			}
			return curr;
		});
	}

	/**
	 * Generate a random value for a username to use for digesting the user's
	 * password externally and passing the key back to the
	 * {@link #decrypt(String, String)} method.
	 * 
	 * <p>
	 * The returned encryption data expires after 5 minutes. A new key must be
	 * generated after that time in order to successfully call the
	 * {@link #decrypt(String, String)} method again after it expires.
	 * </p>
	 * 
	 * @param username
	 *        the username to generate a login encryption key for
	 * @param salt
	 *        Base64 encoded salt, must be at least 12 bytes long (decoded)
	 * @return the encryption key
	 */
	public LoginKey generateKey(String username, String salt) {
		log.debug("Generating login key for [{}] using salt [{}]", username, salt);
		byte[] saltBytes = (salt != null ? Base64.decodeBase64(salt) : null);
		if ( saltBytes == null || saltBytes.length != SALT_LENGTH ) {
			throw new IllegalArgumentException("Salt must be exactly " + SALT_LENGTH + " bytes.");
		}
		byte[] usernameBytes = username.getBytes(UTF8);
		byte[] saltyUsernameBytes = new byte[saltBytes.length + usernameBytes.length];
		System.arraycopy(saltBytes, 0, saltyUsernameBytes, 0, saltBytes.length);
		System.arraycopy(usernameBytes, 0, saltyUsernameBytes, saltBytes.length, usernameBytes.length);

		Secret secret = secret();
		byte[] key = new HmacUtils(HMAC_ALG, secret.data).hmac(saltyUsernameBytes);

		LoginKey result = new LoginKey(encodeBase64String(secret.iv), encodeBase64String(key));
		if ( log.isDebugEnabled() ) {
			log.debug("Generated login key for [{}] using salt [{}]: secret = [{}], key = [{}]",
					username, salt, encodeBase64String(secret.data), result.getKey());
		}
		return result;
	}

	private Cipher createCipher(boolean encrypt, byte[] iv, byte[] key) throws InvalidKeyException,
			InvalidAlgorithmParameterException, NoSuchAlgorithmException, NoSuchPaddingException {
		Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
		SecretKey k = new SecretKeySpec(key, "AES");
		IvParameterSpec ivSpec = new IvParameterSpec(iv);
		cipher.init(encrypt ? Cipher.ENCRYPT_MODE : Cipher.DECRYPT_MODE, k, ivSpec);
		return cipher;
	}

	/**
	 * Decrypt a Base64-encoded string that has been encrypted using the key
	 * returned from {@link #generateKey(String, String)}.
	 * 
	 * @param saltyUsername
	 *        the Base64 encoded salt + username
	 * @param cipherText
	 *        the Base64-encoded AES-CBC encrypted data
	 * @return the decrypted username and password value
	 * @throws org.springframework.security.core.userdetails.UsernameNotFoundException
	 *         if {@code username} is not known
	 */
	public UsernamePasswordAuthenticationToken decrypt(String saltyUsername, String cipherText) {
		byte[] saltyUsernameBytes = Base64.decodeBase64(saltyUsername);
		if ( !(saltyUsernameBytes.length > SALT_LENGTH) ) {
			throw new BadCredentialsException("Salted username not long enough.");
		}
		byte[] usernameBytes = new byte[saltyUsernameBytes.length - SALT_LENGTH];
		System.arraycopy(saltyUsernameBytes, SALT_LENGTH, usernameBytes, 0, usernameBytes.length);
		String username = new String(usernameBytes, UTF8);

		Secret secret = secret();
		try {
			return decrypt(secret, saltyUsernameBytes, username, cipherText);
		} catch ( BadCredentialsException e ) {
			// try again with the previous secret, if available
			if ( secret.previousSecret != null ) {
				return decrypt(secret.previousSecret, saltyUsernameBytes, username, cipherText);
			}
			throw e;
		}
	}

	private UsernamePasswordAuthenticationToken decrypt(Secret secret, byte[] usernameBytes,
			String username, String cipherText) {
		byte[] key = new HmacUtils(HMAC_ALG, secret.data).hmac(usernameBytes);
		if ( log.isDebugEnabled() ) {
			log.debug("Decrypting login password [{}] for username [{}] with key = [{}], secret = [{}]",
					cipherText, username, encodeBase64String(key), encodeBase64String(secret.data));
		}
		try {
			Cipher cipher = createCipher(false, secret.iv, key);
			byte[] passwordBytes = cipher.doFinal(decodeBase64(cipherText));
			UsernamePasswordAuthenticationToken result = new UsernamePasswordAuthenticationToken(
					username, new String(passwordBytes, UTF8));
			if ( log.isDebugEnabled() ) {
				log.debug(
						"Decrypted login password [{}] successfully for username [{}] with key = [{}], secret = [{}]",
						cipherText, username, encodeBase64String(key), encodeBase64String(secret.data));
			}
			return result;
		} catch ( Exception e ) {
			log.info("Login password decryption error for {}: {}", username, e.toString());
			throw new BadCredentialsException("Invalid password.");
		}
	}

}
