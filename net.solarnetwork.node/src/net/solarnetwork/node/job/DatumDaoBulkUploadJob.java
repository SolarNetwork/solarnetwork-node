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
 */

package net.solarnetwork.node.job;

import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.io.IOException;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import net.solarnetwork.node.dao.DatumDao;
import net.solarnetwork.node.domain.datum.NodeDatum;
import net.solarnetwork.node.service.BulkUploadResult;
import net.solarnetwork.node.service.BulkUploadService;
import net.solarnetwork.node.service.support.BaseIdentifiable;
import net.solarnetwork.node.setup.SetupException;
import net.solarnetwork.settings.SettingSpecifier;

/**
 * Job to query a {@link DatumDao} for datum to upload via a
 * {@link BulkUploadService}.
 * 
 * <p>
 * This job will call {@link DatumDao#getDatumNotUploaded(String)} for datum to
 * upload and pass them to
 * {@link BulkUploadService#uploadBulkDatum(java.util.Collection)}. For each
 * non-null {@link BulkUploadResult#getId()} tracking ID returned, the
 * associated {@link BulkUploadResult#getDatum()} will be passed to
 * {@link DatumDao#setDatumUploaded(NodeDatum, Instant, String, String)} method
 * so it can be marked as uploaded.
 * </p>
 * 
 * @author matt
 * @version 3.0
 */
public class DatumDaoBulkUploadJob extends BaseIdentifiable implements JobService {

	private final DatumDao dao;
	private final BulkUploadService uploadService;

	/**
	 * Constructor.
	 * 
	 * @param dao
	 *        the DAO to use
	 * @param uploadService
	 *        the upload service to use
	 * @throws IllegalArgumentException
	 *         if any argument is {@literal null}
	 */
	public DatumDaoBulkUploadJob(DatumDao dao, BulkUploadService uploadService) {
		super();
		this.dao = requireNonNullArgument(dao, "dao");
		this.uploadService = requireNonNullArgument(uploadService, "uploadService");
	}

	@Override
	public String getSettingUid() {
		String uid = getUid();
		return (uid != null && !uid.isEmpty() ? uid : "net.solarnetwork.node.job.DatumDaoBulkUploadJob");
	}

	@Override
	public List<SettingSpecifier> getSettingSpecifiers() {
		return Collections.emptyList();
	}

	@Override
	public void executeJobService() throws Exception {
		if ( log.isDebugEnabled() ) {
			log.debug("Collecting datum to bulk upload to [{}]", uploadService.getKey());
		}

		List<NodeDatum> toUpload = dao.getDatumNotUploaded(uploadService.getKey());

		if ( log.isDebugEnabled() ) {
			log.debug("Found {} datum to bulk upload to [{}]", toUpload.size(), uploadService.getKey());
		}

		final Instant uploadDate = Instant.now();
		try {
			int count = 0;
			List<BulkUploadResult> results = uploadService.uploadBulkDatum(toUpload);
			if ( results != null ) {
				for ( BulkUploadResult result : results ) {
					String tid = result.getId();
					if ( log.isTraceEnabled() ) {
						log.trace("Bulk uploaded [{} {}] [{}] and received tid [{}]",
								new Object[] { result.getDatum().getClass().getSimpleName(),
										result.getDatum().getTimestamp(),
										result.getDatum().getSourceId(), tid });
					}

					if ( tid != null ) {
						dao.setDatumUploaded(result.getDatum(), uploadDate, uploadService.getKey(), tid);
						count++;
					}
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
					log.warn("Network problem posting data ({}): {}", root.getClass().getSimpleName(),
							root.getMessage());
				}
			} else if ( root instanceof SetupException ) {
				log.warn("Unable to post data: {}", root.getMessage());
			} else {
				if ( log.isErrorEnabled() ) {
					log.error("Exception posting data", root);
				}
			}
		}
	}

}
