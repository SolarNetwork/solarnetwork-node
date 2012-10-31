/* ==================================================================
 * BulkUploadResult.java - Feb 23, 2011 11:43:34 AM
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

package net.solarnetwork.node;

/**
 * Result object for a specific datum uploaded in bulk.
 * 
 * <p>The {@code id} value represents the remote ID received 
 * from the server for the given {@code datum}. This value
 * can be stored in {@link DatumUpload} objects as a receipt
 * for the upload transaction.</p>
 * 
 * @author matt
 * @version $Revision$
 */
public class BulkUploadResult {

	private final Datum datum;
	private final Long id;
	
	/**
	 * Constructor.
	 * 
	 * @param datum the datum
	 * @param id the ID
	 */
	public BulkUploadResult(Datum datum, Long id) {
		this.datum = datum;
		this.id = id;
	}

	/**
	 * @return the datum
	 */
	public Datum getDatum() {
		return datum;
	}

	/**
	 * @return the id
	 */
	public Long getId() {
		return id;
	}
	
}
