/* ==================================================================
 * WebsocketXorCsrfChannelProcessor.java - 17/06/2025 1:58:31 pm
 *
 * Copyright 2025 SolarNetwork.net Dev Team
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

import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.ExecutorSubscribableChannel;
import org.springframework.security.messaging.web.csrf.CsrfChannelInterceptor;
import org.springframework.security.messaging.web.csrf.XorCsrfChannelInterceptor;

/**
 * Bean post-processor to replace the default {@link CsrfChannelInterceptor}
 * configured by Spring Security 6's XML with the
 * {@link XorCsrfChannelInterceptor} that works with Spring 6.
 *
 * @author matt
 * @version 1.0
 */
public class WebsocketXorCsrfChannelBeanPostProcessor implements BeanPostProcessor {

	/**
	 * Constructor.
	 */
	public WebsocketXorCsrfChannelBeanPostProcessor() {
		super();
	}

	@Override
	public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
		if ( bean instanceof ExecutorSubscribableChannel ch ) {
			boolean replace = false;
			for ( ChannelInterceptor ci : ch.getInterceptors() ) {
				if ( ci instanceof CsrfChannelInterceptor ) {
					replace = true;
					break;
				}
			}
			if ( replace ) {
				List<ChannelInterceptor> newInterceptors = new ArrayList<>(ch.getInterceptors().size());
				for ( ChannelInterceptor ci : ch.getInterceptors() ) {
					if ( ci instanceof CsrfChannelInterceptor ) {
						newInterceptors.add(new XorCsrfChannelInterceptor());
					} else {
						newInterceptors.add(ci);
					}
				}
				ch.setInterceptors(newInterceptors);
				return bean;

			}
			for ( ListIterator<ChannelInterceptor> itr = ch.getInterceptors().listIterator(); itr
					.hasNext(); ) {
				ChannelInterceptor ci = itr.next();
				if ( ci instanceof CsrfChannelInterceptor ) {
					itr.remove();
					itr.add(new XorCsrfChannelInterceptor());
				}
			}
		}
		return bean;
	}

}
