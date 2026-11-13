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
import net.solarnetwork.codec.CborUtils;
import net.solarnetwork.service.ObjectMapperService;

/**
 * Customize the web {@link ObjectMapper}.
 * 
 * <p>
 * A CBOR copy of the customized mapper is also provided, so CBOR responses have
 * the same shape as JSON responses.
 * </p>
 * 
 * @author matt
 * @version 1.1
 * @since 3.3
 */
public class ObjectMapperCustomizer implements ObjectMapperService {

	private final ObjectMapper mapper;
	private final ObjectMapper cborMapper;

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
		this.cborMapper = mapper.copyWith(CborUtils.cborFactory());
	}

	@Override
	public ObjectMapper getObjectMapper() {
		return mapper;
	}

	/**
	 * Get the CBOR mapper.
	 * 
	 * <p>
	 * This mapper has the same configuration and modules as
	 * {@link #getObjectMapper()}, using a CBOR factory.
	 * </p>
	 * 
	 * @return the CBOR mapper
	 * @since 1.1
	 */
	public ObjectMapper getCborObjectMapper() {
		return cborMapper;
	}

}
