/* ==================================================================
 * DefaultKeystoreService.java - Dec 5, 2012 9:10:53 AM
 * 
 * Copyright 2007-2012 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.setup.impl;

import static net.solarnetwork.node.SetupSettings.KEY_CONFIRMATION_CODE;
import static net.solarnetwork.node.SetupSettings.SETUP_TYPE_KEY;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import javax.annotation.Resource;
import net.solarnetwork.node.dao.SettingDao;
import net.solarnetwork.node.setup.SetupException;
import net.solarnetwork.support.CertificateService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Service for managing a {@link KeyStore}.
 * 
 * @author matt
 * @version 1.0
 */
public class DefaultKeystoreService {

	public static final String DEFAULT_KEY_STORE_PATH = "conf/pki/node.jks";

	private String keyStorePath = DEFAULT_KEY_STORE_PATH;
	private String nodeAlias = "node";
	private String caAlias = "ca";
	private int keySize = 2048;

	@Resource
	private CertificateService certificateService;

	@Resource
	private SettingDao settingDao;

	private final Logger log = LoggerFactory.getLogger(getClass());

	/**
	 * Check if the node's certificate is valid.
	 * 
	 * <p>
	 * The certificate is considered valid if it is not self-signed and its
	 * chain can be verified and it has not expired.
	 * </p>
	 * 
	 * @return boolean
	 */
	public boolean isNodeCertificateValid() {
		KeyStore keyStore = loadKeyStore();
		try {
			// TODO: need to validate actual certificate, and not self-signed
			return keyStore != null && keyStore.containsAlias(nodeAlias);
		} catch ( KeyStoreException e ) {
			throw new SetupException("Error checking for node certificate", e);
		}
	}

	/**
	 * Generate a new public and private key pair, and a new self-signed
	 * certificate.
	 * 
	 * @param dn
	 *        the certificate subject DN
	 * @return the Certificate
	 */
	public Certificate generateSelfSignedCertificate(String dn) {
		KeyStore keyStore = loadKeyStore();
		return setupNodeAlias(keyStore, dn);
	}

	private void saveKeyStore(KeyStore keyStore) {
		if ( keyStore == null ) {
			return;
		}
		File ksFile = new File(keyStorePath);
		File ksDir = ksFile.getParentFile();
		if ( !ksDir.isDirectory() && !ksDir.mkdirs() ) {
			throw new RuntimeException("Unable to create KeyStore directory: " + ksFile.getParent());
		}
		OutputStream out = null;
		try {
			String passwd = getSetting(KEY_CONFIRMATION_CODE);
			out = new BufferedOutputStream(new FileOutputStream(ksFile));
			keyStore.store(out, passwd.toCharArray());
		} catch ( KeyStoreException e ) {
			throw new SetupException("Error creating certificate key store", e);
		} catch ( NoSuchAlgorithmException e ) {
			throw new SetupException("Error creating certificate key store", e);
		} catch ( CertificateException e ) {
			throw new SetupException("Error creating certificate key store", e);
		} catch ( IOException e ) {
			throw new SetupException("Error creating certificate key store", e);
		} finally {
			if ( out != null ) {
				try {
					out.flush();
					out.close();
				} catch ( IOException e ) {
					throw new SetupException("Error closing KeyStore file: " + ksFile.getPath(), e);
				}
			}
		}
	}

	private Certificate setupNodeAlias(KeyStore keyStore, String dn) {
		try {
			// create new key pair for the node
			KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
			keyGen.initialize(keySize, new SecureRandom());
			KeyPair keypair = keyGen.generateKeyPair();
			PublicKey publicKey = keypair.getPublic();
			PrivateKey privateKey = keypair.getPrivate();

			Certificate cert = certificateService.generateCertificate(dn, publicKey, privateKey);
			keyStore.setKeyEntry(nodeAlias, privateKey, new char[0], new Certificate[] { cert });
			saveKeyStore(keyStore);
			return cert;
		} catch ( NoSuchAlgorithmException e ) {
			throw new SetupException("Error setting up node key pair", e);
		} catch ( KeyStoreException e ) {
			throw new SetupException("Error setting up node key pair", e);
		}
	}

	private KeyStore loadKeyStore() {
		File ksFile = new File(keyStorePath);
		InputStream in = null;
		KeyStore keyStore = null;
		String passwd = getSetting(KEY_CONFIRMATION_CODE);
		if ( passwd == null ) {
			log.info("Network association confirmation not available, cannot open key store");
			return null;
		}
		try {
			keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
			if ( ksFile.isFile() ) {
				in = new BufferedInputStream(new FileInputStream(ksFile));
			}
			keyStore.load(in, passwd.toCharArray());
			return keyStore;
		} catch ( KeyStoreException e ) {
			throw new SetupException("Error creating certificate key store", e);
		} catch ( NoSuchAlgorithmException e ) {
			throw new SetupException("Error creating certificate key store", e);
		} catch ( CertificateException e ) {
			throw new SetupException("Error creating certificate key store", e);
		} catch ( IOException e ) {
			throw new SetupException("Error creating certificate key store", e);
		} finally {
			if ( in != null ) {
				try {
					in.close();
				} catch ( IOException e ) {
					log.warn("Error closing key store file {}: {}", ksFile.getPath(), e.getMessage());
				}
			}
		}
	}

	private String getSetting(String key) {
		return settingDao.getSetting(key, SETUP_TYPE_KEY);
	}

	private void saveSetting(String key, String value) {
		settingDao.storeSetting(key, SETUP_TYPE_KEY, value);
	}

	public void setKeyStorePath(String keyStorePath) {
		this.keyStorePath = keyStorePath;
	}

	public void setSettingDao(SettingDao settingDao) {
		this.settingDao = settingDao;
	}

	public void setNodeAlias(String nodeAlias) {
		this.nodeAlias = nodeAlias;
	}

	public void setCaAlias(String caAlias) {
		this.caAlias = caAlias;
	}

	public void setKeySize(int keySize) {
		this.keySize = keySize;
	}

	public void setCertificateService(CertificateService certificateService) {
		this.certificateService = certificateService;
	}

}
