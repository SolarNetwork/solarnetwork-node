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
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
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
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.solarnetwork.node.dao.jdbc.AbstractJdbcDao;
import net.solarnetwork.node.reactor.Instruction;
import net.solarnetwork.node.reactor.InstructionStatus;
import net.solarnetwork.node.reactor.InstructionStatus.InstructionState;
import net.solarnetwork.node.reactor.support.BasicInstruction;
import net.solarnetwork.node.reactor.support.BasicInstructionStatus;

/**
 * JDBC implementation of {@link JdbcInstructionDao}.
 * 
 * @author matt
 * @version 1.2
 */
public class JdbcInstructionDao extends AbstractJdbcDao<Instruction>
		implements net.solarnetwork.node.reactor.InstructionDao {

	/** The default tables version. */
	public static final int DEFAULT_TABLES_VERSION = 2;

	/** The table name for {@link Instruction} data. */
	public static final String TABLE_INSTRUCTION = "sn_instruction";

	/** The table name for {@link Instruction} parameter data. */
	public static final String TABLE_INSTRUCTION_PARAM = "sn_instruction_param";

	/** The table name for {@link InstructionStatus} data. */
	public static final String TABLE_INSTRUCTION_STATUS = "sn_instruction_status";

	/** The default classpath Resource for the {@code initSqlResource}. */
	public static final String DEFAULT_INIT_SQL = "derby-instruction-init.sql";

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
	 * unique keys.
	 */
	public static final String RESOURCE_SQL_SELECT_INSTRUCTION_FOR_KEYS = "select-for-keys";

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
	 * state.
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

	private static final class StringMapTypeReference
			extends TypeReference<LinkedHashMap<String, Object>> {

		public StringMapTypeReference() {
			super();
		}

	}

	private static final StringMapTypeReference STRING_MAP_TYPE = new StringMapTypeReference();

	private ObjectMapper objectMapper;
	private int maxInstructionStatusParamLength = DEFAULT_MAX_INSTRUCTION_STATUS_PARAM_LENGTH;

	/**
	 * Default constructor.
	 */
	public JdbcInstructionDao() {
		super();
		setTableName(TABLE_INSTRUCTION);
		setTablesVersion(DEFAULT_TABLES_VERSION);
		setSqlGetTablesVersion(DEFAULT_SQL_GET_TABLES_VERSION);
		setSqlResourcePrefix("derby-instruction");
		setInitSqlResource(new ClassPathResource(DEFAULT_INIT_SQL, getClass()));
		objectMapper = new ObjectMapper();
	}

	@Override
	public String[] getTableNames() {
		return new String[] { getTableName(), TABLE_INSTRUCTION_PARAM, TABLE_INSTRUCTION_STATUS };
	}

	@Override
	@Transactional(readOnly = true, propagation = Propagation.REQUIRED)
	public Instruction getInstruction(Long instructionId) {
		return getJdbcTemplate().query(getSqlResource(RESOURCE_SQL_SELECT_INSTRUCTION_FOR_ID),
				new Object[] { instructionId }, new ResultSetExtractor<Instruction>() {

					@Override
					public Instruction extractData(ResultSet rs)
							throws SQLException, DataAccessException {
						List<Instruction> results = extractInstructions(rs);
						if ( results.size() > 0 ) {
							return results.get(0);
						}
						return null;
					}
				});
	}

	@Override
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	public Long storeInstruction(final Instruction instruction) {
		// first store our Instruction entity
		final Long pk = storeDomainObject(instruction, getSqlResource(RESOURCE_SQL_INSERT_INSTRUCTION));

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
						ps.setLong(col++, pk);
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
		Date statusDate = new Date();
		InstructionState state = instruction.getInstructionState();
		getJdbcTemplate().update(getSqlResource(RESOURCE_SQL_INSERT_INSTRUCTION_STATUS), pk,
				new java.sql.Timestamp(statusDate.getTime()),
				(state != null ? state : InstructionState.Received).toString());
		return pk;
	}

	@Override
	protected void setStoreStatementValues(Instruction instruction, PreparedStatement ps)
			throws SQLException {
		int col = 1;
		ps.setTimestamp(col++, new java.sql.Timestamp(instruction.getInstructionDate().getTime()));
		ps.setString(col++, instruction.getRemoteInstructionId());
		ps.setString(col++, instruction.getInstructorId());
		ps.setString(col++, instruction.getTopic());
	}

	@Override
	@Transactional(readOnly = true, propagation = Propagation.REQUIRED)
	public Instruction getInstruction(String instructionId, String instructorId) {
		return getJdbcTemplate().query(getSqlResource(RESOURCE_SQL_SELECT_INSTRUCTION_FOR_KEYS),
				new Object[] { instructionId, instructorId }, new ResultSetExtractor<Instruction>() {

					@Override
					public Instruction extractData(ResultSet rs)
							throws SQLException, DataAccessException {
						List<Instruction> results = extractInstructions(rs);
						if ( results.size() > 0 ) {
							return results.get(0);
						}
						return null;
					}
				});
	}

	private List<Instruction> extractInstructions(ResultSet rs) throws SQLException {
		List<Instruction> results = new ArrayList<Instruction>(5);
		BasicInstruction bi = null;
		while ( rs.next() ) {
			long currId = rs.getLong(1);
			if ( bi == null || bi.getId().longValue() != currId ) {
				Map<String, Object> rParams = null;
				try {
					String jparams = rs.getString(8);
					if ( jparams != null && jparams.length() > 0 ) {
						rParams = objectMapper.readValue(rs.getString(8), STRING_MAP_TYPE);
					}
				} catch ( IOException e ) {
					// report but move on
					log.warn("Error deserializing reulst parameter JSON: {}", e.getMessage());
				}
				InstructionStatus status = new BasicInstructionStatus(currId,
						InstructionState.valueOf(rs.getString(6)), rs.getTimestamp(7),
						(rs.getString(9) == null ? null : InstructionState.valueOf(rs.getString(9))),
						rParams);
				bi = new BasicInstruction(currId, rs.getString(2), rs.getTimestamp(3), rs.getString(4),
						rs.getString(5), status);
				results.add(bi);
			}
			String pName = rs.getString(10);
			String pValue = rs.getString(11);
			if ( pName != null ) {
				bi.addParameter(pName, pValue);
			}
		}
		return results;
	}

	@Override
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	public void storeInstructionStatus(Long instructionId, InstructionStatus status) {
		String jparams = encodeInstructionParameters(status);
		getJdbcTemplate().update(getSqlResource(RESOURCE_SQL_UPDATE_INSTRUCTION_STATUS),
				status.getInstructionState().toString(), jparams,
				(status.getAcknowledgedInstructionState() == null ? null
						: status.getAcknowledgedInstructionState().toString()),
				instructionId);
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
			try {
				jparams = objectMapper.writeValueAsString(stringParameters);
			} catch ( JsonProcessingException e ) {
				// report and move on
				log.warn("Error serializing result parameters {} to JSON: {}", stringParameters,
						e.getMessage());
			}
		}
		return jparams;
	}

	@Override
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	public boolean compareAndStoreInstructionStatus(Long instructionId, InstructionState expectedState,
			InstructionStatus status) {
		String jparams = encodeInstructionParameters(status);
		int count = getJdbcTemplate().update(
				getSqlResource(RESOURCE_SQL_COMPARE_AND_SET_INSTRUCTION_STATUS),
				status.getInstructionState().toString(), jparams,
				(status.getAcknowledgedInstructionState() == null ? null
						: status.getAcknowledgedInstructionState().toString()),
				instructionId, expectedState.toString());
		return (count > 0);
	}

	@Override
	@Transactional(readOnly = true, propagation = Propagation.REQUIRED)
	public List<Instruction> findInstructionsForState(InstructionState state) {
		return getJdbcTemplate().query(getSqlResource(RESOURCE_SQL_SELECT_INSTRUCTION_FOR_STATE),
				new Object[] { state.toString() }, new ResultSetExtractor<List<Instruction>>() {

					@Override
					public List<Instruction> extractData(ResultSet rs)
							throws SQLException, DataAccessException {
						return extractInstructions(rs);
					}
				});
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
				});
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
				Calendar c = Calendar.getInstance();
				c.add(Calendar.HOUR, -hours);
				ps.setTimestamp(1, new Timestamp(c.getTimeInMillis()), c);
				return ps;
			}
		});
	}

	/**
	 * Set the ObjectMapper to use for serializing result parameters.
	 * 
	 * @param objectMapper
	 *        the objectMapper to set
	 */
	public void setObjectMapper(ObjectMapper objectMapper) {
		this.objectMapper = objectMapper;
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
