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

package net.solarnetwork.node.setup.stomp.server.test;

import static java.util.Collections.singletonList;
import static java.util.Collections.singletonMap;
import static net.solarnetwork.node.reactor.InstructionUtils.createStatus;
import static net.solarnetwork.node.setup.stomp.SetupTopic.Authenticate;
import static org.easymock.EasyMock.capture;
import static org.easymock.EasyMock.expect;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.springframework.security.core.userdetails.User.withUsername;
import java.nio.charset.Charset;
import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executor;
import org.apache.commons.codec.digest.DigestUtils;
import org.easymock.Capture;
import org.easymock.EasyMock;
import org.easymock.IAnswer;
import org.hamcrest.Matchers;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.util.AntPathMatcher;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.DefaultChannelPromise;
import io.netty.handler.codec.stomp.DefaultStompFrame;
import io.netty.handler.codec.stomp.StompCommand;
import io.netty.handler.codec.stomp.StompFrame;
import io.netty.handler.codec.stomp.StompHeaders;
import net.solarnetwork.codec.BasicGeneralDatumSerializer;
import net.solarnetwork.domain.InstructionStatus.InstructionState;
import net.solarnetwork.domain.datum.GeneralDatum;
import net.solarnetwork.node.reactor.Instruction;
import net.solarnetwork.node.reactor.InstructionHandler;
import net.solarnetwork.node.reactor.InstructionStatus;
import net.solarnetwork.node.reactor.SimpleInstructionExecutionService;
import net.solarnetwork.node.service.DatumService;
import net.solarnetwork.node.setup.UserAuthenticationInfo;
import net.solarnetwork.node.setup.UserService;
import net.solarnetwork.node.setup.stomp.SetupHeader;
import net.solarnetwork.node.setup.stomp.SetupStatus;
import net.solarnetwork.node.setup.stomp.server.SetupSession;
import net.solarnetwork.node.setup.stomp.server.StompSetupServerHandler;
import net.solarnetwork.node.setup.stomp.server.StompSetupServerService;
import net.solarnetwork.security.SnsAuthorizationBuilder;
import net.solarnetwork.test.CallingThreadExecutorService;

/**
 * Test cases for the {@link StompSetupServerHandler} class.
 *
 * @author matt
 * @version 2.0
 */
public class StompSetupServerHandlerTests {

	private static final String TEST_LOGIN = "foo";
	private static final String BCRYPT_ALG = "bcrypt";
	private static final String SALT_PARAM = "salt";

	private Executor executor;
	private UserService userService;
	private UserDetailsService userDetailsService;
	private DatumService datumService;
	private InstructionHandler instructionHandler;
	private StompSetupServerService serverService;
	private ObjectMapper objectMapper;
	private ChannelHandlerContext ctx;
	private Channel channel;
	private ConcurrentMap<UUID, SetupSession> sessions;
	private StompSetupServerHandler handler;

	@Before
	public void setup() {
		userService = EasyMock.createMock(UserService.class);
		userDetailsService = EasyMock.createMock(UserDetailsService.class);
		datumService = EasyMock.createMock(DatumService.class);
		instructionHandler = EasyMock.createMock(InstructionHandler.class);
		serverService = new StompSetupServerService(userService, userDetailsService,
				new AntPathMatcher(),
				new SimpleInstructionExecutionService(singletonList(instructionHandler)));
		objectMapper = createObjectMapper();
		ctx = EasyMock.createMock(ChannelHandlerContext.class);
		channel = EasyMock.createMock(Channel.class);
		sessions = new ConcurrentHashMap<>(4, 0.9f, 1);
		executor = new CallingThreadExecutorService();
		handler = new StompSetupServerHandler(sessions, serverService, objectMapper, executor);
	}

	private ObjectMapper createObjectMapper() {
		ObjectMapper m = new ObjectMapper();
		SimpleModule mod = new SimpleModule("Test");
		mod.addSerializer(GeneralDatum.class, BasicGeneralDatumSerializer.INSTANCE);
		m.registerModule(mod);
		return m;
	}

