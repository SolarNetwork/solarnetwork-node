/* ==================================================================
 * SessionAuthenticationStrategyFactoryBean.java - 31/10/2019 10:24:58 am
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

package net.solarnetwork.node.setup.web.support;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.security.web.authentication.session.CompositeSessionAuthenticationStrategy;
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy;

/**
 * Factory bean to work around getting access to framework-generated
 * {@link SessionAuthenticationStrategy}.
 * 
 * @author matt
 * @version 1.0
 * @since 1.41
 * @see https://github.com/spring-projects/spring-security/issues/3995
 */
public class SessionAuthenticationStrategyFactoryBean
		implements BeanFactoryAware, FactoryBean<SessionAuthenticationStrategy> {

	private BeanFactory beanFactory;

	@Override
	public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
		this.beanFactory = beanFactory;
	}

	@Override
	public SessionAuthenticationStrategy getObject() throws Exception {
		final CompositeSessionAuthenticationStrategy sas = beanFactory
				.getBean(CompositeSessionAuthenticationStrategy.class);
		return sas;
	}

	@Override
	public Class<?> getObjectType() {
		return SessionAuthenticationStrategy.class;
	}

	@Override
	public boolean isSingleton() {
		return true;
	}

}
