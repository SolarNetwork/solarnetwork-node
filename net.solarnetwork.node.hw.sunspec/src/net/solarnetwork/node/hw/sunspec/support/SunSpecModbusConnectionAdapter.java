/* ==================================================================
 * SunSpecModbusConnectionAdapter.java - 9 Oct 2026 5:36:18 pm
 *
 * Copyright 2026 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.hw.sunspec.support;

import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.io.Closeable;
import java.io.IOException;
import java.nio.charset.Charset;
import org.jspecify.annotations.Nullable;
import net.solarnetwork.node.io.modbus.ModbusReadFunction;
import net.solarnetwork.node.io.modbus.ModbusWriteFunction;
import net.solarnetwork.sunspec.modbus.ModbusConnection;
import net.solarnetwork.sunspec.modbus.ModbusReadingFunction;
import net.solarnetwork.sunspec.modbus.ModbusWritingFunction;

/**
 * Lightweight SunSpec Modbus connection adapter that delegates to a SolarNode
 * Modbus connection.
 *
 * <p>
 * This adapter is meant to be able to be created and discarded quickly after
 * use, under the assumption that the given delegate connection manages the
 * physical network state.
 * </p>
 *
 * @author matt
 * @version 1.0
 * @since 6.0
 */
public class SunSpecModbusConnectionAdapter implements ModbusConnection, Closeable {

	private final net.solarnetwork.node.io.modbus.ModbusConnection delegate;
	private final boolean closeable;

	/**
	 * Constructor.
	 *
	 * <p>
	 * The {@code closeable} property will be {@code false}.
	 * </p>
	 *
	 * @param delegate
	 *        the delegate connection
	 * @throws IllegalArgumentException
	 *         if any argument is {@code null}
	 * @see SunSpecModbusConnectionAdapter#SunSpecModbusConnectionAdapter(net.solarnetwork.node.io.modbus.ModbusConnection,
	 *      boolean)
	 */
	public SunSpecModbusConnectionAdapter(net.solarnetwork.node.io.modbus.ModbusConnection delegate) {
		this(delegate, false);
	}

	/**
	 * Constructor.
	 *
	 * @param delegate
	 *        the delegate connection
	 * @param closeable
	 *        {@code true} to invoke {@code close} on the given {@code delegate}
	 *        when {@link #close()} is called, otherwise {@link #close()} does
	 *        nothing
	 * @throws IllegalArgumentException
	 *         if any argument is {@code null}
	 */
	public SunSpecModbusConnectionAdapter(net.solarnetwork.node.io.modbus.ModbusConnection delegate,
			boolean closeable) {
		super();
		this.delegate = requireNonNullArgument(delegate, "delegate");
		this.closeable = closeable;
	}

	/**
	 * {@inheritDoc}
	 *
	 * <p>
	 * This method will only call the delegate's {@code close()} method if the
	 * instance was created with {@code closeable} set to {@code true}.
	 * </p>
	 */
	@Override
	public void close() throws IOException {
		if ( closeable ) {
			delegate.close();
		}
	}

	@Override
	public int getUnitId() {
		return delegate.getUnitId();
	}

	@Override
	public short[] readWords(ModbusReadingFunction function, int address, int count) throws IOException {
		return delegate.readWords(ModbusReadFunction.forCode(function.getCode()), address, count);
	}

	@Override
	public void writeWords(ModbusWritingFunction function, int address, short[] values)
			throws IOException {
		delegate.writeWords(ModbusWriteFunction.forCode(function.getCode()), address, values);
	}

	@Override
	public @Nullable String readString(ModbusReadingFunction function, int address, int count,
			boolean trim, Charset charset) throws IOException {
		return delegate.readString(ModbusReadFunction.forCode(function.getCode()), address, count, trim,
				charset);
	}

}