	@After
	public void teardown() {
		EasyMock.verify(userService, userDetailsService, datumService, instructionHandler, ctx, channel);
	}

	private void replayAll() {
		EasyMock.replay(userService, userDetailsService, datumService, instructionHandler, ctx, channel);
	}

	@Test
	public void connect_noHeaders() {
		// GIVEN

		// get the channel to associate with the session
		expect(ctx.channel()).andReturn(channel);

		// return ERROR to client
		ChannelFuture responseFuture = new DefaultChannelPromise(channel);
		Capture<Object> responseCaptor = Capture.newInstance();
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
		Capture<Object> responseCaptor = Capture.newInstance();
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

		// return CONNECTED to client
		ChannelFuture responseFuture = new DefaultChannelPromise(channel);
		Capture<Object> responseCaptor = Capture.newInstance();
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
				response.headers().getAsString(SetupHeader.Authenticate.getValue()), is("SNS"));
		assertThat("Response has auth-hash header",
				response.headers().getAsString(SetupHeader.AuthHash.getValue()),
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
				.path(Authenticate.getValue());
		// @formatter:on
		String authHeader = authBuilder.build(secret);

		DefaultStompFrame f = new DefaultStompFrame(StompCommand.SEND);
		f.headers().set(StompHeaders.DESTINATION, Authenticate.getValue());
		f.headers().set(SetupHeader.Date.getValue(),
				authBuilder.headerValue(SetupHeader.Date.getValue()));
		f.headers().set(SetupHeader.Authorization.getValue(), authHeader);
		handler.channelRead(ctx, f);

		// THEN
		assertThat("Session is authenticated", session.isAuthenticated(), is(true));
	}

	@Test
	public void authenticate_error_dateSkew_past() {
		// GIVEN
		handler.setMaxAuthDateSkewSeconds(1);

		final String pw = "password123";
		final String salt = BCrypt.gensalt();
		final String pwHash = BCrypt.hashpw(pw, salt);

		// get the channel to associate with the session
		expect(ctx.channel()).andReturn(channel);

		// assume CONNECT already
		SetupSession session = new SetupSession(TEST_LOGIN, channel);
		sessions.put(session.getSessionId(), session);

		// return ERROR to client
		ChannelFuture responseFuture = new DefaultChannelPromise(channel);
		Capture<Object> responseCaptor = Capture.newInstance();
		expect(ctx.writeAndFlush(EasyMock.capture(responseCaptor))).andReturn(responseFuture);

		// WHEN
		replayAll();

		Instant now = Instant.now().minusSeconds(10);
		String secret = DigestUtils.sha256Hex(pwHash);
		// @formatter:off
		SnsAuthorizationBuilder authBuilder = new SnsAuthorizationBuilder(TEST_LOGIN)
				.date(now)
				.verb(StompCommand.SEND.toString())
				.path(Authenticate.getValue());
		// @formatter:on
		String authHeader = authBuilder.build(secret);

		DefaultStompFrame f = new DefaultStompFrame(StompCommand.SEND);
		f.headers().set(StompHeaders.DESTINATION, Authenticate.getValue());
		f.headers().set(SetupHeader.Date.getValue(),
				authBuilder.headerValue(SetupHeader.Date.getValue()));
		f.headers().set(SetupHeader.Authorization.getValue(), authHeader);
		handler.channelRead(ctx, f);

		// THEN
		assertThat("Response is StompFrame", responseCaptor.getValue(),
				is(instanceOf(StompFrame.class)));
		StompFrame response = (StompFrame) responseCaptor.getValue();
		assertThat("Response is ERROR", response.command(), is(StompCommand.ERROR));
		assertThat("Response has date skew message",
				response.headers().getAsString(StompHeaders.MESSAGE), containsString("skew"));
	}

