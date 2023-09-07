/* ==================================================================
 * ObjectMapperCustomizer.java - 8/09/2023 7:13:55 am
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

package net.solarnetwork.node.setup.web.support;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import net.solarnetwork.service.ObjectMapperService;

/**
 * Customize the web {@link ObjectMapper}.
 * 
 * @author matt
 * @version 1.0
 * @since 3.3
 */
public class ObjectMapperCustomizer implements ObjectMapperService {

	private final ObjectMapper mapper;

	/**
	 * Constructor.
	 * 
	 * @param service
	 *        the delegate service to use
	 */
	public ObjectMapperCustomizer(ObjectMapperService service) {
		super();

		ObjectMapper mapper = service.getObjectMapper().copy();

		SimpleModule m = new SimpleModule("SolarNode Web");
		m.addSerializer(BasicInstructionSerializer.INSTANCE);
		mapper.registerModule(m);

		this.mapper = mapper;
	}

	@Override
	public ObjectMapper getObjectMapper() {
		return mapper;
	}

}
