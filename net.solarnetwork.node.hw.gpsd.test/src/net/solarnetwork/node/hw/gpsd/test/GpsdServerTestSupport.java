/* ==================================================================
 * GpsdServerTestSupport.java - 13/11/2019 6:41:46 am
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

package net.solarnetwork.node.hw.gpsd.test;

import static java.util.Arrays.asList;
import java.net.InetSocketAddress;
import org.junit.After;
import org.junit.Before;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import net.solarnetwork.node.hw.gpsd.domain.VersionMessage;
import net.solarnetwork.node.hw.gpsd.util.GpsdMessageDeserializer;
import net.solarnetwork.util.ObjectMapperFactoryBean;

/**
 * Test support using a mock GpsdServer.
 * 
 * @author matt
 * @version 1.0
 */
public class GpsdServerTestSupport {

	public static final VersionMessage DEFAULT_GPSD_VERSION = VersionMessage.builder()
			.withRelease("1.0.0").withRevision("1").withProtocolMajor(3).withProtocolMinor(1).build();

	private EventLoopGroup gpsdBossGroup;
	private EventLoopGroup gpsdWorkerGroup;
	private Channel gpsdChannel;
	private ObjectMapper mapper;

	/**
	 * Setup this class.
	 * 
	 * <p>
	 * This method will create an {@link ObjectMapper} that can be obtained via
	 * {@link #getMapper()}.
	 * </p>
	 */
	@Before
	public void setup() {
		mapper = createObjectMapper();
	}

	@After
	public void teardown() {
		if ( gpsdBossGroup != null ) {
			gpsdBossGroup.shutdownGracefully();
		}
		if ( gpsdWorkerGroup != null ) {
			gpsdWorkerGroup.shutdownGracefully();
		}
	}

	private ObjectMapper createObjectMapper() {
		net.solarnetwork.util.ObjectMapperFactoryBean factory = new ObjectMapperFactoryBean();
		factory.setFeaturesToDisable(asList(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES));
		factory.setFeaturesToEnable(asList(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS));
		factory.setDeserializers(asList(new GpsdMessageDeserializer()));
		try {
			return factory.getObject();
		} catch ( Exception e ) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Get the {@link ObjectMapper} previously created via the {@link #setup}
	 * method.
	 * 
	 * @return the mapper, or {@literal null}
	 */
	protected ObjectMapper getMapper() {
		return mapper;
	}

	/**
	 * Configure the GPSd mock server using {@link #DEFAULT_GPSD_VERSION}.
	 */
	protected synchronized void setupGpsdServer() {
		setupGpsdServer(DEFAULT_GPSD_VERSION);
	}

	/**
	 * Configure the GPSd mock server.
	 * 
	 * @param version
	 *        the version to use
	 */
	protected synchronized void setupGpsdServer(VersionMessage version) {
		if ( gpsdChannel != null ) {
			return;
		}
		if ( mapper == null ) {
			mapper = createObjectMapper();
		}
		gpsdBossGroup = new NioEventLoopGroup(1);
		gpsdWorkerGroup = new NioEventLoopGroup();

		ServerBootstrap b = new ServerBootstrap();
		b.group(gpsdBossGroup, gpsdWorkerGroup).channel(NioServerSocketChannel.class)
				.handler(new LoggingHandler(LogLevel.INFO))
				.childHandler(new GpsdServerInitializer(new GpsdServerHandler(version, mapper)));

		try {
			gpsdChannel = b.bind(0).sync().channel();
		} catch ( InterruptedException e ) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Get the port number the GPSd server is running on.
	 * 
	 * @return the port, or {@literal -1} if the server is not running
	 */
	protected int gpsdServerPort() {
		return (gpsdChannel != null ? ((InetSocketAddress) gpsdChannel.localAddress()).getPort() : -1);
	}
}