	@Test
	public void authenticate_error_dateSkew_future() {
		// GIVEN
		handler.setMaxAuthDateSkewSeconds(1);

		final String pw = "password123";
		final String salt = BCrypt.gensalt();
		final String pwHash = BCrypt.hashpw(pw, salt);

		// get the channel to associate with the session
		expect(ctx.channel()).andReturn(channel);

		// assume CONNECT already
		SetupSession session = new SetupSession(TEST_LOGIN, channel);
		sessions.put(session.getSessionId(), session);

		// return ERROR to client
		ChannelFuture responseFuture = new DefaultChannelPromise(channel);
		Capture<Object> responseCaptor = Capture.newInstance();
		expect(ctx.writeAndFlush(EasyMock.capture(responseCaptor))).andReturn(responseFuture);

		// WHEN
		replayAll();

		Instant now = Instant.now().plusSeconds(10);
		String secret = DigestUtils.sha256Hex(pwHash);
		// @formatter:off
		SnsAuthorizationBuilder authBuilder = new SnsAuthorizationBuilder(TEST_LOGIN)
				.date(now)
				.verb(StompCommand.SEND.toString())
				.path(Authenticate.getValue());
		// @formatter:on
		String authHeader = authBuilder.build(secret);

		DefaultStompFrame f = new DefaultStompFrame(StompCommand.SEND);
		f.headers().set(StompHeaders.DESTINATION, Authenticate.getValue());
		f.headers().set(SetupHeader.Date.getValue(),
				authBuilder.headerValue(SetupHeader.Date.getValue()));
		f.headers().set(SetupHeader.Authorization.getValue(), authHeader);
		handler.channelRead(ctx, f);

		// THEN
		assertThat("Response is StompFrame", responseCaptor.getValue(),
				is(instanceOf(StompFrame.class)));
		StompFrame response = (StompFrame) responseCaptor.getValue();
		assertThat("Response is ERROR", response.command(), is(StompCommand.ERROR));
		assertThat("Response has date skew message",
				response.headers().getAsString(StompHeaders.MESSAGE), containsString("skew"));
	}

	/**
	 * Example use of {@link SnsAuthorizationBuilder} for generating the
	 * {@code authorization} and {@code date} headers required by the SNS
	 * authorization scheme.
	 *
	 * <p>
	 * This example is meant to help you understand how a client application
	 * would authenticate to the SolarNode STOMP Setup Server as the user
	 * {@literal me@example.com} whose password is {@literal password123}.
	 * </p>
	 */
	@Ignore
	public void authenticate_example_bcrypt() {
		// We received a CONNECTED frame whose `auth-hash` header value was `bcrypt`.
		// Take the salt provided by the `auth-hash-param-salt` header value:
		String salt = "$2a$10$upVbEZHge9Iph1NN3L6ENO";

		// Generate the BCrypt hash of the plain-text user password `password123`
		String passwordHash = BCrypt.hashpw("password123", salt);

		// Compute the SHA-256 digest of the BCrypt hash: this will be our SNS secret key
		String secret = DigestUtils.sha256Hex(passwordHash);

		// The signature includes the current date
		Instant now = Instant.now();

		// Compute the SNS `authorization` header value, using the STOMP command `SEND` as
		// the verb and `/setup/authenticate` as the path:

		// @formatter:off

		SnsAuthorizationBuilder authBuilder = new SnsAuthorizationBuilder("me@example.com")
				.date(now)
				.verb("SEND")
				.path("/setup/authenticate");
		String authHeader = authBuilder.build(secret);

		// @formatter:on

		// at this point, authHeader contains the `authorization` header value, and
		// we must also use a correctly formatted `date` header:
		Map<String, String> sendFrameHeaders = new HashMap<>();
		sendFrameHeaders.put("date", authBuilder.headerValue("date"));
		sendFrameHeaders.put("authorization", authHeader);

		// Now conceptually we'd post the SEND frame using our STOMP client, e.g.
		/*-
		client.publish("SEND", sendFrameHeaders);
		 */
	}

