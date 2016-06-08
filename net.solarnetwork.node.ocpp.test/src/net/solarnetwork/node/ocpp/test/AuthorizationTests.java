/* ==================================================================
 * AuthorizationTests.java - 9/06/2015 6:39:33 am
 * 
 * Copyright 2007-2015 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.ocpp.test;

import java.util.GregorianCalendar;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import net.solarnetwork.node.ocpp.Authorization;
import net.solarnetwork.node.test.AbstractNodeTest;
import ocpp.v15.cs.AuthorizationStatus;
import ocpp.v15.cs.IdTagInfo;
import org.junit.Assert;
import org.junit.Test;

/**
 * Test cases for the {@link Authorization} class.
 * 
 * @author matt
 * @version 1.0
 */
public class AuthorizationTests extends AbstractNodeTest {

	private static final String TEST_TAG = "test.tag";
	private static final String TEST_PARENT_TAG = "test.parent.tag";

	private XMLGregorianCalendar newXmlCalendar() {
		DatatypeFactory factory;
		try {
			factory = DatatypeFactory.newInstance();
		} catch ( DatatypeConfigurationException e ) {
			throw new RuntimeException(e);
		}
		GregorianCalendar cal = new GregorianCalendar();
		return factory.newXMLGregorianCalendar(cal);
	}

	@Test
	public void constructFromValuesJustStatus() {
		IdTagInfo info = new IdTagInfo();
		info.setStatus(AuthorizationStatus.ACCEPTED);
		Authorization auth = new Authorization(TEST_TAG, info);
		Assert.assertEquals("IdTag", TEST_TAG, auth.getIdTag());
		Assert.assertEquals("Status", AuthorizationStatus.ACCEPTED, auth.getStatus());
		Assert.assertNull("ExpiryDate", auth.getExpiryDate());
		Assert.assertNull("ParentIdTag", auth.getParentIdTag());

		info.setParentIdTag(TEST_PARENT_TAG);
		XMLGregorianCalendar xmlCal = newXmlCalendar();
		info.setExpiryDate(xmlCal);
		auth = new Authorization(TEST_TAG, info);
		Assert.assertEquals("IdTag", TEST_TAG, auth.getIdTag());
		Assert.assertEquals("Status", AuthorizationStatus.ACCEPTED, auth.getStatus());
		Assert.assertEquals("ExpiryDate", xmlCal, auth.getExpiryDate());
		Assert.assertEquals("ParentIdTag", TEST_PARENT_TAG, auth.getParentIdTag());
	}

	@Test
	public void constructFromValues() {
		IdTagInfo info = new IdTagInfo();
		info.setStatus(AuthorizationStatus.ACCEPTED);
		info.setParentIdTag(TEST_PARENT_TAG);
		XMLGregorianCalendar xmlCal = newXmlCalendar();
		info.setExpiryDate(xmlCal);
		Authorization auth = new Authorization(TEST_TAG, info);
		Assert.assertEquals("IdTag", TEST_TAG, auth.getIdTag());
		Assert.assertEquals("Status", AuthorizationStatus.ACCEPTED, auth.getStatus());
		Assert.assertEquals("ExpiryDate", xmlCal, auth.getExpiryDate());
		Assert.assertEquals("ParentIdTag", TEST_PARENT_TAG, auth.getParentIdTag());
	}

	@Test
	public void expiredNoExpiryDate() {
		Authorization auth = new Authorization();
		boolean expired = auth.isExpired();
		Assert.assertFalse("Expired", expired);
	}

	@Test
	public void expiredExpiryDateInFuture() {
		Authorization auth = new Authorization();
		XMLGregorianCalendar xmlCal = newXmlCalendar();
		xmlCal.setYear(xmlCal.getYear() + 1);
		auth.setExpiryDate(xmlCal);
		boolean expired = auth.isExpired();
		Assert.assertFalse("Expired", expired);
	}

	@Test
	public void expiredExpiryDateInPast() {
		Authorization auth = new Authorization();
		XMLGregorianCalendar xmlCal = newXmlCalendar();
		xmlCal.setYear(xmlCal.getYear() - 1);
		auth.setExpiryDate(xmlCal);
		boolean expired = auth.isExpired();
		Assert.assertTrue("Expired", expired);
	}

	@Test
	public void acceptedNoExpiryDate() {
		Authorization auth = new Authorization();
		for ( AuthorizationStatus status : AuthorizationStatus.values() ) {
			auth.setStatus(status);
			boolean accepted = auth.isAccepted();
			Assert.assertEquals(status.toString(), AuthorizationStatus.ACCEPTED.equals(status), accepted);
		}
	}

	@Test
	public void acceptedExpiryDateInFuture() {
		Authorization auth = new Authorization();
		XMLGregorianCalendar xmlCal = newXmlCalendar();
		xmlCal.setYear(xmlCal.getYear() + 1);
		auth.setExpiryDate(xmlCal);
		for ( AuthorizationStatus status : AuthorizationStatus.values() ) {
			auth.setStatus(status);
			boolean accepted = auth.isAccepted();
			Assert.assertEquals(status.toString(), AuthorizationStatus.ACCEPTED.equals(status), accepted);
		}
	}

	@Test
	public void acceptedExpiryDateInPast() {
		Authorization auth = new Authorization();
		XMLGregorianCalendar xmlCal = newXmlCalendar();
		xmlCal.setYear(xmlCal.getYear() - 1);
		auth.setExpiryDate(xmlCal);
		for ( AuthorizationStatus status : AuthorizationStatus.values() ) {
			auth.setStatus(status);
			boolean accepted = auth.isAccepted();
			Assert.assertFalse(status.toString(), accepted);
		}
	}

}
