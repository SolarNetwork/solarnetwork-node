/* ==================================================================
 * UploadServiceDatumDao.java - 7/06/2018 12:17:55 PM
 * 
 * Copyright 2018 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.upload.mqtt;

import java.io.IOException;
import java.util.Date;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.solarnetwork.node.UploadService;
import net.solarnetwork.node.dao.DatumDao;
import net.solarnetwork.node.domain.Datum;
import net.solarnetwork.util.OptionalService;

/**
 * {@link DatumDao} that delegates to another {@link DatumDao} only when an
 * {@link UploadService} fails to upload a datum.
 * 
 * @param T
 *        the datum type
 * @author matt
 * @version 1.2
 */
public class UploadServiceDatumDao<T extends Datum> implements DatumDao<T> {

	private final DatumDao<T> delegate;
	private final UploadService uploadService;

	private final Logger log = LoggerFactory.getLogger(getClass());

	/**
	 * Constructor.
	 * 
	 * @param uploadService
	 *        the upload service
	 * @param delegate
	 *        the delegate DAO
	 */
	public UploadServiceDatumDao(UploadService uploadService, DatumDao<T> delegate) {
		super();
		this.delegate = delegate;
		this.uploadService = uploadService;
	}

	/**
	 * Gemini Blueprint hack constructor.
	 * 
	 * <p>
	 * This constructor exists to work around a Gemini Blueprint bug where it
	 * complains about setting the DatumDao&lt;T&gt; argument due to some
	 * reified type error.
	 * </p>
	 * 
	 * @param delegate
	 *        the delegate DAO; will be cast to DatumDao&lt;T&gt;
	 * @param uploadService
	 *        the upload service
	 * @param yesReally
	 *        unused
	 * @see #UploadServiceDatumDao(UploadService, DatumDao, OptionalService)
	 */
	@SuppressWarnings("unchecked")
	public UploadServiceDatumDao(Object delegate, UploadService uploadService, boolean yesReally) {
		this(uploadService, (DatumDao<T>) delegate);
	}

	@Override
	public Class<? extends T> getDatumType() {
		return delegate.getDatumType();
	}

	@Override
	public void storeDatum(T datum) {
		try {
			String id = uploadService.uploadDatum(datum);
			if ( id != null ) {
				// datum posted, no need to persist locally
				return;
			}
		} catch ( RuntimeException e ) {
			Throwable root = e;
			while ( root.getCause() != null ) {
				root = root.getCause();
			}
			if ( root instanceof IOException ) {
				log.info("Communication error posting datum {}; persisting to upload later: {}", datum,
						root.getMessage());
			} else {
				log.warn("Error posting datum {}; persisting to upload later: {}", datum,
						root.getMessage(), e);
			}
		}
		delegate.storeDatum(datum);
	}

	@Override
	public List<T> getDatumNotUploaded(String destination) {
		return delegate.getDatumNotUploaded(destination);
	}

	@Override
	public void setDatumUploaded(T datum, Date date, String destination, String trackingId) {
		delegate.setDatumUploaded(datum, date, destination, trackingId);
	}

	@Override
	public int deleteUploadedDataOlderThan(int hours) {
		return delegate.deleteUploadedDataOlderThan(hours);
	}

}
