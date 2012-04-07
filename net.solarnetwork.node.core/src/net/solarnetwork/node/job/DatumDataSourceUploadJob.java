/* ===================================================================
 * DatumDataSourceUploadJob.java
 * 
 * Created Dec 3, 2009 10:58:30 AM
 * 
 * Copyright 2007-2009 SolarNetwork.net Dev Team
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
 * ===================================================================
 * $Id$
 * ===================================================================
 */

package net.solarnetwork.node.job;

import net.solarnetwork.node.Datum;
import net.solarnetwork.node.DatumDataSource;
import net.solarnetwork.node.UploadService;

import org.quartz.JobExecutionContext;
import org.quartz.StatefulJob;

/**
 * Job that obtains a {@link Datum} from a {@link DatumDataSource} and then
 * uploads it immediately via {@link UploadService}, without persisting the
 * Datum locally first.
 * 
 * <p>This job can be used to collect data such as weather where the
 * resolution of the data is not very fine and persisting it locally would
 * be overkill.</p>
 *
 * <p>The configurable properties of this class are:</p>
 * 
 * <dl class="class-properties">
 *   <dt>datumDataSource</dt>
 *   <dd>The {@link DatumDataSource} to collect the data from. The 
 *   {@link DatumDataSource#readCurrentDatum()} method will be called
 *   to get the currently available data.</dd>
 *   
 *   <dt>uploadService</dt>
 *   <dd>The {@link UploadService} implementation to use to upload the
 *   datum to.</dd>
 * </dl>
 *
 * @param <T> the Datum type for this job
 * @author matt
 * @version $Revision$ $Date$
 */
public class DatumDataSourceUploadJob<T extends Datum> extends AbstractJob
implements StatefulJob {

	private DatumDataSource<T> datumDataSource = null;
	private UploadService uploadService;
	
	@Override
	protected void executeInternal(JobExecutionContext jobContext)
			throws Exception {
		if ( log.isInfoEnabled() ) {
			log.info("Collecting [" 
					+datumDataSource.getDatumType().getSimpleName() 
					+"] now from [" +datumDataSource +']');
		}
		
		T datum = datumDataSource.readCurrentDatum();
		if ( datum == null ) {
			if ( log.isInfoEnabled() ) {
				log.info("No data returned from DatumDataSource.");
			}
			return;
		}
		
		if ( log.isInfoEnabled() ) {
			log.info("Uploading [" +datum
					+"] to [" +uploadService.getKey() +']');
		}
		
		Long tid = uploadService.uploadDatum(datum);
		if ( log.isTraceEnabled() ) {
			log.trace("Just uploaded [" 
				+datumDataSource.getDatumType().getSimpleName() 
				+"] and received tid [" + tid +"]");
		}
		
	}

	/**
	 * @return the datumDataSource
	 */
	public DatumDataSource<T> getDatumDataSource() {
		return datumDataSource;
	}

	/**
	 * @param datumDataSource the datumDataSource to set
	 */
	public void setDatumDataSource(DatumDataSource<T> datumDataSource) {
		this.datumDataSource = datumDataSource;
	}

	/**
	 * @return the uploadService
	 */
	public UploadService getUploadService() {
		return uploadService;
	}

	/**
	 * @param uploadService the uploadService to set
	 */
	public void setUploadService(UploadService uploadService) {
		this.uploadService = uploadService;
	}

}
