/* ==================================================================
 * ExpressionRoot.java - 20/02/2019 10:21:55 am
 * 
 * Copyright 2019 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.datum.modbus;

import java.util.BitSet;
import java.util.Collections;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.solarnetwork.node.domain.datum.NodeDatum;
import net.solarnetwork.node.io.modbus.ModbusData;
import net.solarnetwork.node.io.modbus.ModbusRegisterData;
import net.solarnetwork.node.service.DatumService;
import net.solarnetwork.service.ExpressionService;
import net.solarnetwork.util.IntRangeSet;
import net.solarnetwork.util.IntShortMap;

/**
 * An object to use as the "root" for {@link ExpressionService} evaluation.
 * 
 * @author matt
 * @version 3.2
 */
public class ExpressionRoot extends net.solarnetwork.node.domain.ExpressionRoot {

	/** A pattern for matching references to register numbers. */
	public static final Pattern REGISTER_REF = Pattern
			.compile("(?:inputRegs|holdingRegs|regs)\\[(\\d+)\\]");

	/**
	 * A pattern for matching references to register numbers via getter methods
	 * on {@link #getInputs()}, {@link #getHoldings()}, and
	 * {@link #getSample()}, with up to 1, 2, or 4 captured integer arguments.
	 * 
	 * <p>
	 * This is designed to work with expressions like
	 * {@code sample.getInt32(1, 2)} and {@code holdings.getInt64(1, 2, 3, 4)},
	 * returning the referenced getter name followed by the register address
	 * arguments as captured values.
	 * </p>
	 */
	public static final Pattern SAMPLE_GETTER_REF = Pattern.compile(
			"(?:inputs|holdings|sample)\\.get(\\w+?(?<!Bytes|String))\\(\\s*(\\d+?)(?:\\s*,\\s*(\\d+))?(?:\\s*,\\s*(\\d+)\\s*,\\s*(\\d+))?\\s*\\)");

	/**
	 * A pattern for matching references to register ranges via getter methods
	 * on {@link #getInputs()}, {@link #getHoldings()}, and
	 * {@link #getSample()}.
	 * 
	 * <p>
	 * This is designed to work with expressions like
	 * {@code sample.getUtf8String(0, 8, true)} and
	 * {@code holdings.getBytes(0, 8)}, returning the referenced register
	 * address arguments as captured range values.
	 * </p>
	 */
	public static final Pattern SAMPLE_RANGE_GETTER_REF = Pattern.compile(
			"(?:inputs|holdings|sample)\\.get(?:Bytes|.*?String)\\(\\s*(\\d+?)(?:\\s*,\\s*(\\d+))?(?:\\s*,\\s*(?:true|false))?\\s*\\)");

	private final ModbusRegisterData data;
	private final Map<Integer, Integer> sampleUnsignedData;
	private final Map<Integer, Integer> inputUnsignedData;
	private final Map<Integer, Integer> holdingUnsignedData;

	/**
	 * Constructor.
	 * 
	 * @param datum
	 *        the datum currently being populated
	 * @param data
	 *        the current Modbus register data
	 * @param datumService
	 *        the datum service
	 */
	public ExpressionRoot(NodeDatum datum, ModbusRegisterData data, DatumService datumService) {
		super(datum, null, null, datumService);
		this.data = data;

		// for backwards-compat, populate sampleUnsignedData, trying to pick the register block with data,
		// merging the Holding block on top of the Input block if both have data
		if ( data.hasRegisterData() ) {
			IntShortMap i = data.getInputs().dataRegisters();
			this.inputUnsignedData = i.unsignedMap();
			IntShortMap h = data.getHoldings().dataRegisters();
			this.holdingUnsignedData = h.unsignedMap();
			if ( i.isEmpty() ) {
				this.sampleUnsignedData = h.unsignedMap();
			} else if ( h.isEmpty() ) {
				this.sampleUnsignedData = i.unsignedMap();
			} else {
				h.forEachOrdered((a, b) -> {
					i.putValue(a, b);
				});
				this.sampleUnsignedData = i.unsignedMap();
			}
		} else {
			this.sampleUnsignedData = Collections.emptyMap();
			this.inputUnsignedData = Collections.emptyMap();
			this.holdingUnsignedData = Collections.emptyMap();
		}
	}

