/* ==================================================================
 * StompSetupServerHandlerTests.java - 15/08/2021 5:17:28 PM
 * 
 * Copyright 2021 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.setup.stomp.test;

import static java.util.Collections.singletonList;
import static java.util.Collections.singletonMap;
import static net.solarnetwork.node.setup.stomp.SetupTopic.Authenticate;
import static org.easymock.EasyMock.expect;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;
import java.time.Instant;
import java.util.Collections;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.apache.commons.codec.digest.DigestUtils;
import org.easymock.Capture;
import org.easymock.EasyMock;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCrypt;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.DefaultChannelPromise;
import io.netty.handler.codec.stomp.DefaultStompFrame;
import io.netty.handler.codec.stomp.StompCommand;
import io.netty.handler.codec.stomp.StompFrame;
import io.netty.handler.codec.stomp.StompHeaders;
import net.solarnetwork.node.reactor.FeedbackInstructionHandler;
import net.solarnetwork.node.setup.UserAuthenticationInfo;
import net.solarnetwork.node.setup.UserService;
import net.solarnetwork.node.setup.stomp.SetupHeaders;
import net.solarnetwork.node.setup.stomp.SetupSession;
import net.solarnetwork.node.setup.stomp.StompSetupServerHandler;
import net.solarnetwork.security.SnsAuthorizationBuilder;

/**
 * Test cases for the {@link StompSetupServerHandler} class.
 * 
 * @author matt
 * @version 1.0
 */
public class StompSetupServerHandlerTests {

	private static final String TEST_LOGIN = "foo";
	private static final String BCRYPT_ALG = "bcrypt";
	private static final String SALT_PARAM = "salt";

	private UserService userService;
	private UserDetailsService userDetailsService;
	private FeedbackInstructionHandler instructionHandler;
	private ChannelHandlerContext ctx;
	private Channel channel;
	private ConcurrentMap<UUID, SetupSession> sessions;
	private StompSetupServerHandler handler;

	@Before
	public void setup() {
		userService = EasyMock.createMock(UserService.class);
		userDetailsService = EasyMock.createMock(UserDetailsService.class);
		instructionHandler = EasyMock.createMock(FeedbackInstructionHandler.class);
		ctx = EasyMock.createMock(ChannelHandlerContext.class);
		channel = EasyMock.createMock(Channel.class);
		sessions = new ConcurrentHashMap<>(4, 0.9f, 1);
		handler = new StompSetupServerHandler(sessions, userService, userDetailsService,
				singletonList(instructionHandler));
	}

	@After
	public void teardown() {
		EasyMock.verify(userService, userDetailsService, instructionHandler, ctx, channel);
	}

	private void replayAll() {
		EasyMock.replay(userService, userDetailsService, instructionHandler, ctx, channel);
	}

	@Test
	public void connect_noHeaders() {
		// GIVEN

		// get the channel to associate with the session
		expect(ctx.channel()).andReturn(channel);

		// return ERROR to client
		ChannelFuture responseFuture = new DefaultChannelPromise(channel);
		Capture<Object> responseCaptor = new Capture<>();
		expect(ctx.writeAndFlush(EasyMock.capture(responseCaptor))).andReturn(responseFuture);

		// WHEN
		replayAll();
		DefaultStompFrame f = new DefaultStompFrame(StompCommand.CONNECT);
		handler.channelRead(ctx, f);

		// THEN
		assertThat("Response is StompFrame", responseCaptor.getValue(),
				is(instanceOf(StompFrame.class)));
		StompFrame response = (StompFrame) responseCaptor.getValue();
		assertThat("Response is ERROR", response.command(), is(StompCommand.ERROR));
		assertThat("Response has message", response.headers().getAsString(StompHeaders.MESSAGE),
				is(notNullValue()));
	}

	@Test
	public void connect_userUnknown() {
		// GIVEN
		// get the channel to associate with the session
		expect(ctx.channel()).andReturn(channel);

		// request user auth info
		expect(userService.authenticationInfo(TEST_LOGIN)).andReturn(null);

		// return ERROR to client
		ChannelFuture responseFuture = new DefaultChannelPromise(channel);
		Capture<Object> responseCaptor = new Capture<>();
		expect(ctx.writeAndFlush(EasyMock.capture(responseCaptor))).andReturn(responseFuture);

		// WHEN
		replayAll();
		DefaultStompFrame f = new DefaultStompFrame(StompCommand.CONNECT);
		f.headers().set(StompHeaders.ACCEPT_VERSION, "1.2");
		f.headers().set(StompHeaders.HOST, "localhost");
		f.headers().set(StompHeaders.LOGIN, TEST_LOGIN);
		handler.channelRead(ctx, f);

		// THEN
		assertThat("Response is StompFrame", responseCaptor.getValue(),
				is(instanceOf(StompFrame.class)));
		StompFrame response = (StompFrame) responseCaptor.getValue();
		assertThat("Response is ERROR", response.command(), is(StompCommand.ERROR));
		assertThat("Response has message", response.headers().getAsString(StompHeaders.MESSAGE),
				is(notNullValue()));
	}

