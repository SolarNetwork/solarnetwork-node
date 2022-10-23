/* ==================================================================
 * AssociateNodeCommand.java - Sep 6, 2011 2:52:00 PM
 * 
 * Copyright 2007-2011 SolarNetwork.net Dev Team
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

/**
 * A command object for associating a node with a SolarNet account.
 * 
 * @author maxieduncan
 * @version 1.1
 */
public class AssociateNodeCommand {

	private String verificationCode;
	private String keystorePassword;

	/**
	 * Get the verification code.
	 * 
	 * @return the code
	 */
	public String getVerificationCode() {
		return verificationCode;
	}

	/**
	 * Set the verification code.
	 * 
	 * @param associationCode
	 *        the code to set
	 */
	public void setVerificationCode(String associationCode) {
		this.verificationCode = associationCode;
	}

	/**
	 * Get the keystore password.
	 * 
	 * @return the password
	 */
	public String getKeystorePassword() {
		return keystorePassword;
	}

	/**
	 * Set the keystore password.
	 * 
	 * @param keystorePassword
	 *        the password to set
	 */
	public void setKeystorePassword(String keystorePassword) {
		this.keystorePassword = keystorePassword;
	}

}
