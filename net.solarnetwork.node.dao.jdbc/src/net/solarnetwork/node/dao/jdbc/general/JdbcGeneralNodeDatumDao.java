/* ==================================================================
 * JdbcGeneralNodeDatumDao.java - Aug 26, 2014 7:03:34 AM
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

package net.solarnetwork.node.dao.jdbc.general;

import static java.lang.String.format;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import org.osgi.service.event.Event;
import org.springframework.core.io.ClassPathResource;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.core.PreparedStatementSetter;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.solarnetwork.domain.datum.Datum;
import net.solarnetwork.domain.datum.DatumId;
import net.solarnetwork.domain.datum.DatumSamples;
import net.solarnetwork.domain.datum.DatumSamplesContainer;
import net.solarnetwork.domain.datum.DatumSamplesOperations;
import net.solarnetwork.domain.datum.DatumSamplesType;
import net.solarnetwork.domain.datum.ObjectDatumKind;
import net.solarnetwork.node.dao.DatumDao;
import net.solarnetwork.node.dao.jdbc.AbstractJdbcDao;
import net.solarnetwork.node.domain.Mock;
import net.solarnetwork.node.domain.datum.NodeDatum;
import net.solarnetwork.node.domain.datum.SimpleDatum;
import net.solarnetwork.node.service.DatumEvents;
import net.solarnetwork.service.PingTest;
import net.solarnetwork.service.PingTestResult;
import net.solarnetwork.settings.SettingSpecifier;
import net.solarnetwork.settings.SettingSpecifierProvider;
import net.solarnetwork.settings.support.BasicTitleSettingSpecifier;
import net.solarnetwork.util.StatCounter;

/**
 * JDBC-based implementation of {@link net.solarnetwork.node.dao.DatumDao} for
 * {@link NodeDatum} domain objects.
 *
 * @author matt
 * @version 2.3
 */
