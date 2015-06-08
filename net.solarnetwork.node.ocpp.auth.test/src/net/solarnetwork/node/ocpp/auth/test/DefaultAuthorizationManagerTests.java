/* ==================================================================
 * DefaultAuthorizationManagerTests.java - 9/06/2015 6:58:08 am
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

package net.solarnetwork.node.ocpp.auth.test;

import static org.easymock.EasyMock.capture;
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import java.util.GregorianCalendar;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import net.solarnetwork.node.ocpp.Authorization;
import net.solarnetwork.node.ocpp.AuthorizationDao;
import net.solarnetwork.node.ocpp.CentralSystemServiceFactory;
import net.solarnetwork.node.ocpp.auth.DefaultAuthorizationManager;
import net.solarnetwork.node.test.AbstractNodeTest;
import net.solarnetwork.util.StaticOptionalService;
import ocpp.v15.AuthorizationStatus;
import ocpp.v15.AuthorizeRequest;
import ocpp.v15.AuthorizeResponse;
import ocpp.v15.CentralSystemService;
import ocpp.v15.IdTagInfo;
import org.easymock.Capture;
import org.easymock.EasyMock;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * Test cases for the {@link DefaultAuthorizationManager} class.
 * 
 * @author matt
 * @version 1.0
 */
public class DefaultAuthorizationManagerTests extends AbstractNodeTest {

	private static final String TEST_CHARGE_BOX_IDENT = "test.ident";
	private static final String TEST_ID_TAG = "test.tag";

	private CentralSystemServiceFactory centralSystem;
	private CentralSystemService client;
	private AuthorizationDao authorizationDao;

	private DefaultAuthorizationManager manager;

	@Before
	public void setup() {
		centralSystem = EasyMock.createMock(CentralSystemServiceFactory.class);
		client = EasyMock.createMock(CentralSystemService.class);
		authorizationDao = EasyMock.createMock(AuthorizationDao.class);

		manager = new DefaultAuthorizationManager();
		manager.setCentralSystem(centralSystem);
		manager.setAuthorizationDao(new StaticOptionalService<AuthorizationDao>(authorizationDao));
	}

	@After
	public void finish() {
		EasyMock.verify(centralSystem, client, authorizationDao);
	}

	private void replayAll() {
		EasyMock.replay(centralSystem, client, authorizationDao);
	}

	private IdTagInfo newIdTagInfo(String parentTagId, AuthorizationStatus status,
			XMLGregorianCalendar expiryDate) {
		IdTagInfo info = new IdTagInfo();
		info.setParentIdTag(parentTagId);
		info.setStatus(status);
		info.setExpiryDate(expiryDate);
		return info;
	}

	private AuthorizeResponse newAuthResponse(AuthorizationStatus status) {
		AuthorizeResponse r = new AuthorizeResponse();
		r.setIdTagInfo(newIdTagInfo(null, status, null));
		return r;
	}

	@Test
	public void acceptedNotCached() {
		// look in DAO first
		EasyMock.expect(authorizationDao.getAuthorization(TEST_ID_TAG)).andReturn(null);

		// not found in DAO, so query central system
		expect(centralSystem.chargeBoxIdentity()).andReturn(TEST_CHARGE_BOX_IDENT);
		expect(centralSystem.service()).andReturn(client);
		Capture<AuthorizeRequest> reqCapture = new Capture<AuthorizeRequest>();
		AuthorizeResponse authResp = newAuthResponse(AuthorizationStatus.ACCEPTED);
		expect(client.authorize(capture(reqCapture), eq(TEST_CHARGE_BOX_IDENT))).andReturn(authResp);

		// cache result in DAO
		Capture<Authorization> authCapture = new Capture<Authorization>();
		authorizationDao.storeAuthorization(capture(authCapture));

		replayAll();

		boolean authorized = manager.authorize(TEST_ID_TAG);
		Assert.assertTrue("Authorized", authorized);

		Assert.assertEquals("Req IdTag", TEST_ID_TAG, reqCapture.getValue().getIdTag());
		Assert.assertEquals("Cached Authorization IdTag", TEST_ID_TAG, authCapture.getValue().getIdTag());
		Assert.assertEquals("Cached Authorization IdTag", AuthorizationStatus.ACCEPTED, authCapture
				.getValue().getStatus());
	}

