/* ===================================================================
 * DatumDataSourceLoggerJob.java
 * 
 * Created Dec 1, 2009 4:35:24 PM
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
 */

package net.solarnetwork.node.job;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import net.solarnetwork.node.DatumDataSource;
import net.solarnetwork.node.MultiDatumDataSource;
import net.solarnetwork.node.dao.DatumDao;
import net.solarnetwork.node.domain.Datum;
import org.quartz.JobExecutionContext;
import org.quartz.StatefulJob;
import org.springframework.dao.DuplicateKeyException;

/**
 * Job to collect data from a {@link DatumDataSource} and persist that via a
 * {@link DatumDao}.
 * 
 * <p>
 * This job simply calls {@link DatumDataSource#readCurrentDatum()} and if that
 * returns a non-null object, passes that to {@link DatumDao#storeDatum(Datum)}.
 * In essence, this job is for reading the current data available on some device
 * and then persisting it to a (probably local) database.
 * </p>
 * 
 * <p>
 * If the configured {@code datumDataSource} implements
 * {@link MultiDatumDataSource} and the Class returned by
 * {@link MultiDatumDataSource#getMultiDatumType()} is assignable to the Class
 * returned by {@link DatumDataSource#getDatumType()} then
 * {@link MultiDatumDataSource#readMultipleDatum()} will be called instead of
 * {@link DatumDataSource#readCurrentDatum()}. Each {@code Datum} returned in
 * the resulting Collection will be persisted to the configured {@link DatumDao}
 * .
 * </p>
 * 
 * <p>
 * The configurable properties of this class are:
 * </p>
 * 
 * <dl class="class-properties">
 * <dt>datumDataSource</dt>
 * <dd>The {@link DatumDataSource} to collect the data from. The
 * {@link DatumDataSource#readCurrentDatum()} method will be called to get the
 * currently available data.</dd>
 * 
 * <dt>datumDao</dt>
 * <dd>The {@link DatumDao} to persist the collected data to. The
 * {@link DatumDao#storeDatum(Datum)} method will be called with the
 * {@link Datum} returned by {@link DatumDataSource#readCurrentDatum()}, if it
 * is non-null.</dd>
 * </dl>
 * 
 * @param <T>
 *        the Datum type for this job
 * @author matt
 * @version 1.1
 */
public class DatumDataSourceLoggerJob<T extends Datum> extends AbstractJob implements StatefulJob {

	private List<DatumDataSource<T>> datumDataSources = null;
	private DatumDao<T> datumDao = null;

	@Override
	protected void executeInternal(JobExecutionContext jobContext) throws Exception {
		for ( DatumDataSource<T> datumDataSource : datumDataSources ) {
			try {
				if ( log.isDebugEnabled() ) {
					log.debug("Collecting [{}] from [{}]", datumDataSource.getDatumType()
							.getSimpleName(), datumDataSource);
				}

				Collection<T> datumList = null;
				if ( datumDataSource instanceof MultiDatumDataSource<?> ) {
					datumList = readMultiDatum(datumDataSource);
				}
				if ( datumList == null ) {
					T datum = datumDataSource.readCurrentDatum();
					if ( datum != null ) {
						datumList = new LinkedList<T>();
						datumList.add(datum);
					}
				}
				if ( datumList == null || datumList.isEmpty() ) {
					if ( log.isInfoEnabled() ) {
						log.info("No data returned from [{}]", datumDataSource);
					}
					continue;
				}

				if ( log.isInfoEnabled() ) {
					log.info("Got Datum to persist: {}", (datumList.size() == 1 ? datumList.iterator()
							.next().toString() : datumList.toString()));
				}
				for ( T datum : datumList ) {
					try {
						datumDao.storeDatum(datum);
						log.debug("Persisted Datum {}", datum);
					} catch ( DuplicateKeyException e ) {
						// we ignore duplicate key exceptions, as we sometimes collect the same 
						// datum multiple times for redundancy
						log.info("Duplicate datum {}; not persisting", datum);
					}
				}
			} catch ( Throwable e ) {
				logThrowable(e);
			}
		}
	}

	@SuppressWarnings("unchecked")
	private Collection<T> readMultiDatum(DatumDataSource<T> datumDataSource) {
		MultiDatumDataSource<T> multi = (MultiDatumDataSource<T>) datumDataSource;
		if ( !datumDataSource.getDatumType().isAssignableFrom(multi.getMultiDatumType()) ) {
			return null;
		}
		return multi.readMultipleDatum();
	}

	public DatumDataSource<T> getDatumDataSource() {
		return datumDataSources == null || datumDataSources.size() < 1 ? null : datumDataSources.get(0);
	}

	public void setDatumDataSource(DatumDataSource<T> datumDataSource) {
		if ( this.datumDataSources == null ) {
			this.datumDataSources = new ArrayList<DatumDataSource<T>>(2);
		}
		this.datumDataSources.clear();
		this.datumDataSources.add(datumDataSource);
	}

	public List<DatumDataSource<T>> getDatumDataSources() {
		return datumDataSources;
	}

	public void setDatumDataSources(List<DatumDataSource<T>> datumDataSources) {
		this.datumDataSources = datumDataSources;
	}

	public DatumDao<T> getDatumDao() {
		return datumDao;
	}

	public void setDatumDao(DatumDao<T> datumDao) {
		this.datumDao = datumDao;
	}

}