public class JdbcGeneralNodeDatumDao extends AbstractJdbcDao<NodeDatum>
		implements DatumDao, SettingSpecifierProvider, PingTest {

	/** The default tables version. */
	public static final int DEFAULT_TABLES_VERSION = 5;

	/** The maximum allowed length of a datum samples when encoded as JSON. */
	public static final int MAX_SAMPLES_JSON_LENGTH = 8192;

	/** The table name for datum. */
	public static final String TABLE_GENERAL_NODE_DATUM = "sn_general_node_datum";

	/** The default classpath Resource for the {@code initSqlResource}. */
	public static final String DEFAULT_INIT_SQL = "generalnodedatum-init.sql";

	/** The default value for the {@code sqlGetTablesVersion} property. */
	public static final String DEFAULT_SQL_GET_TABLES_VERSION = "SELECT svalue FROM solarnode.sn_settings WHERE skey = "
			+ "'solarnode.sn_general_node_datum.version'";

	/** The default value for the {@code maxFetchForUpload} property. */
	public static final int DEFAULT_MAX_FETCH_FOR_UPLOAD = 60;

	/**
	 * The {@code maxCountPingFail} property default value.
	 *
	 * @since 2.1
	 */
	public static final int DEFAULT_MAX_COUNT_PING_FAIL = 10000;

	/** The SQL resource to insert. */
	public static final String SQL_RESOURCE_INSERT = "insert";

	/** The SQL resource to delete old uploaded rows. */
	public static final String SQL_RESOURCE_DELETE_OLD = "delete-old";

	/** The SQL resource to find rows needing upload. */
	public static final String SQL_RESOURCE_FIND_FOR_UPLOAD = "find-upload";

	/** The SQL resource to fetch by primary key. */
	public static final String SQL_RESOURCE_FIND_FOR_PRIMARY_KEY = "find-pk";

	/** The SQL resource to update the upload date. */
	public static final String SQL_RESOURCE_UPDATE_UPLOADED = "update-upload";

	/** The SQL resource to update data. */
	public static final String SQL_RESOURCE_UPDATE_DATA = "update-data";

	/** The SQL resource to count. */
	public static final String SQL_RESOURCE_COUNT = "count";

	/**
	 * A source ID for log messages posted as datum.
	 *
	 * @since 2.2
	 */
	public static final String LOG_SOURCE_ID = "log";

	/**
	 * A source ID prefix for log messages posted as datum.
	 *
	 * @since 2.2
	 */
	public static final String LOG_SOURCE_ID_PREFIX = LOG_SOURCE_ID + "/";

	private final StatCounter stats;
	private ObjectMapper objectMapper;
	private int maxFetchForUpload = DEFAULT_MAX_FETCH_FOR_UPLOAD;
	private boolean ignoreMockData = true;
	private int maxCountPingFail = DEFAULT_MAX_COUNT_PING_FAIL;

	/**
	 * Default constructor.
	 */
	public JdbcGeneralNodeDatumDao() {
		super();
		setSqlResourcePrefix("generalnodedatum");
		setTableName(TABLE_GENERAL_NODE_DATUM);
		setTablesVersion(DEFAULT_TABLES_VERSION);
		setSqlGetTablesVersion(DEFAULT_SQL_GET_TABLES_VERSION);
		setInitSqlResource(new ClassPathResource(DEFAULT_INIT_SQL, getClass()));
		this.stats = new StatCounter("JdbcDatumDao", "", log, 100, DatumDaoStat.values());
	}

	@Override
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED,
			noRollbackFor = DuplicateKeyException.class)
	public void storeDatum(NodeDatum datum) {
		try {
			storeDomainObject(datum);
		} catch ( DuplicateKeyException e ) {
			List<NodeDatum> existing = findDatum(SQL_RESOURCE_FIND_FOR_PRIMARY_KEY,
					preparedStatementSetterForPrimaryKey(datum.getTimestamp(), datum.getSourceId()),
					rowMapper());
			if ( existing.size() > 0 ) {
				// only update if the samples have changed
				DatumSamplesOperations existingSamples = existing.get(0).asSampleOperations();
				DatumSamplesOperations newSamples = datum.asSampleOperations();
				if ( newSamples.differsFrom(existingSamples) ) {
					updateDomainObject(datum, getSqlResource(SQL_RESOURCE_UPDATE_DATA));
				}
			}
		}
	}

	@Override
	protected void setUpdateStatementValues(NodeDatum datum, PreparedStatement ps) throws SQLException {
		int col = 1;
		ps.setString(col++, jsonForSamples(datum));
		ps.setTimestamp(col++, Timestamp.from(datum.getTimestamp()));
		ps.setString(col++, datum.getSourceId());
	}

	@Override
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	public void setDatumUploaded(NodeDatum datum, Instant date, String destination, String trackingId) {
		updateDatumUpload(datum, date == null ? Instant.now() : date);
	}

	@Override
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	public int deleteUploadedDataOlderThan(int hours) {
		return deleteUploadedDataOlderThanHours(hours);
	}

	private RowMapper<NodeDatum> rowMapper() {
		return new RowMapper<NodeDatum>() {

			@Override
			public NodeDatum mapRow(ResultSet rs, int rowNum) throws SQLException {
				if ( log.isTraceEnabled() ) {
					log.trace("Handling result row " + rowNum);
				}
				int col = 0;
				Instant ts = rs.getTimestamp(++col).toInstant();
				String sourceId = rs.getString(++col);
				Long locationId = (Long) rs.getObject(++col);

				DatumId id = (locationId != null ? DatumId.locationId(locationId, sourceId, ts)
						: DatumId.nodeId(null, sourceId, ts));
				DatumSamples s = null;
				String jdata = rs.getString(++col);
				if ( jdata != null ) {
					try {
						s = objectMapper.readValue(jdata, DatumSamples.class);
					} catch ( IOException e ) {
						log.error("Error deserializing JSON into GeneralNodeDatumSamples: {}",
								e.getMessage());
					}
				}
				return new SimpleDatum(id, s);
			}
		};
	}

	@Override
	@Transactional(readOnly = true, propagation = Propagation.REQUIRED)
	public List<NodeDatum> getDatumNotUploaded(String destination) {
		return findDatumNotUploaded(rowMapper());
	}

	private String jsonForSamples(NodeDatum datum) {
		DatumSamples s = ((DatumSamplesContainer) datum).getSamples();
		String json;
		try {
			json = objectMapper.writeValueAsString(s);
		} catch ( IOException e ) {
			log.error("Error serializing DatumSamples into JSON: {}", e.getMessage());
			json = "{}";
		}
		int len = (json != null ? json.length() : 0);
		if ( len > MAX_SAMPLES_JSON_LENGTH ) {
			// try to remove all status properties and replace with error message
			DatumSamples s2 = new DatumSamples(s);
			if ( s2.getStatus() != null && !s2.getStatus().isEmpty() ) {
				if ( s2.hasSampleValue(DatumSamplesType.Status, "exSt") ) {
					s2.putStatusSampleValue("exSt", null);
				} else {
					s2.getStatus().clear();
					s2.putStatusSampleValue("error",
							format("Datum must be less than %d characters, but is %d",
									MAX_SAMPLES_JSON_LENGTH, len));
				}
			}
			try {
				json = objectMapper.writeValueAsString(s2);
			} catch ( IOException e ) {
				log.error("Error serializing DatumSamples into JSON: {}", e.getMessage());
				json = "{}";
			}
		}
		len = (json != null ? json.length() : 0);
		if ( len > MAX_SAMPLES_JSON_LENGTH ) {
			json = "{}"; // fallback
		}
		return json;
	}

	@Override
	protected void setStoreStatementValues(NodeDatum datum, PreparedStatement ps) throws SQLException {
		int col = 0;
		ps.setTimestamp(++col,
				Timestamp
						.from(datum.getTimestamp() == null ? Instant.now().truncatedTo(ChronoUnit.MILLIS)
								: datum.getTimestamp().truncatedTo(ChronoUnit.MILLIS)));
		ps.setString(++col, datum.getSourceId() == null ? "" : datum.getSourceId());
		if ( datum.getKind() == ObjectDatumKind.Location ) {
			ps.setObject(++col, datum.getObjectId());
		} else {
			ps.setNull(++col, Types.CHAR);
		}

		String json = jsonForSamples(datum);
		ps.setString(++col, json);
	}

	/**
	 * Get the object mapper.
	 *
	 * @return the mapper
	 */
	public ObjectMapper getObjectMapper() {
		return objectMapper;
	}

	/**
	 * Set the object mapper.
	 *
	 * @param objectMapper
	 *        the mapper to set
	 */
	public void setObjectMapper(ObjectMapper objectMapper) {
		this.objectMapper = objectMapper;
	}

	/**
	 * Execute a SQL update to delete data that has already been "uploaded" and
	 * is older than a specified number of hours.
	 *
	 * <p>
	 * This executes SQL from the {@code sqlDeleteOld} property, setting a
	 * single timestamp parameter as the current time minus {@code hours} hours.
	 * The general idea is for the SQL to join to some "upload" table to find
	 * the rows in the "datum" table that have been uploaded and are older than
	 * the specified number of hours. For example:
	 * </p>
	 *
	 * <pre>
	 * DELETE FROM solarnode.sn_some_datum p WHERE p.id IN
	 * (SELECT pd.id FROM solarnode.sn_some_datum pd
	 * INNER JOIN solarnode.sn_some_datum_upload u
	 * ON u.power_datum_id = pd.id WHERE pd.created &lt; ?)
	 * </pre>
	 *
	 * @param hours
	 *        the number of hours hold to delete
	 * @return the number of rows deleted
	 */
	protected int deleteUploadedDataOlderThanHours(final int hours) {
		int result = getJdbcTemplate().update(new PreparedStatementCreator() {

			@Override
			public PreparedStatement createPreparedStatement(Connection con) throws SQLException {
				String sql = getSqlResource(SQL_RESOURCE_DELETE_OLD);
				log.debug("Preparing SQL to delete old datum [{}] with hours [{}]", sql, hours);
				PreparedStatement ps = con.prepareStatement(sql);
				Calendar c = Calendar.getInstance();
				c.add(Calendar.HOUR, -hours);
				ps.setTimestamp(1, new Timestamp(c.getTimeInMillis()), c);
				return ps;
			}
		});
		stats.addAndGet(DatumDaoStat.DatumDeleted, result);
		return result;
	}

	/**
	 * Find datum entities that have not been uploaded to a specific
	 * destination.
	 *
	 * <p>
	 * This executes SQL from the {@code findForUploadSql} property. It uses the
	 * {@code maxFetchForUpload} property to limit the number of rows returned,
	 * so the call may not return all rows available from the database (this is
	 * to conserve memory and process the data in small batches).
	 * </p>
	 *
	 * @param rowMapper
	 *        a {@link RowMapper} implementation to instantiate entities from
	 *        found rows
	 * @return the matching rows, never {@literal null}
	 */
	protected List<NodeDatum> findDatumNotUploaded(final RowMapper<NodeDatum> rowMapper) {
		List<NodeDatum> result = getJdbcTemplate().query(new PreparedStatementCreator() {

			@Override
			public PreparedStatement createPreparedStatement(Connection con) throws SQLException {
				String sql = getSqlResource(SQL_RESOURCE_FIND_FOR_UPLOAD);
				if ( log.isTraceEnabled() ) {
					log.trace("Preparing SQL to find datum not uploaded [" + sql
							+ "] with maxFetchForUpload [" + maxFetchForUpload + ']');
				}
				PreparedStatement ps = con.prepareStatement(sql);
				ps.setFetchDirection(ResultSet.FETCH_FORWARD);
				ps.setFetchSize(maxFetchForUpload);
				ps.setMaxRows(maxFetchForUpload);
				return ps;
			}
		}, rowMapper);
		if ( log.isDebugEnabled() ) {
			log.debug("Found " + result.size() + " datum entities not uploaded");
		}
		return result;
	}

	/**
	 * Find datum entities.
	 *
	 * @param sqlResource
	 *        The name of the SQL resource to use. See
	 *        {@link #getSqlResource(String)}
	 * @param setter
	 *        A prepared statement setter
	 * @param rowMapper
	 *        a {@link RowMapper} implementation to instantiate entities from
	 *        found rows
	 * @return the matching rows, never {@literal null}
	 * @since 1.2
	 */
	protected List<NodeDatum> findDatum(final String sqlResource, final PreparedStatementSetter setter,
			final RowMapper<NodeDatum> rowMapper) {
		List<NodeDatum> result = getJdbcTemplate().query(new PreparedStatementCreator() {

			@Override
			public PreparedStatement createPreparedStatement(Connection con) throws SQLException {
				String sql = getSqlResource(sqlResource);
				if ( log.isTraceEnabled() ) {
					log.trace("Preparing SQL [{}] to find datum", sql);
				}
				PreparedStatement ps = con.prepareStatement(sql);
				ps.setFetchDirection(ResultSet.FETCH_FORWARD);
				ps.setFetchSize(1);
				ps.setMaxRows(1);
				setter.setValues(ps);
				return ps;
			}
		}, rowMapper);
		return result;
	}

	/**
	 * Create a {@link PreparedStatementSetter} that sets the primary key values
	 * on a statement.
	 *
	 * @param created
	 *        The created date of the datum.
	 * @param sourceId
	 *        The source ID of the datum.
	 * @return The setter instance.
	 * @see #findDatum(String, PreparedStatementSetter, RowMapper)
	 * @since 1.2
	 */
	protected PreparedStatementSetter preparedStatementSetterForPrimaryKey(final Instant created,
			final String sourceId) {
		return new PreparedStatementSetter() {

			@Override
			public void setValues(PreparedStatement ps) throws SQLException {
				ps.setTimestamp(1, Timestamp.from(created));
				ps.setString(2, sourceId);
			}
		};
	}

	/**
	 * Store a new domain object using the {@link #SQL_RESOURCE_INSERT} SQL.
	 *
	 * <p>
	 * If {@link #isIgnoreMockData()} returns {@literal true} and {@code datum}
	 * is an instance of {@link Mock} then this method will not persist the
	 * object and will simply return {@code -1}.
	 * </p>
	 *
	 * @param datum
	 *        the datum to persist
	 */
	protected void storeDomainObject(final NodeDatum datum) {
		if ( ignoreMockData && datum instanceof Mock ) {
			if ( log.isDebugEnabled() ) {
				log.debug("Not persisting Mock datum: " + datum);
			}
			return;
		}
		insertDomainObject(datum, getSqlResource(SQL_RESOURCE_INSERT));
		if ( !(LOG_SOURCE_ID.equalsIgnoreCase(datum.getSourceId())
				|| datum.getSourceId().startsWith(LOG_SOURCE_ID_PREFIX)) ) {
			log.info("Persisted datum locally: {}", datum);
		}
		stats.incrementAndGet(DatumDaoStat.DatumStored);
		postDatumStoredEvent(datum);
	}

	/**
	 * {@inheritDoc}
	 *
	 * @since 1.3
	 */
	@Override
	protected int updateDomainObject(NodeDatum datum, String sqlUpdate) {
		int result = super.updateDomainObject(datum, sqlUpdate);
		if ( result > 0 ) {
			postDatumStoredEvent(datum);
		}
		return result;
	}

	/**
	 * Mark a Datum as uploaded.
	 *
	 * <p>
	 * This method will call
	 * {@link #updateDatumUpload(Instant, Object, Instant)} passing in
	 * {@link NodeDatum#getTimestamp()}, {@link NodeDatum#getSourceId()}, and
	 * {@code timestamp}.
	 * </p>
	 *
	 * @param datum
	 *        the datum that was uploaded
	 * @param timestamp
	 *        the date the upload happened
	 */
	protected void updateDatumUpload(final NodeDatum datum, final Instant timestamp) {
		updateDatumUpload(datum.getTimestamp(), datum.getSourceId(), timestamp);
	}

	/**
	 * Mark a Datum as uploaded.
	 *
	 * <p>
	 * This method will execute the {@link #SQL_RESOURCE_UPDATE_UPLOADED} SQL
	 * setting the following parameters:
	 * </p>
	 *
	 * <ol>
	 * <li>Timestamp parameter based on {@code timestamp}</li>
	 * <li>Timestamp parameter based on {@code created}</li>
	 * <li>Object parameter based on {@code id}</li>
	 * </ol>
	 *
	 * @param created
	 *        the date the object was created
	 * @param id
	 *        the object's source or location ID
	 * @param timestamp
	 *        the date the upload happened
	 * @return the number of updated rows
	 */
	protected int updateDatumUpload(final Instant created, final Object id, final Instant timestamp) {
		int result = getJdbcTemplate().update(new PreparedStatementCreator() {

			@Override
			public PreparedStatement createPreparedStatement(Connection con) throws SQLException {
				PreparedStatement ps = con
						.prepareStatement(getSqlResource(SQL_RESOURCE_UPDATE_UPLOADED));
				int col = 1;
				ps.setTimestamp(col++, Timestamp.from(timestamp));
				ps.setTimestamp(col++, Timestamp.from(created));
				ps.setObject(col++, id);
				return ps;
			}
		});
		stats.addAndGet(DatumDaoStat.DatumUploaded, result);
		return result;
	}

	/**
	 * Post an {@link Event} for the {@link DatumDao#EVENT_TOPIC_DATUM_STORED}
	 * topic.
	 *
	 * @param datum
	 *        the datum that was stored
	 * @since 1.3
	 */
	protected final void postDatumStoredEvent(NodeDatum datum) {
		Event event = createDatumStoredEvent(datum);
		postEvent(event);
	}

	/**
	 * Create a new {@link DatumDao#EVENT_TOPIC_DATUM_STORED} {@link Event}
	 * object out of a {@link Datum}.
	 *
	 * <p>
	 * This method uses the result of {@link Datum#asSimpleMap()} as the event
	 * properties.
	 * </p>
	 *
	 * @param datum
	 *        the datum to create the event for
	 * @return the new Event instance
	 * @since 1.3
	 */
	protected Event createDatumStoredEvent(final NodeDatum datum) {
		return DatumEvents.datumEvent(EVENT_TOPIC_DATUM_STORED, datum);
	}

	@Override
	public String getPingTestId() {
		return getSettingUid();
	}

	@Override
	public String getPingTestName() {
		return getDisplayName();
	}

	@Override
	public long getPingTestMaximumExecutionMilliseconds() {
		return 5000;
	}

	@Override
	public Result performPingTest() throws Exception {
		final long rowCount = rowCount();
		final int maxCount = getMaxCountPingFail();
		boolean ok = true;
		String msg = getMessageSource().getMessage("db.rowCount", new Object[] { rowCount },
				Locale.getDefault());
		if ( maxCount > 0 && rowCount > maxCount ) {
			ok = false;
		}
		return new PingTestResult(ok, msg, Collections.singletonMap("count", rowCount));
	}

	private long rowCount() {
		final Number rowCountNum = getJdbcTemplate().queryForObject(getSqlResource(SQL_RESOURCE_COUNT),
				Number.class);
		return (rowCountNum == null ? 0 : rowCountNum.longValue());
	}

	@Override
	public String getSettingUid() {
		return "net.solarnetwork.node.dao.jdbc.general.datum";
	}

	@Override
	public String getDisplayName() {
		return "DatumDao (JDBC)";
	}

	@Override
	public List<SettingSpecifier> getSettingSpecifiers() {
		return Collections.singletonList((SettingSpecifier) new BasicTitleSettingSpecifier("status",
				getStatusMessage(), true, true));
	}

	private String getStatusMessage() {
		// @formatter:off
		long rowCount = 0;
		try {
			rowCount = rowCount();
		} catch ( Exception e ) {
			log.warn("Error finding datum row count.", e);
		}
		return getMessageSource().getMessage("status.msg",
				new Object[] {
						rowCount,
						stats.get(DatumDaoStat.DatumStored),
						stats.get(DatumDaoStat.DatumUploaded),
						stats.get(DatumDaoStat.DatumDeleted),
						},
				Locale.getDefault());
		// @formatter:on
	}

	/**
	 * Get the maximum number of datum to fetch for upload at one time.
	 *
	 * @return the maximum number of datum rows to fetch
	 */
	public int getMaxFetchForUpload() {
		return maxFetchForUpload;
	}

	/**
	 * The maximum number of rows to return in the
	 * {@link #findDatumNotUploaded(RowMapper)} method.
	 *
	 * <p>
	 * Defaults to {@link #DEFAULT_MAX_FETCH_FOR_UPLOAD}.
	 * </p>
	 *
	 * @param maxFetchForUpload
	 *        the maximum upload value
	 */
	public void setMaxFetchForUpload(int maxFetchForUpload) {
		this.maxFetchForUpload = maxFetchForUpload;
	}

	/**
	 * Get the flag to ignore mock data.
	 *
	 * @return {@literal true} to not store any mock data
	 */
	public boolean isIgnoreMockData() {
		return ignoreMockData;
	}

	/**
	 * Set a flag to not actually store any domain object that implements the
	 * {@link Mock} interface.
	 *
	 * <p>
	 * This defaults to {@literal true}, but during development it can be useful
	 * to configure this as {@literal false} for testing.
	 * </p>
	 *
	 * @param ignoreMockData
	 *        the ignore mock data value
	 */
	public void setIgnoreMockData(boolean ignoreMockData) {
		this.ignoreMockData = ignoreMockData;
	}

	/**
	 * Get the maximum number of messages to store before failing ping tests.
	 *
	 * @return the maximum count, or {@literal 0} to disable the test; defaults
	 *         to {@link #DEFAULT_MAX_COUNT_PING_FAIL}
	 * @since 2.1
	 */
	public int getMaxCountPingFail() {
		return maxCountPingFail;
	}

	/**
	 * Set the maximum number of messages to store before failing ping tests.
	 *
	 * @param maxCountPingFail
	 *        the maximum count, or {@literal 0} to disable the test
	 * @since 2.1
	 */
	public void setMaxCountPingFail(int maxCountPingFail) {
		this.maxCountPingFail = maxCountPingFail;
	}

}
