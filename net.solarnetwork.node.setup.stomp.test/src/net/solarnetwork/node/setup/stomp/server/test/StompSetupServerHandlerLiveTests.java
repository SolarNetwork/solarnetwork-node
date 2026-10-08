/* ==================================================================
 * StompSetupServerHandlerLiveTests.java - 10/10/2026 1:42:00 PM
 *
 * Copyright 2026 SolarNetwork.net Dev Team
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
import static org.easymock.EasyMock.capture;
import static org.easymock.EasyMock.expect;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.springframework.security.core.userdetails.User.withUsername;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.easymock.Capture;
import org.easymock.EasyMock;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.util.AntPathMatcher;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.DefaultChannelPromise;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.handler.codec.stomp.DefaultStompFrame;
import io.netty.handler.codec.stomp.StompCommand;
import io.netty.handler.codec.stomp.StompFrame;
import io.netty.handler.codec.stomp.StompHeaders;
import io.netty.handler.codec.stomp.StompSubframeAggregator;
import io.netty.handler.codec.stomp.StompSubframeDecoder;
import io.netty.handler.codec.stomp.StompSubframeEncoder;
import io.netty.util.concurrent.ImmediateEventExecutor;
import net.solarnetwork.domain.datum.DatumSamples;
import net.solarnetwork.node.domain.datum.SimpleDatum;
import net.solarnetwork.node.reactor.InstructionHandler;
import net.solarnetwork.node.reactor.SimpleInstructionExecutionService;
import net.solarnetwork.node.service.DatumEvents;
import net.solarnetwork.node.setup.UserAuthenticationInfo;
import net.solarnetwork.node.setup.UserService;
import net.solarnetwork.node.setup.stomp.LiveHeader;
import net.solarnetwork.node.setup.stomp.SetupHeader;
import net.solarnetwork.node.setup.stomp.SetupStatus;
import net.solarnetwork.node.setup.stomp.SetupTopic;
import net.solarnetwork.node.setup.stomp.StompUtils;
import net.solarnetwork.node.setup.stomp.server.LiveDatumService;
import net.solarnetwork.node.setup.stomp.server.SetupSession;
import net.solarnetwork.node.setup.stomp.server.StompSetupServerHandler;
import net.solarnetwork.node.setup.stomp.server.StompSetupServerService;
import net.solarnetwork.service.StaticOptionalService;
import net.solarnetwork.test.CallingThreadExecutorService;

/**
 * Test cases for the live datum support of the {@link StompSetupServerHandler}
 * class.
 *
 * @author elijah
 * @version 1.0
 */
public class StompSetupServerHandlerLiveTests {

	private static final String TEST_LOGIN = "foo";
	private static final String BCRYPT_ALG = "bcrypt";
	private static final String SALT_PARAM = "salt";

