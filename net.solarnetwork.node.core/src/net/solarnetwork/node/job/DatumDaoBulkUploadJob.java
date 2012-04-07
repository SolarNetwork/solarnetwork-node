/* ==================================================================
 * DatumDaoBulkUploadJob.java - Feb 23, 2011 1:22:17 PM
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

package net.solarnetwork.node.job;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import net.solarnetwork.node.BulkUploadResult;
import net.solarnetwork.node.BulkUploadService;
import net.solarnetwork.node.Datum;
import net.solarnetwork.node.dao.DatumDao;

import org.quartz.JobExecutionContext;
import org.quartz.StatefulJob;

/**
 * Job to query a collection of {@link DatumDao} instances for data to upload
 * via a {@link BulkUploadService}.
 * 
 * <p>This job will call {@link DatumDao#getDatumNotUploaded(String)} for each
 * configured {@link DatumDao} and combine them into a single collection to pass
 * to {@link BulkUploadService#uploadBulkDatum(java.util.Collection)}. For each
 * non-null {@link BulkUploadResult#getId()} tracking ID returned, the associated
 * {@link BulkUploadResult#getDatum()} will be passed to the appropriate 
 * {@link DatumDao} instance's 
 * {@link DatumDao#storeDatumUpload(Datum, String, Long)} method.</p>
 * 
 * <p>The configurable properties of this class are:</p>
 * 
 * <dl class="class-properties">
 *   <dt>daos</dt>
 *   <dd>A collection of {@link DatumDao} instances to collect Datum objects from
 *   that need uploading, and to then store the uploaded tracking IDs with.</dd>
 *   
 *   <dt>uploadService</dt>
 *   <dd>The {@link BulkUploadService} to upload the data with.</dd>
 * </dl>
 * 
 * @author matt
 * @version $Revision$
 */
public class DatumDaoBulkUploadJob extends AbstractJob
implements StatefulJob {

	private Collection<DatumDao<Datum>> daos;
	private BulkUploadService uploadService;
	
	@Override
	protected void executeInternal(JobExecutionContext jobContext)
			throws Exception {
		Map<Class<? extends Datum>,DatumDao<Datum>> daoMapping
			= new LinkedHashMap<Class<? extends Datum>, DatumDao<Datum>>(daos.size());
		
		List<Datum> uploadList = new ArrayList<Datum>();
		
		for ( DatumDao<Datum> datumDao : daos ) {
			if ( log.isDebugEnabled() ) {
				log.debug("Collecting [{}] data to bulk upload to [{}]",
						datumDao.getDatumType().getSimpleName(), uploadService.getKey());
			}
			
			daoMapping.put(datumDao.getDatumType(), datumDao);
			
			List<Datum> toUpload = datumDao.getDatumNotUploaded(
					uploadService.getKey());
			
			if ( log.isDebugEnabled() ) {
				log.debug("Found " + toUpload.size() +" [" 
						+datumDao.getDatumType().getSimpleName() 
						+"] data to bulk upload to [" +uploadService.getKey() +']');
			}
			
			uploadList.addAll(toUpload);
		}
		if ( uploadList.size() < 1 ) {
			if ( log.isDebugEnabled() ) {
				log.debug("Collected {} datum to bulk upload to [{}]", uploadList.size(), 
						uploadService.getKey());
			}
			return;
		}
		if ( log.isInfoEnabled() ) {
			log.info("Collected {} datum to bulk upload to [{}]", uploadList.size(), 
					uploadService.getKey());
		}
		try {
			int count = 0;
			List<BulkUploadResult> results = uploadService.uploadBulkDatum(uploadList);
			for ( BulkUploadResult result : results ) {
				Long tid = result.getId();
				if ( log.isTraceEnabled() ) {
					log.trace("Bulk uploaded [{}] [{}] and received tid [{}]",
						new Object[] {result.getDatum().getClass().getSimpleName(),
						result.getDatum().getId(), tid});
				}
				
				if ( tid != null ) {
					DatumDao<Datum> datumDao = daoMapping.get(result.getDatum().getClass());
					datumDao.storeDatumUpload(result.getDatum(), uploadService.getKey(), tid);
					count++;
				}
			}
			if ( log.isInfoEnabled() ) {
				log.info("Bulk uploaded {} objects to [{}]", count, uploadService.getKey());
			}
		} catch ( RuntimeException e ) {
			Throwable root = e;
			while ( root.getCause() != null ) {
				root = root.getCause();
			}
			if ( root instanceof IOException ) {
				if ( log.isWarnEnabled() ) {
					log.warn("Network problem posting data: {}" +root.getMessage());
				}
			} else {
				if ( log.isErrorEnabled() ) {
					log.error("Exception posting data", root);
				}
			}
		}
	}

	/**
	 * @param daos the daos to set
	 */
	public void setDaos(Collection<DatumDao<Datum>> daos) {
		this.daos = daos;
	}

	/**
	 * @param uploadService the uploadService to set
	 */
	public void setUploadService(BulkUploadService uploadService) {
		this.uploadService = uploadService;
	}

}
