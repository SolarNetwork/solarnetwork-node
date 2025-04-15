/* ==================================================================
 * LocalState.java - 14/04/2025 7:24:26 am
 *
 * Copyright 2025 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.domain;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import net.solarnetwork.dao.BasicStringEntity;
import net.solarnetwork.domain.Differentiable;

/**
 * A persistent local state entity.
 *
 * @author matt
 * @version 1.0
 * @since 3.23
 */
@JsonIgnoreProperties({ "data", "id" })
@JsonPropertyOrder({ "key", "created", "modified", "type", "value" })
public class LocalState extends BasicStringEntity implements Differentiable<LocalState>, Cloneable {

	private static final long serialVersionUID = 6892709852567164350L;

	/** The modification date. */
	private Instant modified;

	/** The data type. */
	private LocalStateType type;

	/** The data value. */
	private byte[] data;

	/** A cached decoded value. */
	private Object value;

	/**
	 * Constructor.
	 */
	public LocalState() {
		super();
	}

	/**
	 * Constructor.
	 *
	 * @param id
	 *        the ID
	 * @param created
	 *        the creation date
	 */
	public LocalState(String id, Instant created) {
		super(id, created);
	}

	/**
	 * Constructor.
	 *
	 * @param id
	 *        the ID
	 * @param type
	 *        the type
	 * @param value
	 *        the value
	 */
	public LocalState(String id, LocalStateType type, Object value) {
		this(id, Instant.now().truncatedTo(ChronoUnit.MILLIS), type, value);
	}

	/**
	 * Constructor.
	 *
	 * @param id
	 *        the ID
	 * @param created
	 *        the creation date
	 * @param type
	 *        the type
	 * @param value
	 *        the value
	 */
	public LocalState(String id, Instant created, LocalStateType type, Object value) {
		super(id, created);
		this.type = type;
		setValue(value);
	}

	@Override
	public LocalState clone() {
		return (LocalState) super.clone();
	}

	@Override
	public boolean differsFrom(LocalState other) {
		return !isSameAs(other);
	}

	/**
	 * Test if the properties of another entity are the same as in this
	 * instance.
	 *
	 * <p>
	 * The {@code id}, {@code created}, and {@code modified} properties are not
	 * compared by this method.
	 * </p>
	 *
	 * @param other
	 *        the other entity to compare to
	 * @return {@code true} if the properties of this instance are equal to the
	 *         other
	 */
	public boolean isSameAs(LocalState other) {
		if ( other == null ) {
			return false;
		}
		return type == other.type && Arrays.equals(data, other.data);
	}

	/**
	 * Get the key value.
	 *
	 * <p>
	 * This is an alias for {@link #getId()}.
	 * </p>
	 *
	 * @return the identifier
	 */
	public String getKey() {
		return getId();
	}

	/**
	 * Get the value.
	 *
	 * @return the value
	 */
	public synchronized Object getValue() {
		if ( value == null ) {
			value = decodeValue(type, data);
		}
		return value;
	}

	/**
	 * Set the value.
	 *
	 * @param value
	 *        the value to set
	 */
	public synchronized void setValue(Object value) {
		this.value = value;
		this.data = encodeValue(type, value);
	}

	public static Object decodeValue(LocalStateType type, byte[] data) {
		if ( type == null || data == null ) {
			return null;
		}
		return type.decode(data);
	}

	public static byte[] encodeValue(LocalStateType type, Object value) {
		if ( type == null || value == null ) {
			return null;
		}
		return type.encode(value);
	}

	/**
	 * Get the modification date.
	 *
	 * @return the modification date
	 */
	public Instant getModified() {
		return modified;
	}

	/**
	 * Set the modification date.
	 *
	 * @param modified
	 *        the modification date to set
	 */
	public void setModified(Instant modified) {
		this.modified = modified;
	}

	/**
	 * Get the data type.
	 *
	 * @return the type
	 */
	public LocalStateType getType() {
		return type;
	}

	/**
	 * Set the data type.
	 *
	 * @param type
	 *        the type to set
	 */
	public synchronized void setType(LocalStateType type) {
		this.type = type;
		if ( value != null ) {
			value = null;
		}
	}

	/**
	 * Get the data value.
	 *
	 * @return the data
	 */
	public byte[] getData() {
		return data;
	}

	/**
	 * Set the data value.
	 *
	 * @param data
	 *        the data to set
	 */
	public synchronized void setData(byte[] data) {
		this.data = data;
		if ( value != null ) {
			value = null;
		}
	}

}