	private static IntRangeSet EMPTY_SET = new IntRangeSet().immutableCopy();

	/**
	 * Get a set of referenced Modbus register addresses in the configured
	 * expression.
	 * 
	 * <p>
	 * This will look for references like
	 * {@literal regs[1] + sample.getInt32(2,3)} and return the referenced
	 * addresses, a set containing {@literal [1, 2, 3]} in this case.
	 * </p>
	 * 
	 * @param expression
	 *        the expression to inspect
	 * @return the referenced addresses, never {@literal null}
	 */
	public static IntRangeSet registerAddressReferences(String expression) {
		if ( expression == null || expression.isEmpty() ) {
			return EMPTY_SET;
		}
		IntRangeSet result = new IntRangeSet(8);
		Matcher m = ExpressionRoot.REGISTER_REF.matcher(expression);
		while ( m.find() ) {
			result.add(Integer.parseInt(m.group(1)));
		}

		m = ExpressionRoot.SAMPLE_GETTER_REF.matcher(expression);
		while ( m.find() ) {
			final int groupCount = m.groupCount();
			final String dataName = m.group(1);
			int start = -1;
			int argCount = 0;
			for ( int i = 2; i <= groupCount; i++ ) {
				String s = m.group(i);
				if ( s != null ) {
					int r = Integer.parseInt(m.group(i));
					if ( i == 2 ) {
						start = r;
					}
					result.add(r);
					argCount++;
				}
			}
			if ( argCount == 1 ) {
				// check for multi-register data type in getter name; for example getInt32(0) would actually require
				// two registers 0, 1
				if ( dataName.endsWith("32") ) {
					result.add(start + 1);
				} else if ( dataName.endsWith("64") ) {
					result.addRange(start + 1, start + 3);
				}
			}
		}

		m = ExpressionRoot.SAMPLE_RANGE_GETTER_REF.matcher(expression);
		while ( m.find() ) {
			int start = Integer.parseInt(m.group(1));
			int len = Integer.parseInt(m.group(2));
			if ( len > 0 ) {
				result.addRange(start, start + len - 1);
			}
		}
		return result;
	}

	/**
	 * Get the sample.
	 * 
	 * <p>
	 * This returns the {@code Holding} register map if it is not empty,
	 * otherwise the {@code Input} block.
	 * </p>
	 * 
	 * @return the sample
	 */
	public ModbusData getSample() {
		if ( !data.getHoldings().isEmpty() ) {
			return data.getHoldings();
		}
		return data.getInputs();
	}

	/**
	 * Get the holding register block.
	 * 
	 * @return the holding data
	 * @since 3.1
	 */
	public ModbusData getHoldings() {
		return data.getHoldings();
	}

	/**
	 * Get the input register block.
	 * 
	 * @return the input data
	 * @since 3.1
	 */
	public ModbusData getInputs() {
		return data.getInputs();
	}

	/**
	 * Get the {@link #getSample()} as an unsigned integer map.
	 * 
	 * @return the sample data as unsigned integer values, never {@literal null}
	 */
	public Map<Integer, Integer> getRegs() {
		return sampleUnsignedData;
	}

	/**
	 * Get the holding registers as an unsigned integer map.
	 * 
	 * @return the holding register data as unsigned integer values, never
	 *         {@literal null}
	 * @since 3.1
	 */
	public Map<Integer, Integer> getHoldingRegs() {
		return inputUnsignedData;
	}

	/**
	 * Get the input registers as an unsigned integer map.
	 * 
	 * @return the input register data as unsigned integer values, never
	 *         {@literal null}
	 * @since 3.1
	 */
	public Map<Integer, Integer> getInputRegs() {
		return holdingUnsignedData;
	}

	/**
	 * Get the coil registers.
	 * 
	 * @return the coils
	 * @since 3.1
	 */
	public BitSet getCoils() {
		return data.getCoils();
	}

	/**
	 * Get the discrete registers.
	 * 
	 * @return the discrete registers
	 * @since 3.1
	 */
	public BitSet getDiscretes() {
		return data.getDiscretes();
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("ModbusExpressionRoot{");
		ModbusData sample = getSample();
		if ( sample != null ) {
			builder.append("sample=");
			builder.append(sample.dataDebugString());
			builder.append(", ");
		}
		builder.append(super.toString());
		builder.append("}");
		return builder.toString();
	}

}