	@Test
	public void connect_ok() {
		// GIVEN
		// get the channel to associate with the session
		expect(ctx.channel()).andReturn(channel).anyTimes();

		// request user auth info
		final String salt = BCrypt.gensalt();
		final UserAuthenticationInfo userInfo = new UserAuthenticationInfo(BCRYPT_ALG,
				singletonMap(SALT_PARAM, salt));
		expect(userService.authenticationInfo(TEST_LOGIN)).andReturn(userInfo);

		// add hook to clean up sessions
		ChannelFuture closeFuture = new DefaultChannelPromise(channel);
		expect(channel.closeFuture()).andReturn(closeFuture);

		// return ERROR to client
		ChannelFuture responseFuture = new DefaultChannelPromise(channel);
		Capture<Object> responseCaptor = new Capture<>();
		expect(ctx.writeAndFlush(EasyMock.capture(responseCaptor))).andReturn(responseFuture);

		// WHEN
		replayAll();
		DefaultStompFrame f = new DefaultStompFrame(StompCommand.CONNECT);
		f.headers().set(StompHeaders.ACCEPT_VERSION, "1.2");
		f.headers().set(StompHeaders.HOST, "localhost");
		f.headers().set(StompHeaders.LOGIN, TEST_LOGIN);
		handler.channelRead(ctx, f);

		// THEN
		assertThat("Response is StompFrame", responseCaptor.getValue(),
				is(instanceOf(StompFrame.class)));
		StompFrame response = (StompFrame) responseCaptor.getValue();
		assertThat("Response is CONNECTED", response.command(), is(StompCommand.CONNECTED));
		assertThat("Response has message", response.headers().getAsString(StompHeaders.MESSAGE),
				is(notNullValue()));
		assertThat("Response has session ID", response.headers().getAsString(StompHeaders.SESSION),
				is(notNullValue()));
		assertThat("Session saved as SetupSession",
				sessions.get(UUID.fromString(response.headers().getAsString(StompHeaders.SESSION))),
				is(instanceOf(SetupSession.class)));
		assertThat("Response has authenticate header",
				response.headers().getAsString(SetupHeaders.Authenticate.getValue()), is("SNS"));
		assertThat("Response has auth-hash header",
				response.headers().getAsString(SetupHeaders.AuthHash.getValue()),
				is(userInfo.getHashAlgorithm()));
		assertThat("Response has auth-hash-param-salt",
				response.headers().getAsString("auth-hash-param-salt"),
				is(userInfo.getHashParameters().get(SALT_PARAM)));
	}

	@Test
	public void authenticate_ok() {
		// GIVEN
		final String pw = "password123";
		final String salt = BCrypt.gensalt();
		final String pwHash = BCrypt.hashpw(pw, salt);

		// get the channel to associate with the session
		expect(ctx.channel()).andReturn(channel);

		// assume CONNECT already
		SetupSession session = new SetupSession(TEST_LOGIN, channel);
		sessions.put(session.getSessionId(), session);

		// load UserDetails
		GrantedAuthority role = new SimpleGrantedAuthority("ROLE_USER");
		UserDetails user = new User(TEST_LOGIN, pwHash, Collections.singleton(role));
		expect(userDetailsService.loadUserByUsername(TEST_LOGIN)).andReturn(user);

		// WHEN
		replayAll();

		Instant now = Instant.now();
		String secret = DigestUtils.sha256Hex(pwHash);
		// @formatter:off
		SnsAuthorizationBuilder authBuilder = new SnsAuthorizationBuilder(TEST_LOGIN)
				.date(now)
				.verb(StompCommand.SEND.toString())
				.path(Authenticate.getTopic());
		// @formatter:on
		String authHeader = authBuilder.build(secret);

		DefaultStompFrame f = new DefaultStompFrame(StompCommand.SEND);
		f.headers().set(StompHeaders.DESTINATION, Authenticate.getTopic());
		f.headers().set(SetupHeaders.Date.getValue(),
				authBuilder.headerValue(SetupHeaders.Date.getValue()));
		f.headers().set(SetupHeaders.Authorization.getValue(), authHeader);
		handler.channelRead(ctx, f);

		// THEN
		assertThat("Session is authenticated", session.isAuthenticated(), is(true));
	}

}