	@Test
	public void acceptedCached() {
		// look in DAO first
		Authorization cachedAuth = new Authorization(TEST_ID_TAG, newIdTagInfo(null,
				AuthorizationStatus.ACCEPTED, null));
		EasyMock.expect(authorizationDao.getAuthorization(TEST_ID_TAG)).andReturn(cachedAuth);

		replayAll();

		boolean authorized = manager.authorize(TEST_ID_TAG);
		Assert.assertTrue("Authorized", authorized);
	}

	@Test
	public void invalidCachedNoExpiry() {
		// look in DAO first
		Authorization cachedAuth = new Authorization(TEST_ID_TAG, newIdTagInfo(null,
				AuthorizationStatus.INVALID, null));
		EasyMock.expect(authorizationDao.getAuthorization(TEST_ID_TAG)).andReturn(cachedAuth);

		// invalid in DAO but no expiry date, so query central system
		expect(centralSystem.chargeBoxIdentity()).andReturn(TEST_CHARGE_BOX_IDENT);
		expect(centralSystem.service()).andReturn(client);
		Capture<AuthorizeRequest> reqCapture = new Capture<AuthorizeRequest>();
		AuthorizeResponse authResp = newAuthResponse(AuthorizationStatus.INVALID);
		expect(client.authorize(capture(reqCapture), eq(TEST_CHARGE_BOX_IDENT))).andReturn(authResp);

		// cache result in DAO
		Capture<Authorization> authCapture = new Capture<Authorization>();
		authorizationDao.storeAuthorization(capture(authCapture));

		replayAll();

		boolean authorized = manager.authorize(TEST_ID_TAG);
		Assert.assertFalse("Authorized", authorized);

		Assert.assertEquals("Req IdTag", TEST_ID_TAG, reqCapture.getValue().getIdTag());
		Assert.assertEquals("Cached Authorization IdTag", TEST_ID_TAG, authCapture.getValue().getIdTag());
		Assert.assertEquals("Cached Authorization IdTag", AuthorizationStatus.INVALID, authCapture
				.getValue().getStatus());
	}

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
	public void invalidCachedExpiryInFuture() {
		// look in DAO first
		XMLGregorianCalendar futureExipryDate = newXmlCalendar();
		futureExipryDate.setYear(futureExipryDate.getYear() + 1);
		Authorization cachedAuth = new Authorization(TEST_ID_TAG, newIdTagInfo(null,
				AuthorizationStatus.INVALID, futureExipryDate));
		EasyMock.expect(authorizationDao.getAuthorization(TEST_ID_TAG)).andReturn(cachedAuth);

		replayAll();

		boolean authorized = manager.authorize(TEST_ID_TAG);
		Assert.assertFalse("Authorized", authorized);
	}

	@Test
	public void invalidCachedExpiryInPastAccepted() {
		// look in DAO first
		XMLGregorianCalendar pastExipryDate = newXmlCalendar();
		pastExipryDate.setYear(pastExipryDate.getYear() - 1);
		Authorization cachedAuth = new Authorization(TEST_ID_TAG, newIdTagInfo(null,
				AuthorizationStatus.INVALID, null));
		EasyMock.expect(authorizationDao.getAuthorization(TEST_ID_TAG)).andReturn(cachedAuth);

		// invalid in DAO but expiry date in past, so query central system (which says accepted)
		expect(centralSystem.chargeBoxIdentity()).andReturn(TEST_CHARGE_BOX_IDENT);
		expect(centralSystem.service()).andReturn(client);
		Capture<AuthorizeRequest> reqCapture = new Capture<AuthorizeRequest>();
		AuthorizeResponse authResp = newAuthResponse(AuthorizationStatus.ACCEPTED);
		expect(client.authorize(capture(reqCapture), eq(TEST_CHARGE_BOX_IDENT))).andReturn(authResp);

		// cache result in DAO
		Capture<Authorization> authCapture = new Capture<Authorization>();
		authorizationDao.storeAuthorization(capture(authCapture));

		replayAll();

		boolean authorized = manager.authorize(TEST_ID_TAG);
		Assert.assertTrue("Authorized", authorized);

		Assert.assertEquals("Req IdTag", TEST_ID_TAG, reqCapture.getValue().getIdTag());
		Assert.assertEquals("Cached Authorization IdTag", TEST_ID_TAG, authCapture.getValue().getIdTag());
		Assert.assertEquals("Cached Authorization IdTag", AuthorizationStatus.ACCEPTED, authCapture
				.getValue().getStatus());
	}
}
