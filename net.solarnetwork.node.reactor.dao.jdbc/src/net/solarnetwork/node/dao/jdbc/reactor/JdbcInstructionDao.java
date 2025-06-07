/* ==================================================================
 * JdbcInstructionDao.java - Feb 28, 2011 3:11:51 PM
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

package net.solarnetwork.node.dao.jdbc.reactor;

import static net.solarnetwork.node.dao.jdbc.JdbcDaoConstants.SCHEMA_NAME;
import static net.solarnetwork.node.dao.jdbc.JdbcUtils.setUtcTimestampStatementValue;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.core.io.ClassPathResource;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.PreparedStatementCallback;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import net.solarnetwork.codec.JsonUtils;
import net.solarnetwork.domain.InstructionStatus.InstructionState;
import net.solarnetwork.node.dao.jdbc.AbstractJdbcDao;
import net.solarnetwork.node.dao.jdbc.JdbcUtils;
import net.solarnetwork.node.reactor.BasicInstruction;
import net.solarnetwork.node.reactor.BasicInstructionStatus;
import net.solarnetwork.node.reactor.Instruction;
import net.solarnetwork.node.reactor.InstructionDao;
import net.solarnetwork.node.reactor.InstructionStatus;

/**
 * JDBC implementation of {@link JdbcInstructionDao}.
 *
 * @author matt
 * @version 2.2
 */
public class JdbcInstructionDao extends AbstractJdbcDao<Instruction> implements InstructionDao {

	/** The default tables version. */
	public static final int DEFAULT_TABLES_VERSION = 4;

	/** The table name for {@link Instruction} data. */
	public static final String TABLE_INSTRUCTION = "sn_instruction";

	/** The table name for {@link Instruction} parameter data. */
	public static final String TABLE_INSTRUCTION_PARAM = "sn_instruction_param";

	/** The table name for {@link InstructionStatus} data. */
	public static final String TABLE_INSTRUCTION_STATUS = "sn_instruction_status";

	/** The default classpath Resource for the {@code initSqlResource}. */
	public static final String DEFAULT_INIT_SQL = "instruction-init.sql";

	/** The default value for the {@code sqlGetTablesVersion} property. */
	public static final String DEFAULT_SQL_GET_TABLES_VERSION = "SELECT svalue FROM solarnode.sn_settings WHERE skey = '"
			+ SCHEMA_NAME + '.' + TABLE_INSTRUCTION + ".version'";

	/**
	 * The classpath Resource for the SQL template for inserting an Instruction.
	 */
	public static final String RESOURCE_SQL_INSERT_INSTRUCTION = "insert";

	/**
	 * The classpath Resource for the SQL template for inserting an Instruction
	 * parameter.
	 */
	public static final String RESOURCE_SQL_INSERT_INSTRUCTION_PARAM = "insert-param";

	/**
	 * The classpath Resource for the SQL template for inserting an
	 * InstructionStatus.
	 */
	public static final String RESOURCE_SQL_INSERT_INSTRUCTION_STATUS = "insert-status";

	/**
	 * The classpath Resource for the SQL template for updating an
	 * InstructionStatus.
	 */
	public static final String RESOURCE_SQL_UPDATE_INSTRUCTION_STATUS = "update-status";

	/**
	 * The classpath Resource for the SQL template for performing a
	 * compare-and-set update on an InstructionStatus.
	 *
	 * @since 1.2
	 */
	public static final String RESOURCE_SQL_COMPARE_AND_SET_INSTRUCTION_STATUS = "compare-set-status";

	/**
	 * The classpath Resource for the SQL template for selecting Instruction by
	 * ID.
	 */
	public static final String RESOURCE_SQL_SELECT_INSTRUCTION_FOR_ID = "select-for-id";

	/**
	 * The classpath Resource for the SQL template for selecting Instruction by
	 * state.
	 */
	public static final String RESOURCE_SQL_SELECT_INSTRUCTION_FOR_STATE = "select-for-state";