	private SetupSession givenSessionAuthenticated() {
		String[] roles = new String[] { "ROLE_USER" };
		UserDetails user = withUsername(TEST_LOGIN).password("pw").authorities(roles).build();
		SetupSession session = new SetupSession(TEST_LOGIN, channel);
		session.setAuthentication(new TestingAuthenticationToken(user, null, roles));
		sessions.put(session.getSessionId(), session);
		return session;
	}

	@Test
	public void subscribe_ok() {
		// GIVEN
		// get the channel to associate with the session
		expect(ctx.channel()).andReturn(channel);

		// assume authenticated already
		final SetupSession session = givenSessionAuthenticated();

		// WHEN
		replayAll();

		final String subId = "123";
		DefaultStompFrame f = new DefaultStompFrame(StompCommand.SUBSCRIBE);
		f.headers().set(StompHeaders.ID, subId);
		f.headers().set(StompHeaders.DESTINATION, "/setup/**");
		handler.channelRead(ctx, f);

		// THEN
		assertThat("Session is subscribed", session.subscriptionIdsForTopic("/setup/**", null),
				containsInAnyOrder(subId));
	}

	private SetupSession givenSessionAuthenticatedAndSubscribed() {
		SetupSession session = givenSessionAuthenticated();
		session.addSubscription("0", "/setup/**");
		return session;
	}

	@Test
	public void sendInstruction_ok() {
		// GIVEN
		// get the channel to associate with the session
		expect(ctx.channel()).andReturn(channel);

		// assume authenticated already
		SetupSession session = givenSessionAuthenticatedAndSubscribed();

		// send instruction to configured handlers
		final String dest = "/setup/do/something";
		expect(instructionHandler.handlesTopic(InstructionHandler.TOPIC_SYSTEM_CONFIGURE))
				.andReturn(true);

		// process instruction OK
		final String contentType = "text/plain;charset=utf-8";
		final String arg = "Hello, world.";
		final String res = "Good day to you, sir.";
		Capture<Instruction> instrCaptor = Capture.newInstance();
		expect(instructionHandler.processInstruction(capture(instrCaptor)))
				.andAnswer(new IAnswer<InstructionStatus>() {

					@Override
					public InstructionStatus answer() throws Throwable {
						// should be authenticated as the user here
						assertThat("Authenticated as login",
								SecurityContextHolder.getContext().getAuthentication(),
								is(Matchers.sameInstance(session.getAuthentication())));

						Instruction instr = instrCaptor.getValue();
						assertThat("Instruction topic is SystemConfigure", instr.getTopic(),
								is(InstructionHandler.TOPIC_SYSTEM_CONFIGURE));
						assertThat("Intruction service param is STOMP dest",
								instr.getParameterValue(InstructionHandler.PARAM_SERVICE), is(dest));
						assertThat("Instruction service arg is STOMP body",
								instr.getParameterValue(InstructionHandler.PARAM_SERVICE_ARGUMENT),
								is(arg));
						assertThat("STOMP content-type header provided as parameter",
								instr.getParameterValue(StompHeaders.CONTENT_TYPE.toString()),
								is(contentType));
						assertThat("Custom STOMP header provided as parameter",
								instr.getParameterValue("foo"), is("bar"));
						return createStatus(instr, InstructionState.Completed,
								singletonMap(InstructionHandler.PARAM_SERVICE_RESULT, res));
					}

				});

		// post instruction result as MESSAGE back to client
		Capture<Object> msgCaptor = Capture.newInstance();
		expect(ctx.writeAndFlush(capture(msgCaptor))).andReturn(new DefaultChannelPromise(channel));

		// WHEN
		replayAll();

		DefaultStompFrame f = new DefaultStompFrame(StompCommand.SEND);
		f.headers().set(StompHeaders.DESTINATION, dest);
		f.headers().set("foo", "bar");
		f.headers().set(StompHeaders.CONTENT_TYPE, contentType);
		f.headers().set(StompHeaders.CONTENT_LENGTH, String.valueOf(arg.length()));
		f.content().writeBytes(arg.getBytes(Charset.forName("UTF-8")));

		handler.channelRead(ctx, f);

		// THEN
		assertThat("MESSAGE response provided", msgCaptor.getValue(), is(instanceOf(StompFrame.class)));
		StompFrame msg = (StompFrame) msgCaptor.getValue();
		assertThat("Response dest is SEND dest", msg.headers().getAsString(StompHeaders.DESTINATION),
				is(dest));
		assertThat("Response status is OK", msg.headers().getAsString(SetupHeader.Status.getValue()),
				is(String.valueOf(SetupStatus.Ok.getCode())));
		assertThat("Custom request header copied to response", msg.headers().getAsString("foo"),
				is("bar"));
		assertThat("Response body is instruction result as JSON string",
				msg.content().toString(Charset.forName("UTF-8")), is(String.format("\"%s\"", res)));
	}