	private UserService userService;
	private UserDetailsService userDetailsService;
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
		instructionHandler = EasyMock.createMock(InstructionHandler.class);
		serverService = new StompSetupServerService(userService, userDetailsService,
				new AntPathMatcher(),
				new SimpleInstructionExecutionService(singletonList(instructionHandler)));
		objectMapper = new ObjectMapper();
		ctx = EasyMock.createMock(ChannelHandlerContext.class);
		channel = EasyMock.createMock(Channel.class);
		sessions = new ConcurrentHashMap<>(4, 0.9f, 1);
		handler = new StompSetupServerHandler(sessions, serverService, objectMapper,
				new CallingThreadExecutorService());
	}

	@After
	public void teardown() {
		EasyMock.verify(userService, userDetailsService, instructionHandler, ctx, channel);
		if ( liveChannel != null ) {
			liveChannel.finishAndReleaseAll();
		}
	}

	private void replayAll() {
		EasyMock.replay(userService, userDetailsService, instructionHandler, ctx, channel);
	}

	private static final String LIVE_SOURCE_ID = "/GEN/1";

	private TestOperationalModesService opModes;
	private LiveDatumService liveService;
	private EmbeddedChannel liveChannel;

	private SetupSession givenLiveSupport() {
		return givenLiveSupport(new EmbeddedChannel());
	}

	private SetupSession givenLiveSupport(EmbeddedChannel ch) {
		MutableClock clock = new MutableClock(Instant.now());
		opModes = new TestOperationalModesService(clock);
		liveService = new LiveDatumService(new StaticOptionalService<>(opModes),
				new StaticOptionalService<>(null), objectMapper);
		liveService.setClock(clock);
		liveService.setTaskScheduler(new TestTaskScheduler());
		serverService.setLiveDatumService(liveService);

		liveChannel = ch;
		String[] roles = new String[] { "ROLE_USER" };
		UserDetails user = withUsername(TEST_LOGIN).password("pw").authorities(roles).build();
		SetupSession session = new SetupSession(TEST_LOGIN, liveChannel);
		session.setAuthentication(new TestingAuthenticationToken(user, null, roles));
		sessions.put(session.getSessionId(), session);
		return session;
	}

	private DefaultStompFrame liveSubscribeFrame(String subId) {
		DefaultStompFrame f = new DefaultStompFrame(StompCommand.SUBSCRIBE);
		f.headers().set(StompHeaders.ID, subId);
		f.headers().set(StompHeaders.DESTINATION, SetupTopic.DatumLive.getValue());
		f.headers().set(LiveHeader.SourceId.getValue(), LIVE_SOURCE_ID);
		f.headers().set(LiveHeader.Properties.getValue(), "watts,current");
		f.headers().set(LiveHeader.Interval.getValue(), "1000");
		return f;
	}

	private StompFrame nextLiveFrame() {
		Object o = liveChannel.readOutbound();
		assertThat("Frame written to live channel", o, is(instanceOf(StompFrame.class)));
		return (StompFrame) o;
	}

	private void publishLiveDatum() {
		DatumSamples s = new DatumSamples();
		s.putInstantaneousSampleValue("watts", 1234);
		s.putInstantaneousSampleValue("current", 5.6f);
		liveService.handleEvent(DatumEvents
				.datumCapturedEvent(SimpleDatum.nodeDatum(LIVE_SOURCE_ID, Instant.now(), s)));
	}


	@Test
	public void subscribeLive_ok() {
		// GIVEN
		final SetupSession session = givenLiveSupport();
		expect(ctx.channel()).andReturn(liveChannel);

		// WHEN
		replayAll();
		handler.channelRead(ctx, liveSubscribeFrame("live-1"));

		// THEN
		assertThat("Live subscription registered", liveService.getSubscriptionCount(), is(1));
		assertThat("Live subscription NOT added to session subscriptions",
				session.subscriptionIdsForTopic(SetupTopic.DatumLive.getValue(),
						serverService.getPathMatcher()),
				is(empty()));
		assertThat("No reply without receipt", liveChannel.readOutbound(), is(nullValue()));

		// WHEN datum captured
		publishLiveDatum();

		// THEN
		StompFrame msg = nextLiveFrame();
		assertThat("Live MESSAGE", msg.command(), is(StompCommand.MESSAGE));
		assertThat("Subscription", msg.headers().getAsString(StompHeaders.SUBSCRIPTION),
				is("live-1"));
		assertThat("Status OK", msg.headers().getAsString(SetupHeader.Status.getValue()),
				is(String.valueOf(SetupStatus.Ok.getCode())));
	}

	@Test
	public void subscribeLive_missingSource_unprocessable_noClose() {
		// GIVEN
		givenLiveSupport();
		expect(ctx.channel()).andReturn(liveChannel);

		// WHEN
		replayAll();
		DefaultStompFrame f = liveSubscribeFrame("live-1");
		f.headers().remove(LiveHeader.SourceId.getValue());
		handler.channelRead(ctx, f);

		// THEN
		StompFrame msg = nextLiveFrame();
		assertThat("Status MESSAGE, not ERROR", msg.command(), is(StompCommand.MESSAGE));
		assertThat("Status 422", msg.headers().getAsString(SetupHeader.Status.getValue()),
				is(String.valueOf(SetupStatus.Unprocessable.getCode())));
		assertThat("Channel still open", liveChannel.isActive(), is(true));
		assertThat("Not registered", liveService.getSubscriptionCount(), is(0));
	}

	@Test
	public void subscribeLive_tooMany() {
		// GIVEN
		givenLiveSupport();
		liveService.setMaxSubscriptionsPerSession(1);
		expect(ctx.channel()).andReturn(liveChannel).times(2);

		// WHEN
		replayAll();
		handler.channelRead(ctx, liveSubscribeFrame("live-1"));
		handler.channelRead(ctx, liveSubscribeFrame("live-2"));

		// THEN
		StompFrame msg = nextLiveFrame();
		assertThat("Status 429", msg.headers().getAsString(SetupHeader.Status.getValue()),
				is(String.valueOf(SetupStatus.TooManyRequests.getCode())));
		assertThat("Rejected subscription ID",
				msg.headers().getAsString(StompHeaders.SUBSCRIPTION), is("live-2"));
		assertThat("One registered", liveService.getSubscriptionCount(), is(1));
	}

	@Test
	public void subscribeLive_noLiveService() {
		// GIVEN
		givenLiveSupport();
		serverService.setLiveDatumService(null);
		expect(ctx.channel()).andReturn(liveChannel);

		// WHEN
		replayAll();
		handler.channelRead(ctx, liveSubscribeFrame("live-1"));

		// THEN
		StompFrame msg = nextLiveFrame();
		assertThat("Status 503", msg.headers().getAsString(SetupHeader.Status.getValue()),
				is(String.valueOf(SetupStatus.ServiceUnavailable.getCode())));
		assertThat("Subscription", msg.headers().getAsString(StompHeaders.SUBSCRIPTION),
				is("live-1"));
	}

	@Test
	public void subscribeLive_withReceipt() {
		// GIVEN
		givenLiveSupport();
		expect(ctx.channel()).andReturn(liveChannel);
		Capture<Object> receiptCaptor = Capture.newInstance();
		expect(ctx.writeAndFlush(capture(receiptCaptor)))
				.andReturn(new DefaultChannelPromise(channel));

		// WHEN
		replayAll();
		DefaultStompFrame f = liveSubscribeFrame("live-1");
		f.headers().set(StompHeaders.RECEIPT, "r-1");
		handler.channelRead(ctx, f);

		// THEN
		StompFrame receipt = (StompFrame) receiptCaptor.getValue();
		assertThat("RECEIPT frame", receipt.command(), is(StompCommand.RECEIPT));
		assertThat("Receipt ID", receipt.headers().getAsString(StompHeaders.RECEIPT_ID),
				is("r-1"));
	}

	@Test
	public void wildcardSubscriber_neverGetsLiveFrames() {
		// GIVEN
		final SetupSession session = givenLiveSupport();
		session.addSubscription("0", "/setup/**");
		expect(ctx.channel()).andReturn(liveChannel);

		// WHEN
		replayAll();
		handler.channelRead(ctx, liveSubscribeFrame("live-1"));
		publishLiveDatum();

		// THEN
		StompFrame msg = nextLiveFrame();
		assertThat("Only the live subscription receives the frame",
				msg.headers().getAsString(StompHeaders.SUBSCRIPTION), is("live-1"));
		assertThat("No other frames", liveChannel.readOutbound(), is(nullValue()));
	}

	@Test
	public void unsubscribe_live() {
		// GIVEN
		givenLiveSupport();
		expect(ctx.channel()).andReturn(liveChannel).times(2);

		// WHEN
		replayAll();
		handler.channelRead(ctx, liveSubscribeFrame("live-1"));
		DefaultStompFrame f = new DefaultStompFrame(StompCommand.UNSUBSCRIBE);
		f.headers().set(StompHeaders.ID, "live-1");
		handler.channelRead(ctx, f);

		// THEN
		assertThat("Live subscription removed", liveService.getSubscriptionCount(), is(0));
		publishLiveDatum();
		assertThat("No frames after unsubscribe", liveChannel.readOutbound(), is(nullValue()));
	}

	@Test
	public void unsubscribe_live_receiptQueuedOnEventLoop() {
		// GIVEN
		givenLiveSupport();
		expect(ctx.channel()).andReturn(liveChannel).times(2);

		// receipt is deferred to the event loop, after any queued live message
		expect(ctx.executor()).andReturn(ImmediateEventExecutor.INSTANCE);
		Capture<Object> receiptCaptor = Capture.newInstance();
		expect(ctx.writeAndFlush(capture(receiptCaptor)))
				.andReturn(new DefaultChannelPromise(channel));

		// WHEN
		replayAll();
		handler.channelRead(ctx, liveSubscribeFrame("live-1"));
		DefaultStompFrame f = new DefaultStompFrame(StompCommand.UNSUBSCRIBE);
		f.headers().set(StompHeaders.ID, "live-1");
		f.headers().set(StompHeaders.RECEIPT, "r-2");
		handler.channelRead(ctx, f);

		// THEN
		StompFrame receipt = (StompFrame) receiptCaptor.getValue();
		assertThat("RECEIPT frame", receipt.command(), is(StompCommand.RECEIPT));
		assertThat("Receipt ID", receipt.headers().getAsString(StompHeaders.RECEIPT_ID),
				is("r-2"));
		assertThat("Live subscription removed", liveService.getSubscriptionCount(), is(0));
	}

	@Test
	public void subscribeLive_sourceIdPassedAsDecodedByNetty() {
		// GIVEN a source ID with characters STOMP escapes, as unescaped by Netty
		final String sourceId = "C:\\new";
		givenLiveSupport();
		expect(ctx.channel()).andReturn(liveChannel);

		// WHEN
		replayAll();
		DefaultStompFrame f = liveSubscribeFrame("live-1");
		f.headers().set(LiveHeader.SourceId.getValue(), sourceId);
		handler.channelRead(ctx, f);
		DatumSamples s = new DatumSamples();
		s.putInstantaneousSampleValue("watts", 1);
		liveService.handleEvent(DatumEvents
				.datumCapturedEvent(SimpleDatum.nodeDatum(sourceId, Instant.now(), s)));

		// THEN
		StompFrame msg = nextLiveFrame();
		assertThat("Datum for exact source ID delivered",
				msg.headers().getAsString(LiveHeader.SourceId.getValue()), is(sourceId));
	}

	@Test
	public void channelClose_removesLiveSubscriptions() {
		// GIVEN
		givenLiveSupport();
		expect(ctx.channel()).andReturn(channel).anyTimes();

		final String salt = BCrypt.gensalt();
		expect(userService.authenticationInfo(TEST_LOGIN))
				.andReturn(new UserAuthenticationInfo(BCRYPT_ALG, singletonMap(SALT_PARAM, salt)));
		DefaultChannelPromise closeFuture = new DefaultChannelPromise(channel,
				ImmediateEventExecutor.INSTANCE);
		expect(channel.closeFuture()).andReturn(closeFuture);
		Capture<Object> responseCaptor = Capture.newInstance();
		expect(ctx.writeAndFlush(EasyMock.capture(responseCaptor)))
				.andReturn(new DefaultChannelPromise(channel));
		expect(channel.isActive()).andReturn(true).anyTimes();

		// WHEN
		replayAll();
		DefaultStompFrame f = new DefaultStompFrame(StompCommand.CONNECT);
		f.headers().set(StompHeaders.ACCEPT_VERSION, "1.2");
		f.headers().set(StompHeaders.HOST, "localhost");
		f.headers().set(StompHeaders.LOGIN, TEST_LOGIN);
		handler.channelRead(ctx, f);

		StompFrame connected = (StompFrame) responseCaptor.getValue();
		SetupSession session = sessions
				.get(UUID.fromString(connected.headers().getAsString(StompHeaders.SESSION)));
		liveService.subscribe(session, "live-1", LIVE_SOURCE_ID, "watts", null);
		assertThat("Live subscription registered", liveService.getSubscriptionCount(), is(1));

		closeFuture.setSuccess();

		// THEN
		assertThat("Session removed", sessions.containsKey(session.getSessionId()), is(false));
		assertThat("Live subscriptions removed", liveService.getSubscriptionCount(), is(0));
	}

	private void writeWire(EmbeddedChannel ch, String frame) {
		ch.writeInbound(Unpooled.copiedBuffer(frame, StompUtils.UTF8));
	}

	private String readWire(EmbeddedChannel ch) {
		StringBuilder buf = new StringBuilder();
		Object o;
		while ( (o = ch.readOutbound()) != null ) {
			ByteBuf b = (ByteBuf) o;
			buf.append(b.toString(StompUtils.UTF8));
			b.release();
		}
		return buf.toString();
	}

	@Test
	public void liveSubscriptionId_roundTripsThroughCodec() {
		// GIVEN the same pipeline as the server, with the real STOMP codec
		givenLiveSupport(new EmbeddedChannel(new StompSubframeDecoder(),
				new StompSubframeAggregator(4096), new StompSubframeEncoder(), handler));
		replayAll();

		// WHEN a client subscribes with the ID a\nb (a, backslash, n, b), which STOMP
		// escapes on the wire as a\\nb
		writeWire(liveChannel, "SUBSCRIBE\nid:a\\\\nb\ndestination:/setup/datum/live\n"
				+ "source-id:" + LIVE_SOURCE_ID + "\nproperties:watts\n\n\0");
		publishLiveDatum();

		// THEN the MESSAGE carries the same ID, escaped once, so the client decodes it
		// back to a\nb
		String wire = readWire(liveChannel);
		assertThat("MESSAGE frame sent", wire, containsString("MESSAGE\n"));
		assertThat("Subscription ID round trips", wire, containsString("\nsubscription:a\\\\nb\n"));

		// WHEN the client unsubscribes with the same ID
		writeWire(liveChannel, "UNSUBSCRIBE\nid:a\\\\nb\n\n\0");

		// THEN
		assertThat("Live subscription removed", liveService.getSubscriptionCount(), is(0));
	}

}
