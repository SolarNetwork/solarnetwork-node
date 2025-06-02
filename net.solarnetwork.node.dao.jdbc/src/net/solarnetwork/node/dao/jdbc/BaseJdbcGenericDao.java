/* ==================================================================
 * BaseJdbcGenericDao.java - 7/02/2020 9:58:53 am
 *
 * Copyright 2020 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.dao.jdbc;

import static java.lang.String.format;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.UUID;
import org.osgi.service.event.Event;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.RowMapper;
import net.solarnetwork.dao.Entity;
import net.solarnetwork.dao.GenericDao;
import net.solarnetwork.domain.SortDescriptor;

/**
 * Base implementation of {@link GenericDao} for SolarNode using JDBC.
 *
 * @param <T>
 *        the entity type
 * @param <K>
 *        the primary key type
 * @author matt
 * @version 3.0
 */
public abstract class BaseJdbcGenericDao<T extends Entity<K>, K extends Comparable<K>>
		extends AbstractJdbcDao<T> implements GenericDao<T, K> {

	/** Prefix format for SQL resources, e.g. {@code derby-N}. */
	public static final String SQL_RESOURCE_PREFIX = "derby-%s";

	/**
	 * The default SQL format for the {@code sqlGetTablesVersion} property. The
	 * {@link #getTableName()} value is used in the pattern, e.g.
	 * {@code T-init.sql}.
	 */
	public static final String SQL_GET_TABLES_VERSION_FORMAT = "SELECT svalue FROM solarnode.sn_settings WHERE skey = 'solarnode.%s.version'";

	/**
	 * The default classpath resource format for the {@code initSqlResource}.
	 * The {@link #getSqlResourcePrefix()} value is used in the pattern, e.g.
	 * {@code R-init.sql}.
	 */
	public static final String INIT_SQL_FORMAT = "%s-init.sql";

	/** The SQL resource suffix for inserting an entity. */
	public static final String SQL_INSERT = "insert";

	/** The SQL resource suffix for updating an entity. */
	public static final String SQL_UPDATE = "update";

	/** The SQL resource suffix for getting an entity by primary key. */
	public static final String SQL_GET_BY_PK = "get-pk";

	/** The SQL resource suffix for finding all entities. */
	public static final String SQL_FIND_ALL = "find-all";

	/**
	 * The SQL resource suffix for deleting by primary key.
	 */
	public static final String SQL_DELETE_BY_PK = "delete-pk";

	/** The SQL {@literal ORDER BY} term. */
	public static final String ORDER_BY = "ORDER BY";

	/** A UTC based Calendar for managing time based column values. */
	protected static final Calendar UTC_CALENDAR = Calendar.getInstance(TimeZone.getTimeZone("UTC"));

	/**
	 * An event property name for a {@code DataSource} URI.
	 *
	 * @since 2.2
	 */
	public static final String DATASOURCE_URL_PROP = "dataSourceUrl";

	private final Class<? extends T> objectType;
	private final Class<? extends K> keyType;
	private final RowMapper<T> rowMapper;

	/**
	 * Constructor.
	 *
	 * @param objectType
	 *        the entity type
	 * @param keyType
	 *        the key type
	 * @param rowMapper
	 *        a mapper to use when mapping entity query result rows to entity
	 *        objects
	 * @throws IllegalArgumentException
	 *         if any parameter is {@literal null}
	 */
	public BaseJdbcGenericDao(Class<? extends T> objectType, Class<? extends K> keyType,
			RowMapper<T> rowMapper) {
		super();
		if ( objectType == null ) {
			throw new IllegalArgumentException("The objectType parameter must not be null.");
		}
		if ( keyType == null ) {
			throw new IllegalArgumentException("The keyType parameter must not be null.");
		}
		this.objectType = objectType;
		this.keyType = keyType;
		this.rowMapper = rowMapper;
	}

	/**
	 * Init with an an entity name and table version, deriving various names
	 * based on conventions.
	 *
	 * @param objectType
	 *        the entity type
	 * @param keyType
	 *        the key type
	 * @param rowMapper
	 *        a mapper to use when mapping entity query result rows to entity
	 *        objects
	 * @param tableNameTemplate
	 *        a template with a {@code %s} parameter for the SQL table name
	 * @param entityName
	 *        The entity name to use. This name forms the basis of the default
	 *        SQL resource prefix, table name, tables version query, and SQL
	 *        init resource.
	 * @param version
	 *        the tables version, to manage DDL migrations
	 */
	public BaseJdbcGenericDao(Class<? extends T> objectType, Class<? extends K> keyType,
			RowMapper<T> rowMapper, String tableNameTemplate, String entityName, int version) {
		this(objectType, keyType, rowMapper);
		setSqlResourcePrefix(format(SQL_RESOURCE_PREFIX, entityName));
		setTableName(format(tableNameTemplate, entityName));
		setTablesVersion(version);
		setSqlGetTablesVersion(format(SQL_GET_TABLES_VERSION_FORMAT, getTableName()));
		setInitSqlResource(
				new ClassPathResource(format(INIT_SQL_FORMAT, getSqlResourcePrefix()), getClass()));
	}

	@Override
	public Class<? extends T> getObjectType() {
		return objectType;
	}

	/**
	 * Get the key type.
	 *
	 * @return the type, never {@literal null}
	 */
	public Class<? extends K> getKeyType() {
		return keyType;
	}

	/**
	 * Get the default row mapper.
	 *
	 * @return The row mapper.
	 */
	protected RowMapper<T> getRowMapper() {
		return rowMapper;
	}

	@SuppressWarnings("unchecked")
	@Override
	public K save(T entity) {
		K result;
		if ( entity.getId() == null ) {
			if ( !isUseAutogeneratedKeys() ) {
				throw new IllegalArgumentException(
						"The entity ID must be provided when auto-generated keys are not used.");
			}
			if ( !Long.class.isAssignableFrom(keyType) ) {
				throw new RuntimeException(
						"Only Long entity keys may be used with auto-generated keys.");
			}
			result = (K) storeDomainObject(entity, getSqlResource(SQL_INSERT));
		} else {
			int count = updateDomainObject(entity, getSqlResource(SQL_UPDATE));
			if ( count == 0 ) {
				insertDomainObject(entity, getSqlResource(SQL_INSERT));
			}
			result = entity.getId();
		}

		postEntityEvent(result, entity, EntityEventType.STORED);
		return result;
	}

	/**
	 * Post an entity event.
	 *
	 * <p>
	 * The {@link #getEventAdmin()} service must be available for the event to
	 * be posted.
	 * </p>
	 *
	 * @param id
	 *        the entity ID
	 * @param entity
	 *        the optional entity
	 * @param eventType
	 *        the type of event
	 */
	protected void postEntityEvent(K id, T entity, EntityEventType eventType) {
		if ( id == null ) {
			return;
		}
		Map<String, Object> props = GenericDao.createEntityEventProperties(id, entity);
		String url = getJdbcTemplate().execute((ConnectionCallback<String>) conn -> {
			return conn.getMetaData().getURL();
		});
		if ( url != null ) {
			props.put(DATASOURCE_URL_PROP, url);
		}
		String topic = entityEventTopic(eventType);
		if ( topic != null ) {
			Event event = new Event(topic, props);
			postEvent(event);
		}
	}

	@Override
	public T get(K id) {
		if ( id == null ) {
			throw new IllegalArgumentException("The id parameter must not be null.");
		}
		return findFirst(getSqlResource(SQL_GET_BY_PK), primaryKeyArguments(id));
	}

	/**
	 * Get the first returned query result.
	 *
	 * <p>
	 * The {@link #getRowMapper()} will be used to map the results.
	 * </p>
	 *
	 * @param sql
	 *        the SQL to execute
	 * @param parameters
	 *        the optional parameters
	 * @return the first result, or {@literal null}
	 */
	protected T findFirst(String sql, Object... parameters) {
		List<T> results = getJdbcTemplate().query(sql, rowMapper, parameters);
		return (results != null && !results.isEmpty() ? results.get(0) : null);
	}

	/**
	 * Get an argument list for a primary key.
	 *
	 * <p>
	 * This method handles {@link java.util.UUID} values as a pair of
	 * {@code Long} arguments for the most and least significant bits. All other
	 * keys are returned as a single argument list as-is.
	 * </p>
	 *
	 * @param id
	 *        the primary key
	 * @return the arguments
	 */
	protected Object[] primaryKeyArguments(K id) {
		if ( id instanceof UUID ) {
			return new Object[] { ((UUID) id).getMostSignificantBits(),
					((UUID) id).getLeastSignificantBits() };
		}
		return new Object[] { id };
	}

	@Override
	public Collection<T> getAll(List<SortDescriptor> sorts) {
		return getJdbcTemplate().query(querySql(SQL_FIND_ALL, sorts), rowMapper);
	}

	/**
	 * Get the SQL to use for a query with optional sort descriptors applied.
	 *
	 * <p>
	 * This method will call {@link #sqlOrderClauses(String, List)} and if that
	 * returns any values, {@link #applySqlOrderClauses(String, List)}.
	 * </p>
	 *
	 * @param classPathResource
	 *        the base SQL resource to load
	 * @param sorts
	 *        the sorts to apply
	 * @return the SQL
	 */
	protected String querySql(String classPathResource, List<SortDescriptor> sorts) {
		String sql = getSqlResource(classPathResource);
		List<String> orders = sqlOrderClauses(classPathResource, sorts);
		if ( orders != null ) {
			sql = applySqlOrderClauses(sql, orders);
		}
		return sql;
	}

	/**
	 * Get a list of SQL {@literal ORDER BY} clause values to apply for a given
	 * query and sort descriptors.
	 *
	 * <p>
	 * The returned array should contain just the clause values, like
	 * {@literal my_id DESC}.
	 * </p>
	 *
	 * <p>
	 * This method handles the sort keys defined in {@link StandardSortKey} by
	 * generating order clauses that are named after the enumeration values
	 * themselves. Extending classes can override this method to support more
	 * keys, or override this behaviour.
	 * </p>
	 *
	 * @param classPathResource
	 *        the SQL resource the order clauses is to be applied to
	 * @param sorts
	 *        the sort descriptors
	 * @return the order clauses
	 */
	protected List<String> sqlOrderClauses(String classPathResource, List<SortDescriptor> sorts) {
		List<String> clauses = null;
		if ( sorts != null && !sorts.isEmpty() ) {
			for ( SortDescriptor d : sorts ) {
				String clause = null;
				for ( StandardSortKey k : StandardSortKey.values() ) {
					String lc = k.toString().toLowerCase();
					if ( lc.equalsIgnoreCase(d.getSortKey()) ) {
						clause = sqlOrderClause(lc, d.isDescending());
						break;
					}
				}
				if ( clause != null ) {
					if ( clauses == null ) {
						clauses = new ArrayList<>(sorts.size());
					}
					clauses.add(clause);
				}
			}
		}
		return clauses;
	}

	/**
	 * Apply a list of SQL order clauses to a SQL statement.
	 *
	 * <p>
	 * This method looks for the last {@literal ORDER BY} in {@code sql}, and
	 * replaces it with a newly generated order clause derived from the
	 * {@code orderClauses} list. If no {@literal ORDER BY} is found, it will be
	 * appended along with the generated order clause.
	 * </p>
	 *
	 * @param sql
	 *        the SQL
	 * @param orderClauses
	 *        the order clauses, as returned from
	 *        {@link #sqlOrderClause(String, boolean)}
	 * @return the SQL
	 */
	public static String applySqlOrderClauses(String sql, List<String> orderClauses) {
		final int len = orderClauses != null ? orderClauses.size() : 0;
		if ( len < 1 ) {
			return sql;
		}
		StringBuilder buf = new StringBuilder(sql);
		int orderIdx = sql.toUpperCase().lastIndexOf(ORDER_BY);
		if ( orderIdx < 0 ) {
			buf.append(' ').append(ORDER_BY);
		} else {
			buf.delete(orderIdx + ORDER_BY.length(), buf.length());
		}
		for ( int i = 0; i < len; i++ ) {
			if ( i > 0 ) {
				buf.append(',');
			}
			buf.append(' ');
			buf.append(orderClauses.get(i));
		}
		return buf.toString();
	}

	/**
	 * Get a single SQL order clause for a given column and direction.
	 *
	 * @param columnName
	 *        the column name
	 * @param descending
	 *        {@literal true} for descending order, {@literal false} for
	 *        ascending
	 * @return the SQL clause
	 */
	public static String sqlOrderClause(String columnName, boolean descending) {
		return columnName + " " + (descending ? "DESC" : "ASC");
	}

	@Override
	public void delete(T entity) {
		if ( entity == null || entity.getId() == null ) {
			throw new IllegalArgumentException("The entity id parameter must not be null.");
		}
		getJdbcTemplate().update(getSqlResource(SQL_DELETE_BY_PK), primaryKeyArguments(entity.getId()));
		postEntityEvent(entity.getId(), entity, EntityEventType.DELETED);
	}

	/**
	 * Set an {@link Instant} as a timestamp statement parameter.
	 *
	 * @param stmt
	 *        the statement
	 * @param parameterIndex
	 *        the statement parameter index to set
	 * @param time
	 *        the time to set
	 * @throws SQLException
	 *         if any SQL error occurs
	 */
	public static void setInstantParameter(PreparedStatement stmt, int parameterIndex, Instant time)
			throws SQLException {
		if ( time == null ) {
			stmt.setNull(parameterIndex, Types.TIMESTAMP);
		} else {
			stmt.setTimestamp(parameterIndex, new Timestamp(time.toEpochMilli()),
					(Calendar) UTC_CALENDAR.clone());
		}
	}

	/**
	 * Get an {@link Instant} from a timestamp result set column.
	 *
	 * @param rs
	 *        the result set
	 * @param columnIndex
	 *        the column index
	 * @return the new instant, or {@literal null} if the column was null
	 * @throws SQLException
	 *         if any SQL error occurs
	 */
	public static Instant getInstantColumn(ResultSet rs, int columnIndex) throws SQLException {
		Timestamp ts = rs.getTimestamp(columnIndex, (Calendar) UTC_CALENDAR.clone());
		return ts != null ? Instant.ofEpochMilli(ts.getTime()) : null;
	}

	/**
	 * Set a {@link UUID} as a pair of long statement parameters.
	 *
	 * @param stmt
	 *        the statement
	 * @param parameterIndex
	 *        the statement parameter index to set the UUID upper bits; the
	 *        lower bits will be set on parameter {@code parameterIndex + 1}
	 * @param uuid
	 *        the UUID to set
	 * @throws SQLException
	 *         if any SQL error occurs
	 */
	public static void setUuidParameters(PreparedStatement stmt, int parameterIndex, UUID uuid)
			throws SQLException {
		stmt.setLong(parameterIndex, uuid.getMostSignificantBits());
		stmt.setLong(parameterIndex + 1, uuid.getLeastSignificantBits());
	}

	/**
	 * Get a {@link UUID} from a pair of long result set columns.
	 *
	 * @param rs
	 *        the result set
	 * @param columnIndex
	 *        the column index of the UUID upper bits; the lower bits will be
	 *        read from column {@code columnIndex + 1}
	 * @return the new UUID, or {@literal null} if either column was null
	 * @throws SQLException
	 *         if any SQL error occurs
	 */
	public static UUID getUuidColumns(ResultSet rs, int columnIndex) throws SQLException {
		long hi = rs.getLong(columnIndex);
		if ( rs.wasNull() ) {
			return null;
		}
		long lo = rs.getLong(columnIndex + 1);
		if ( rs.wasNull() ) {
			return null;
		}
		return new UUID(hi, lo);
	}

}