	@Test
	public void sendInstruction_noHandler() {
		// GIVEN
		// get the channel to associate with the session
		expect(ctx.channel()).andReturn(channel);

		// assume authenticated already
		givenSessionAuthenticatedAndSubscribed();

		// post instruction to configured handlers
		final String dest = "/setup/do/something";
		expect(instructionHandler.handlesTopic(InstructionHandler.TOPIC_SYSTEM_CONFIGURE))
				.andReturn(false);

		// post instruction result as MESSAGE back to client
		Capture<Object> msgCaptor = Capture.newInstance();
		expect(ctx.writeAndFlush(capture(msgCaptor))).andReturn(new DefaultChannelPromise(channel));

		// WHEN
		replayAll();

		DefaultStompFrame f = new DefaultStompFrame(StompCommand.SEND);
		f.headers().set(StompHeaders.DESTINATION, dest);
		f.content().writeBytes("Yo".getBytes(Charset.forName("UTF-8")));

		handler.channelRead(ctx, f);

		// THEN
		assertThat("MESSAGE response provided", msgCaptor.getValue(), is(instanceOf(StompFrame.class)));
		StompFrame msg = (StompFrame) msgCaptor.getValue();
		assertThat("Response dest is SEND dest", msg.headers().getAsString(StompHeaders.DESTINATION),
				is(dest));
		assertThat("Response status is NOT FOUND",
				msg.headers().getAsString(SetupHeader.Status.getValue()),
				is(String.valueOf(SetupStatus.NotFound.getCode())));
	}

	@Test
	public void sendInstruction_asyncExecuting() {
		// GIVEN
		// get the channel to associate with the session
		expect(ctx.channel()).andReturn(channel);

		// assume authenticated already
		givenSessionAuthenticatedAndSubscribed();

		// post instruction to configured handlers
		final String dest = "/setup/do/something";
		expect(instructionHandler.handlesTopic(InstructionHandler.TOPIC_SYSTEM_CONFIGURE))
				.andReturn(true);

		final String arg = "Hello, world.";
		Capture<Instruction> instrCaptor = Capture.newInstance();
		expect(instructionHandler.processInstruction(capture(instrCaptor)))
				.andAnswer(new IAnswer<InstructionStatus>() {

					@Override
					public InstructionStatus answer() throws Throwable {
						Instruction instr = instrCaptor.getValue();
						assertThat("Instruction topic is SystemConfigure", instr.getTopic(),
								is(InstructionHandler.TOPIC_SYSTEM_CONFIGURE));
						assertThat("Intruction service param is STOMP dest",
								instr.getParameterValue(InstructionHandler.PARAM_SERVICE), is(dest));
						assertThat("Instruction service arg is STOMP body",
								instr.getParameterValue(InstructionHandler.PARAM_SERVICE_ARGUMENT),
								is(arg));
						return createStatus(instr, InstructionState.Executing, null);
					}

				});

		// post instruction result as MESSAGE back to client
		Capture<Object> msgCaptor = Capture.newInstance();
		expect(ctx.writeAndFlush(capture(msgCaptor))).andReturn(new DefaultChannelPromise(channel));

		// WHEN
		replayAll();

		DefaultStompFrame f = new DefaultStompFrame(StompCommand.SEND);
		f.headers().set(StompHeaders.DESTINATION, dest);
		f.content().writeBytes(arg.getBytes(Charset.forName("UTF-8")));

		handler.channelRead(ctx, f);

		// THEN
		assertThat("MESSAGE response provided", msgCaptor.getValue(), is(instanceOf(StompFrame.class)));
		StompFrame msg = (StompFrame) msgCaptor.getValue();
		assertThat("Response dest is SEND dest", msg.headers().getAsString(StompHeaders.DESTINATION),
				is(dest));
		assertThat("Response status is NOT FOUND",
				msg.headers().getAsString(SetupHeader.Status.getValue()),
				is(String.valueOf(SetupStatus.Accepted.getCode())));
	}