	/**
	 * The classpath Resource for the SQL template for selecting Instruction by
	 * state and two parameter values.
	 *
	 * @since 2.1
	 */
	public static final String RESOURCE_SQL_SELECT_INSTRUCTION_FOR_STATE_AND_2_PARAMS = "select-for-state-two-params";

	/**
	 * The classpath Resource for the SQL template for selecting Instruction
	 * ready for acknowledgement.
	 */
	public static final String RESOURCE_SQL_SELECT_INSTRUCTION_FOR_ACKNOWEDGEMENT = "select-for-ack";

	/**
	 * The classpath Resource for the SQL template for deleting old Instruction
	 * rows.
	 */
	public static final String RESOURCE_SQL_DELETE_OLD = "delete-old";

	/**
	 * The default value for the {@code maxInstructionStatusParamLength}
	 * property.
	 *
	 * @since 1.1
	 */
	public static final int DEFAULT_MAX_INSTRUCTION_STATUS_PARAM_LENGTH = 1024;

	private int maxInstructionStatusParamLength = DEFAULT_MAX_INSTRUCTION_STATUS_PARAM_LENGTH;

	/**
	 * Default constructor.
	 */
	public JdbcInstructionDao() {
		super();
		setTableName(TABLE_INSTRUCTION);
		setTablesVersion(DEFAULT_TABLES_VERSION);
		setSqlGetTablesVersion(DEFAULT_SQL_GET_TABLES_VERSION);
		setSqlResourcePrefix("instruction");
		setInitSqlResource(new ClassPathResource(DEFAULT_INIT_SQL, getClass()));
		setUseAutogeneratedKeys(false);
	}

	@Override
	public String[] getTableNames() {
		return new String[] { getTableName(), TABLE_INSTRUCTION_PARAM, TABLE_INSTRUCTION_STATUS };
	}

	@Override
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	public void storeInstruction(final Instruction instruction) {
		// first store our Instruction entity
		storeDomainObject(instruction, getSqlResource(RESOURCE_SQL_INSERT_INSTRUCTION));

		// now store all the Instruction's parameters
		getJdbcTemplate().execute(new PreparedStatementCreator() {

			@Override
			public PreparedStatement createPreparedStatement(Connection con) throws SQLException {
				PreparedStatement ps = con
						.prepareStatement(getSqlResource(RESOURCE_SQL_INSERT_INSTRUCTION_PARAM));
				return ps;
			}
		}, new PreparedStatementCallback<Object>() {

			@Override
			public Object doInPreparedStatement(PreparedStatement ps)
					throws SQLException, DataAccessException {
				int pos = 0;
				for ( String paramName : instruction.getParameterNames() ) {
					String[] paramValues = instruction.getAllParameterValues(paramName);
					if ( paramValues == null || paramValues.length < 1 ) {
						continue;
					}
					for ( String paramValue : paramValues ) {
						int col = 1;
						ps.setLong(col++, instruction.getId());
						ps.setString(col++, instruction.getInstructorId());
						ps.setLong(col++, pos);
						ps.setString(col++, paramName);
						ps.setString(col++, paramValue);
						ps.addBatch();
						pos++;
					}
				}
				int[] batchResults = ps.executeBatch();
				if ( log.isTraceEnabled() ) {
					log.trace("Batch inserted {} instruction params: {}", pos,
							Arrays.toString(batchResults));
				}
				return null;
			}
		});

		// finally create a status row
		InstructionState state = instruction.getInstructionState();
		getJdbcTemplate().update(getSqlResource(RESOURCE_SQL_INSERT_INSTRUCTION_STATUS),
				instruction.getId(), instruction.getInstructorId(), Timestamp.from(Instant.now()),
				(state != null ? state : InstructionState.Received).toString());
	}

