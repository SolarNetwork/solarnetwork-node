/* ==================================================================
 * DefaultSecurityTokenServiceTests.java - 7/09/2023 6:34:55 am
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

package net.solarnetwork.node.runtime.test;

import static org.easymock.EasyMock.capture;
import static org.easymock.EasyMock.expect;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.sameInstance;
import java.util.UUID;
import org.easymock.Capture;
import org.easymock.EasyMock;
import org.easymock.IAnswer;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import net.solarnetwork.domain.KeyValuePair;
import net.solarnetwork.node.dao.SecurityTokenDao;
import net.solarnetwork.node.domain.SecurityToken;
import net.solarnetwork.node.runtime.DefaultSecurityTokenService;
import net.solarnetwork.service.StaticOptionalService;

/**
 * Test cases for the {@link DefaultSecurityTokenService} class.
 * 
 * @author matt
 * @version 1.0
 */
public class DefaultSecurityTokenServiceTests {

	private SecurityTokenDao securityTokenDao;

	private DefaultSecurityTokenService service;
	private SecurityToken last;

	@Before
	public void setup() {
		securityTokenDao = EasyMock.createMock(SecurityTokenDao.class);
		service = new DefaultSecurityTokenService(new StaticOptionalService<>(securityTokenDao));
	}

	private void replayAll() {
		EasyMock.replay(securityTokenDao);
	}

	@After
	public void teardown() {
		EasyMock.verify(securityTokenDao);
	}

	@Test
	public void createToken() {
		// GIVEN
		Capture<SecurityToken> tokenCaptor = Capture.newInstance();
		expect(securityTokenDao.save(capture(tokenCaptor))).andAnswer(new IAnswer<String>() {

			@Override
			public String answer() throws Throwable {
				return tokenCaptor.getValue().getId();
			}
		});

		// WHEN
		replayAll();
		SecurityToken details = SecurityToken.tokenDetails("a", "b");
		KeyValuePair result = service.createToken(details);

		// THEN
		assertThat("Result returned", result, is(notNullValue()));
		assertThat("Token ID generated", result.getKey(), is(notNullValue()));
		assertThat("Token secret generated", result.getValue(), is(notNullValue()));

		SecurityToken entity = tokenCaptor.getValue();
		assertThat("Entity persisted", entity, is(notNullValue()));
		assertThat("Entity ID same as returned token ID", entity.getId(), is(equalTo(result.getKey())));

		String[] holder = new String[1];
		entity.copySecret(s -> holder[0] = s);
		assertThat("Entity secret same as returned token secert", holder[0],
				is(equalTo(result.getValue())));

		last = entity;
	}

	@Test
	public void getToken() {
		// GIVEN
		final String tokenId = UUID.randomUUID().toString();
		final SecurityToken token = SecurityToken.tokenDetails(tokenId, null, null);
		expect(securityTokenDao.get(tokenId)).andReturn(token);

		// WHEN
		replayAll();
		SecurityToken result = service.tokenForId(tokenId);

		// THEN
		assertThat("Token returned from DAO", result, is(sameInstance(token)));
	}

	@Test
	public void updateToken() {
		// GIVEN
		Capture<SecurityToken> tokenCaptor = Capture.newInstance();
		expect(securityTokenDao.save(capture(tokenCaptor))).andAnswer(new IAnswer<String>() {

			@Override
			public String answer() throws Throwable {
				return tokenCaptor.getValue().getId();
			}
		});

		// WHEN
		replayAll();
		SecurityToken token = SecurityToken.tokenDetails("a", "b", "c");
		service.updateToken(token);

		// THEN
		SecurityToken entity = tokenCaptor.getValue();
		assertThat("Entity persisted as passed in", entity, is(sameInstance(token)));
	}

	@Test(expected = IllegalArgumentException.class)
	public void updateToken_withSecret() {
		replayAll();
		SecurityToken token = new SecurityToken("id", "secret");
		service.updateToken(token);
	}

	@Test
	public void deleteToken() {
		// GIVEN
		createToken();
		EasyMock.reset(securityTokenDao);

		Capture<SecurityToken> tokenCaptor = Capture.newInstance();
		securityTokenDao.delete(capture(tokenCaptor));

		// WHEN
		replayAll();
		service.deleteToken(last.getId());

		// THEN
		SecurityToken entity = tokenCaptor.getValue();
		assertThat("Entity deleted by primary key", entity.getId(), is(equalTo(last.getId())));
	}

}