	@Test
	public void sendInstruction_declined() {
		// GIVEN
		// get the channel to associate with the session
		expect(ctx.channel()).andReturn(channel);

		// assume authenticated already
		givenSessionAuthenticatedAndSubscribed();

		// post instruction to configured handlers
		final String dest = "/setup/do/something";
		expect(instructionHandler.handlesTopic(InstructionHandler.TOPIC_SYSTEM_CONFIGURE))
				.andReturn(true);

		final String arg = "Hello, world.";
		Capture<Instruction> instrCaptor = Capture.newInstance();
		expect(instructionHandler.processInstruction(capture(instrCaptor)))
				.andAnswer(new IAnswer<InstructionStatus>() {

					@Override
					public InstructionStatus answer() throws Throwable {
						Instruction instr = instrCaptor.getValue();
						assertThat("Instruction topic is SystemConfigure", instr.getTopic(),
								is(InstructionHandler.TOPIC_SYSTEM_CONFIGURE));
						assertThat("Intruction service param is STOMP dest",
								instr.getParameterValue(InstructionHandler.PARAM_SERVICE), is(dest));
						assertThat("Instruction service arg is STOMP body",
								instr.getParameterValue(InstructionHandler.PARAM_SERVICE_ARGUMENT),
								is(arg));
						return createStatus(instr, InstructionState.Declined, null);
					}

				});

		// post instruction result as MESSAGE back to client
		Capture<Object> msgCaptor = Capture.newInstance();
		expect(ctx.writeAndFlush(capture(msgCaptor))).andReturn(new DefaultChannelPromise(channel));

		// WHEN
		replayAll();

		DefaultStompFrame f = new DefaultStompFrame(StompCommand.SEND);
		f.headers().set(StompHeaders.DESTINATION, dest);
		f.content().writeBytes(arg.getBytes(Charset.forName("UTF-8")));

		handler.channelRead(ctx, f);

		// THEN
		assertThat("MESSAGE response provided", msgCaptor.getValue(), is(instanceOf(StompFrame.class)));
		StompFrame msg = (StompFrame) msgCaptor.getValue();
		assertThat("Response dest is SEND dest", msg.headers().getAsString(StompHeaders.DESTINATION),
				is(dest));
		assertThat("Response status is UNPROCESSABLE",
				msg.headers().getAsString(SetupHeader.Status.getValue()),
				is(String.valueOf(SetupStatus.Unprocessable.getCode())));
	}