	@Override
	protected void setStoreStatementValues(Instruction instruction, PreparedStatement ps)
			throws SQLException {
		int col = 0;
		ps.setObject(++col, instruction.getId());
		ps.setString(++col, instruction.getInstructorId());
		setUtcTimestampStatementValue(ps, ++col, instruction.getInstructionDate());
		ps.setString(++col, instruction.getTopic());

		Instant executeAt = instruction.getExecutionDate();
		setUtcTimestampStatementValue(ps, ++col, executeAt != null ? executeAt : Instant.now());
	}

	@Override
	@Transactional(readOnly = true, propagation = Propagation.REQUIRED)
	public Instruction getInstruction(Long instructionId, String instructorId) {
		return getJdbcTemplate().query(getSqlResource(RESOURCE_SQL_SELECT_INSTRUCTION_FOR_ID),
				new ResultSetExtractor<Instruction>() {

					@Override
					public Instruction extractData(ResultSet rs)
							throws SQLException, DataAccessException {
						List<Instruction> results = extractInstructions(rs);
						if ( results.size() > 0 ) {
							return results.get(0);
						}
						return null;
					}
				}, instructionId, instructorId);
	}

	private List<Instruction> extractInstructions(ResultSet rs) throws SQLException {
		List<Instruction> results = new ArrayList<Instruction>(5);
		BasicInstruction bi = null;
		while ( rs.next() ) {
			Long currId = rs.getLong(1);
			String currInstructorId = rs.getString(2);
			if ( bi == null || currInstructorId == null || !bi.getId().equals(currId)
					|| !bi.getInstructorId().equals(currInstructorId) ) {
				InstructionStatus status = new BasicInstructionStatus(currId,
						InstructionState.valueOf(rs.getString(5)), JdbcUtils.getUtcTimestampColumnValue(rs, 6),
						(rs.getString(8) == null ? null : InstructionState.valueOf(rs.getString(8))),
						JsonUtils.getStringMap(rs.getString(7)));
				bi = new BasicInstruction(currId, rs.getString(3), JdbcUtils.getUtcTimestampColumnValue(rs, 4),
						currInstructorId, status);
				results.add(bi);
			}
			String pName = rs.getString(9);
			String pValue = rs.getString(10);
			if ( pName != null ) {
				bi.addParameter(pName, pValue);
			}
		}
		return results;
	}

	@Override
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	public void storeInstructionStatus(Long instructionId, String instructorId,
			InstructionStatus status) {
		String jparams = encodeInstructionParameters(status);
		getJdbcTemplate().update(getSqlResource(RESOURCE_SQL_UPDATE_INSTRUCTION_STATUS),
				status.getInstructionState().toString(), jparams,
				(status.getAcknowledgedInstructionState() == null ? null
						: status.getAcknowledgedInstructionState().toString()),
				instructionId, instructorId);
	}

	/**
	 * Encode status result parameters as JSON.
	 *
	 * @param status
	 *        the status to encode the result parameters for
	 * @return the JSON string, or {@literal null} if there are no parameters to
	 *         encode
	 * @since 1.2
	 */
	private String encodeInstructionParameters(InstructionStatus status) {
		String jparams = null;
		final Map<String, ?> resultParameters = (status != null ? status.getResultParameters() : null);
		if ( resultParameters != null && !resultParameters.isEmpty() ) {
			Map<String, Object> stringParameters = new LinkedHashMap<String, Object>(
					resultParameters.size());
			for ( Map.Entry<String, ?> me : resultParameters.entrySet() ) {
				String key = me.getKey();
				Object val = me.getValue();
				if ( val != null ) {
					String s = val.toString();
					if ( s.length() > maxInstructionStatusParamLength ) {
						// truncate in middle
						s = s.substring(0, maxInstructionStatusParamLength / 2) + '\u2026'
								+ s.substring(s.length() - maxInstructionStatusParamLength / 2);
					}
					stringParameters.put(key, s);
				}
			}
			jparams = JsonUtils.getJSONString(stringParameters, null);
		}
		return jparams;
	}

