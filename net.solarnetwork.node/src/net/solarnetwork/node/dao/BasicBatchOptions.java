/* ==================================================================
 * BasicBatchOptions.java - Nov 5, 2012 11:19:05 AM
 * 
 * Copyright 2007-2012 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.dao;

import java.util.Map;

import net.solarnetwork.node.dao.BatchableDao.BatchOptions;

/**
 * Basic implementation of {@link BatchOptions}.
 * 
 * @author matt
 * @version 1.0
 */
public class BasicBatchOptions implements BatchOptions {
	
	public static final String DEFAULT_BATCH_NAME = "Anonymous";
	public static final int DEFAULT_BATCH_SIZE = 50;
	
	private String name;
	private boolean updatable = false;
	private int batchSize = DEFAULT_BATCH_SIZE;
	private Map<String, Object> parameters;

	/**
	 * Default constructor.
	 */
	public BasicBatchOptions() {
		this(DEFAULT_BATCH_NAME);
	}
	
	/**
	 * Construct with a name.
	 * 
	 * @param name the name
	 */
	public BasicBatchOptions(String name) {
		this(name, DEFAULT_BATCH_SIZE, false, null);
	}
	
	/**
	 * Construct with values.
	 * 
	 * @param name the name
	 * @param batchSize the size
	 * @param updatable updatable
	 * @param parameters the parameters
	 */
	public BasicBatchOptions(String name, int batchSize, boolean updatable, Map<String, Object> parameters) {
		super();
		this.name = name;
		this.batchSize = batchSize;
		this.updatable = updatable;
		this.parameters = parameters;
	}
	
	@Override
	public String getName() {
		return name;
	}

	@Override
	public int getBatchSize() {
		return batchSize;
	}

	@Override
	public boolean isUpdatable() {
		return updatable;
	}

	@Override
	public Map<String, Object> getParameters() {
		return parameters;
	}

}