	@Test
	public void sendInstruction_internalError() {
		// GIVEN
		// get the channel to associate with the session
		expect(ctx.channel()).andReturn(channel);

		// assume authenticated already
		givenSessionAuthenticatedAndSubscribed();

		// post instruction to configured handlers
		final String dest = "/setup/do/something";
		expect(instructionHandler.handlesTopic(InstructionHandler.TOPIC_SYSTEM_CONFIGURE))
				.andReturn(true);

		final String arg = "Hello, world.";
		final String error = "No way!";
		Capture<Instruction> instrCaptor = Capture.newInstance();
		expect(instructionHandler.processInstruction(capture(instrCaptor)))
				.andAnswer(new IAnswer<InstructionStatus>() {

					@Override
					public InstructionStatus answer() throws Throwable {
						throw new RuntimeException(error);
					}

				});

		// post instruction result as MESSAGE back to client
		Capture<Object> msgCaptor = Capture.newInstance();
		expect(ctx.writeAndFlush(capture(msgCaptor))).andReturn(new DefaultChannelPromise(channel));

		// WHEN
		replayAll();

		DefaultStompFrame f = new DefaultStompFrame(StompCommand.SEND);
		f.headers().set(StompHeaders.DESTINATION, dest);
		f.content().writeBytes(arg.getBytes(Charset.forName("UTF-8")));

		handler.channelRead(ctx, f);

		// THEN
		assertThat("MESSAGE response provided", msgCaptor.getValue(), is(instanceOf(StompFrame.class)));
		StompFrame msg = (StompFrame) msgCaptor.getValue();
		assertThat("Response dest is SEND dest", msg.headers().getAsString(StompHeaders.DESTINATION),
				is(dest));
		assertThat("Response status is INTERNAL ERROR",
				msg.headers().getAsString(SetupHeader.Status.getValue()),
				is(String.valueOf(SetupStatus.InternalError.getCode())));
		assertThat("Response message from exception", msg.headers().getAsString(StompHeaders.MESSAGE),
				is(error));
	}

	@Test
	public void sendInstruction_customStatusCode() {
		// GIVEN
		// get the channel to associate with the session
		expect(ctx.channel()).andReturn(channel);

		// assume authenticated already
		givenSessionAuthenticatedAndSubscribed();

		// send instruction to configured handlers
		final String dest = "/setup/do/something";
		expect(instructionHandler.handlesTopic(InstructionHandler.TOPIC_SYSTEM_CONFIGURE))
				.andReturn(true);

		// process instruction OK
		final int statusCode = 8675309;
		final String message = "Jenny I've got your number.";
		Capture<Instruction> instrCaptor = Capture.newInstance();
		expect(instructionHandler.processInstruction(capture(instrCaptor)))
				.andAnswer(new IAnswer<InstructionStatus>() {

					@Override
					public InstructionStatus answer() throws Throwable {
						Instruction instr = instrCaptor.getValue();
						assertThat("Instruction topic is SystemConfigure", instr.getTopic(),
								is(InstructionHandler.TOPIC_SYSTEM_CONFIGURE));
						assertThat("Intruction service param is STOMP dest",
								instr.getParameterValue(InstructionHandler.PARAM_SERVICE), is(dest));
						Map<String, Object> resultParams = new HashMap<>();
						resultParams.put(InstructionHandler.PARAM_STATUS_CODE, statusCode);
						resultParams.put(InstructionHandler.PARAM_MESSAGE, message);
						return createStatus(instr, InstructionState.Completed, resultParams);
					}

				});

		// post instruction result as MESSAGE back to client
		Capture<Object> msgCaptor = Capture.newInstance();
		expect(ctx.writeAndFlush(capture(msgCaptor))).andReturn(new DefaultChannelPromise(channel));

		// WHEN
		replayAll();

		DefaultStompFrame f = new DefaultStompFrame(StompCommand.SEND);
		f.headers().set(StompHeaders.DESTINATION, dest);

		handler.channelRead(ctx, f);

		// THEN
		assertThat("MESSAGE response provided", msgCaptor.getValue(), is(instanceOf(StompFrame.class)));
		StompFrame msg = (StompFrame) msgCaptor.getValue();
		assertThat("Response dest is SEND dest", msg.headers().getAsString(StompHeaders.DESTINATION),
				is(dest));
		assertThat("Response status is custom", msg.headers().getAsString(SetupHeader.Status.getValue()),
				is(String.valueOf(statusCode)));
		assertThat("Response message header provided", msg.headers().getAsString(StompHeaders.MESSAGE),
				is(message));
	}

}
