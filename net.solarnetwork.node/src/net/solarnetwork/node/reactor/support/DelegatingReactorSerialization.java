/* ==================================================================
 * DelegatingReactorSerialization.java - Mar 1, 2011 4:32:40 PM
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
 * $Id$
 * ==================================================================
 */

package net.solarnetwork.node.reactor.support;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.solarnetwork.node.reactor.Instruction;
import net.solarnetwork.node.reactor.ReactorSerializationService;

/**
 * {@link ReactorSerializationService} that delegates to a collection of
 * other {@link ReactorSerializationService}, making use of the first one
 * that handles a given encode / decode request.
 * 
 * <p>In each method of the {@link ReactorSerializationService} API, this
 * class will iterate over each configured delegate in the {@code serializers}
 * property, calling the same method on each delegate. If the delegate does
 * not throw an {@link IllegalArgumentException}, this class stops iterating
 * and returns the delgate's result immediately.</p>
 * 
 * <p>The idea of this class is that many different Reactor IO services can
 * be registered at once within the system to support different encodings
 * all at the same time. This class is assumed to be published as a service
 * with a higher ranking than all other {@link ReactorSerializationService}
 * instances, so that consumers of the service use this one instead of any
 * individual service directly.</p>
 * 
 * <p>The configurable properties of this class are:</p>
 * 
 * <dl class="class-properties">
 *   <dt>serializers</dt>
 *   <dd>Collection of {@link ReactorSerializationService} to delegate to.</dd>
 * </dl>
 * 
 * @author matt
 * @version $Revision$
 */
public class DelegatingReactorSerialization implements ReactorSerializationService {

	private Collection<ReactorSerializationService> serializers;

	private final Logger log = LoggerFactory.getLogger(getClass());
	
	@Override
	public List<Instruction> decodeInstructions(String instructorId, Object in, String type,
			Map<String, ?> properties) {
		for ( ReactorSerializationService delegate : serializers ) {
			try {
				return delegate.decodeInstructions(instructorId, in, type, properties);
			} catch ( IllegalArgumentException e ) {
				if ( log.isDebugEnabled() ) {
					log.debug("ReactorSerializationService " +delegate 
							+" does not support decoding " +type);
				}
			}
		}
		return null;
	}

	@Override
	public Object encodeInstructions(Collection<Instruction> instructions, String type,
			Map<String, ?> properties) {
		for ( ReactorSerializationService delegate : serializers ) {
			try {
				return delegate.encodeInstructions(instructions, type, properties);
			} catch ( IllegalArgumentException e ) {
				if ( log.isDebugEnabled() ) {
					log.debug("ReactorSerializationService " +delegate 
							+" does not support encoding " +type);
				}
			}
		}
		return null;
	}

	public Collection<ReactorSerializationService> getSerializers() {
		return serializers;
	}
	public void setSerializers(Collection<ReactorSerializationService> serializers) {
		this.serializers = serializers;
	}
	
}
