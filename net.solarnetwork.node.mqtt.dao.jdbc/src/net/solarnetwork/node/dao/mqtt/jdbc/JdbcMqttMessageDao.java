/* ==================================================================
 * JdbcMqttMessageDao.java - 11/06/2021 4:10:17 PM
 * 
 * Copyright 2021 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.dao.mqtt.jdbc;

import static java.lang.String.format;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import org.springframework.jdbc.core.RowMapper;
import net.solarnetwork.common.mqtt.MqttQos;
import net.solarnetwork.common.mqtt.dao.BasicMqttMessageEntity;
import net.solarnetwork.common.mqtt.dao.MqttMessageDao;
import net.solarnetwork.common.mqtt.dao.MqttMessageEntity;
import net.solarnetwork.domain.SortDescriptor;
import net.solarnetwork.node.dao.jdbc.BaseJdbcGenericDao;

/**
 * JDBC implementation of {@link MqttMessageDao}.
 * 
 * @author matt
 * @version 1.0
 */
public class JdbcMqttMessageDao extends BaseJdbcGenericDao<MqttMessageEntity, Long>
		implements MqttMessageDao {

	/**
	 * The default SQL template for the {@code sqlGetTablesVersion} property.
	 * The {@link #getTableName()} value is used in the pattern, e.g.
	 * {@code T-init.sql}.
	 */
	public static final String SQL_GET_TABLES_VERSION_TEMPLATE = "SELECT svalue FROM solarnode.mqtt_message_meta WHERE skey = 'solarnode.%s.version'";

	/** The table name for {@link MqttMessageEntity} entities. */
	public static final String TABLE_NAME = "message";

	/** The charge point table version. */
	public static final int VERSION = 1;

	/**
	 * Constructor.
	 */
	public JdbcMqttMessageDao() {
		super(BasicMqttMessageEntity.class, Long.class, new MqttMessageEntityRowMapper(), "mqtt_%s",
				TABLE_NAME, VERSION);
		setSqlGetTablesVersion(format(SQL_GET_TABLES_VERSION_TEMPLATE, getTableName()));
	}

	/**
	 * A row mapper for {@link MqttMessageEntity} entities.
	 */
	public static final class MqttMessageEntityRowMapper implements RowMapper<MqttMessageEntity> {

		@Override
		public MqttMessageEntity mapRow(ResultSet rs, int rowNum) throws SQLException {
			Long id = rs.getLong(1);
			Instant created = getInstantColumn(rs, 2);
			String topic = rs.getString(3);
			boolean retained = rs.getBoolean(4);
			MqttQos qos = MqttQos.valueOf(rs.getInt(5));
			byte[] payload = rs.getBytes(6);

			return new BasicMqttMessageEntity(id, created, topic, retained, qos, payload);
		}

	}

	@Override
	public Collection<MqttMessageEntity> getRange(List<SortDescriptor> sorts, Integer offset,
			Integer max) {
		// TODO Auto-generated method stub
		return null;
	}

}