	@Override
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	public boolean compareAndStoreInstructionStatus(Long instructionId, String instructorId,
			InstructionState expectedState, InstructionStatus status) {
		String jparams = encodeInstructionParameters(status);
		int count = getJdbcTemplate().update(
				getSqlResource(RESOURCE_SQL_COMPARE_AND_SET_INSTRUCTION_STATUS),
				status.getInstructionState().toString(), jparams,
				(status.getAcknowledgedInstructionState() == null ? null
						: status.getAcknowledgedInstructionState().toString()),
				instructionId, instructorId, expectedState.toString());
		return (count > 0);
	}

	@Override
	@Transactional(readOnly = true, propagation = Propagation.REQUIRED)
	public List<Instruction> findInstructionsForState(InstructionState state) {
		return getJdbcTemplate().query(getSqlResource(RESOURCE_SQL_SELECT_INSTRUCTION_FOR_STATE), ps -> {
			ps.setString(1, state.toString());
			JdbcUtils.setUtcTimestampStatementValue(ps, 2, Instant.now());
		}, (ResultSetExtractor<List<Instruction>>) rs -> extractInstructions(rs));
	}

	@Override
	@Transactional(readOnly = true, propagation = Propagation.REQUIRED)
	public List<Instruction> findInstructionsForStateAndParent(InstructionState state,
			String parentInstructorId, Long parentInstructionId) {
		return getJdbcTemplate().query(
				getSqlResource(RESOURCE_SQL_SELECT_INSTRUCTION_FOR_STATE_AND_2_PARAMS),
				new ResultSetExtractor<List<Instruction>>() {

					@Override
					public List<Instruction> extractData(ResultSet rs)
							throws SQLException, DataAccessException {
						return extractInstructions(rs);
					}
				}, Instruction.PARAM_PARENT_INSTRUCTOR_ID, parentInstructorId,
				Instruction.PARAM_PARENT_INSTRUCTION_ID, parentInstructionId.toString(),
				state.toString());
	}

	@Override
	@Transactional(readOnly = true, propagation = Propagation.REQUIRED)
	public List<Instruction> findInstructionsForAcknowledgement() {
		return getJdbcTemplate().query(
				getSqlResource(RESOURCE_SQL_SELECT_INSTRUCTION_FOR_ACKNOWEDGEMENT),
				new ResultSetExtractor<List<Instruction>>() {

					@Override
					public List<Instruction> extractData(ResultSet rs)
							throws SQLException, DataAccessException {
						return extractInstructions(rs);
					}
				}, Instruction.LOCAL_INSTRUCTION_ID);
	}

	@Override
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	public int deleteHandledInstructionsOlderThan(final int hours) {
		return getJdbcTemplate().update(new PreparedStatementCreator() {

			@Override
			public PreparedStatement createPreparedStatement(Connection con) throws SQLException {
				final String sql = getSqlResource(RESOURCE_SQL_DELETE_OLD);
				log.debug("Preparing SQL to delete old instructions [{}] with hours [{}]", sql, hours);
				PreparedStatement ps = con.prepareStatement(sql);
				JdbcUtils.setUtcTimestampStatementValue(ps, 1, Instant.now().minus(hours, ChronoUnit.HOURS));
				ps.setString(2, Instruction.LOCAL_INSTRUCTION_ID);
				return ps;
			}
		});
	}

	/**
	 * Set a maximum length for all instruction status result parameter values.
	 *
	 * <p>
	 * This is to help work around exceedingly long error message parameters.
	 * </p>
	 *
	 * @param maxInstructionStatusParamLength
	 *        the maximum length; defaults to
	 *        {@link #DEFAULT_MAX_INSTRUCTION_STATUS_PARAM_LENGTH}
	 * @since 1.1
	 */
	public void setMaxInstructionStatusParamLength(int maxInstructionStatusParamLength) {
		this.maxInstructionStatusParamLength = maxInstructionStatusParamLength;
	}

}
