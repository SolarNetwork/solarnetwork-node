/* ==================================================================
 * DatumDataSourceManagedLoggerJob.java - Aug 26, 2014 2:44:36 PM
 * 
 * Copyright 2007-2014 SolarNetwork.net Dev Team
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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.JobExecutionContext;
import org.quartz.PersistJobDataAfterExecution;
import org.springframework.context.MessageSource;
import org.springframework.dao.DuplicateKeyException;
import net.solarnetwork.node.DatumDataSource;
import net.solarnetwork.node.MultiDatumDataSource;
import net.solarnetwork.node.dao.DatumDao;
import net.solarnetwork.node.domain.Datum;
import net.solarnetwork.node.settings.KeyedSettingSpecifier;
import net.solarnetwork.node.settings.SettingSpecifier;
import net.solarnetwork.node.settings.SettingSpecifierProvider;
import net.solarnetwork.util.OptionalService;

/**
 * Extension of {@link DatumDataSourceLoggerJob} designed to be used as a
 * managed service.
 * 
 * <p>
 * This class implements {@link SettingSpecifierProvider} but delegates that API
 * to the configured {@link #getDatumDataSource()}.
 * </p>
 * 
 * @author matt
 * @version 2.2
 */
@PersistJobDataAfterExecution
@DisallowConcurrentExecution
public class DatumDataSourceManagedLoggerJob<T extends Datum> extends AbstractJob
		implements SettingSpecifierProvider {

	/**
	 * The default {@code saveToDao} property value
	 * 
	 * @since 2.2
	 */
	public static final boolean DEFAULT_SAVE_TO_DAO = false;

	private DatumDataSource<T> datumDataSource = null;
	private MultiDatumDataSource<T> multiDatumDataSource = null;
	private OptionalService<DatumDao<T>> datumDao = null;
	private boolean saveToDao = DEFAULT_SAVE_TO_DAO;

	@Override
	protected void executeInternal(JobExecutionContext jobContext) throws Exception {
		try {
			if ( multiDatumDataSource != null ) {
				executeForMultiDatumDataSource(jobContext);
			} else {
				executeForDatumDataSource(jobContext);
			}
		} catch ( Throwable e ) {
			logThrowable(e);
		}
	}

	private void executeForDatumDataSource(JobExecutionContext jobContext) {
		if ( log.isDebugEnabled() ) {
			log.debug("Collecting [{}] from [{}]", datumDataSource.getDatumType().getSimpleName(),
					datumDataSource);
		}
		T datum = datumDataSource.readCurrentDatum();
		if ( datum != null ) {
			persistDatum(Collections.singleton(datum));
		} else {
			log.debug("No data returned from [{}]", datumDataSource);
		}
	}

	private void executeForMultiDatumDataSource(JobExecutionContext jobContext) {
		if ( log.isDebugEnabled() ) {
			log.debug("Collecting [{}] from [{}]",
					multiDatumDataSource.getMultiDatumType().getSimpleName(), multiDatumDataSource);
		}
		Collection<T> datum = multiDatumDataSource.readMultipleDatum();
		if ( datum != null && datum.size() > 0 ) {
			persistDatum(datum);
		} else {
			log.debug("No data returned from [{}]", multiDatumDataSource);
		}
	}

	@Override
	protected void logThrowable(Throwable e) {
		IOException ioEx = null;
		Throwable t = e;
		while ( true ) {
			if ( t instanceof IOException ) {
				ioEx = (IOException) t;
			}
			t = t.getCause();
			if ( t == null ) {
				break;
			}
		}
		if ( ioEx != null ) {
			log.warn("Communication problem collecting data from {}: {}",
					multiDatumDataSource != null ? multiDatumDataSource : datumDataSource,
					ioEx.toString());
		} else {
			super.logThrowable(e);
		}
	}

	private void persistDatum(Collection<T> datumList) {
		if ( !saveToDao || datumList == null || datumList.size() < 1 ) {
			return;
		}
		if ( log.isDebugEnabled() ) {
			log.debug("Got Datum to persist: {}",
					(datumList.size() == 1 ? datumList.iterator().next().toString()
							: datumList.toString()));
		}
		DatumDao<T> dao = datumDao.service();
		if ( dao == null ) {
			log.info("No DatumDao available to persist {}, not saving", datumList);
			return;
		}
		for ( T datum : datumList ) {
			try {
				dao.storeDatum(datum);
				log.debug("Persisted Datum {}", datum);
			} catch ( DuplicateKeyException e ) {
				// we ignore duplicate key exceptions, as we sometimes collect the same 
				// datum multiple times for redundancy
				log.info("Duplicate datum {}; not persisting", datum);
			}
		}
	}

	private SettingSpecifierProvider getSettingSpecifierProvider() {
		if ( multiDatumDataSource instanceof SettingSpecifierProvider ) {
			return (SettingSpecifierProvider) multiDatumDataSource;
		}
		if ( datumDataSource instanceof SettingSpecifierProvider ) {
			return (SettingSpecifierProvider) datumDataSource;
		}
		return null;
	}

	@Override
	public String getSettingUID() {
		SettingSpecifierProvider delegate = getSettingSpecifierProvider();
		if ( delegate != null ) {
			return delegate.getSettingUID();
		}
		return getDatumDataSource().getClass().getName();
	}

	@Override
	public String getDisplayName() {
		SettingSpecifierProvider delegate = getSettingSpecifierProvider();
		if ( delegate != null ) {
			return delegate.getDisplayName();
		}
		return null;
	}

	@Override
	public MessageSource getMessageSource() {
		SettingSpecifierProvider delegate = getSettingSpecifierProvider();
		if ( delegate != null ) {
			return delegate.getMessageSource();
		}
		return null;
	}

	@Override
	public List<SettingSpecifier> getSettingSpecifiers() {
		SettingSpecifierProvider delegate = getSettingSpecifierProvider();
		if ( delegate == null ) {
			return Collections.emptyList();
		}
		List<SettingSpecifier> result = new ArrayList<SettingSpecifier>();
		final String prefix = (multiDatumDataSource != null ? "multiDatumDataSource."
				: "datumDataSource.");
		for ( SettingSpecifier spec : delegate.getSettingSpecifiers() ) {
			if ( spec instanceof KeyedSettingSpecifier<?> ) {
				KeyedSettingSpecifier<?> keyedSpec = (KeyedSettingSpecifier<?>) spec;
				result.add(keyedSpec.mappedTo(prefix));
			} else {
				result.add(spec);
			}
		}
		return result;
	}

	public DatumDataSource<T> getDatumDataSource() {
		return datumDataSource;
	}

	public void setDatumDataSource(DatumDataSource<T> datumDataSource) {
		this.datumDataSource = datumDataSource;
	}

	public MultiDatumDataSource<T> getMultiDatumDataSource() {
		return multiDatumDataSource;
	}

	public void setMultiDatumDataSource(MultiDatumDataSource<T> multiDatumDataSource) {
		this.multiDatumDataSource = multiDatumDataSource;
	}

	public OptionalService<DatumDao<T>> getDatumDao() {
		return datumDao;
	}

	public void setDatumDao(OptionalService<DatumDao<T>> datumDao) {
		this.datumDao = datumDao;
	}

	/**
	 * Get the "save to DAO" flag.
	 * 
	 * <p>
	 * Since {@literal 2.2} this flag defaults to {@literal false}, meaning
	 * datum are <b>not</b> persisted by default. This is because of the
	 * introduction of the {@link net.solarnetwork.node.DatumQueue} service,
	 * which is presumed to handle the processing of datum as they are captured.
	 * </p>
	 * 
	 * @return the flag; defaults to {@link #DEFAULT_SAVE_TO_DAO}
	 * @since 2.2
	 */
	public boolean isSaveToDao() {
		return saveToDao;
	}

	/**
	 * Set the "save to DAO flag.
	 * 
	 * @param {@literal true} to persist collected datum using the configured
	 * DAO, or {@literal false} not to
	 */
	public void setSaveToDao(boolean saveToDao) {
		this.saveToDao = saveToDao;
	}

}
