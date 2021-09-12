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

import java.util.Collections;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.solarnetwork.node.domain.datum.NodeDatum;
import net.solarnetwork.node.io.modbus.ModbusData;
import net.solarnetwork.service.ExpressionService;
import net.solarnetwork.util.IntRangeSet;

/**
 * An object to use as the "root" for {@link ExpressionService} evaluation.
 * 
 * @author matt
 * @version 3.0
 */
public class ExpressionRoot extends net.solarnetwork.node.domain.ExpressionRoot {

	/** A pattern for matching references to register numbers. */
	public static final Pattern REGISTER_REF = Pattern.compile("regs\\[(\\d+)\\]");

	/**
	 * A pattern for matching references to register numbers via getter methods
	 * on {@link #getSample()}, with up to 1, 2, or 4 captured integer
	 * arguments.
	 * 
	 * <p>
	 * This is designed to work with expressions like
	 * {@code sample.getInt32(1, 2)} and {@code sample.getInt64(1, 2, 3, 4)},
	 * returning the referenced register address arguments as captured values.
	 * </p>
	 */
	public static final Pattern SAMPLE_GETTER_REF = Pattern.compile(
			"sample\\.get\\w+?(?<!Bytes|String)\\(\\s*(\\d+?)(?:\\s*,\\s*(\\d+))?(?:\\s*,\\s*(\\d+)\\s*,\\s*(\\d+))?\\s*\\)");

	/**
	 * A pattern for matching references to register ranges via getter methods
	 * on {@link #getSample()}.
	 * 
	 * <p>
	 * This is designed to work with expressions like
	 * {@code sample.getUtf8String(0, 8, true)} and
	 * {@code sample.getBytes(0, 8)}, returning the referenced register address
	 * arguments as captured range values.
	 * </p>
	 */
	public static final Pattern SAMPLE_RANGE_GETTER_REF = Pattern.compile(
			"sample\\.get(?:Bytes|.*?String)\\(\\s*(\\d+?)(?:\\s*,\\s*(\\d+))?(?:\\s*,\\s*(?:true|false))?\\s*\\)");

	private final ModbusData sample;
	private final Map<Integer, Integer> sampleUnsignedData;

	/**
	 * Constructor.
	 * 
	 * @param datum
	 *        the datum currently being populated
	 * @param sample
	 *        the current Modbus sample data
	 */
	public ExpressionRoot(NodeDatum datum, ModbusData sample) {
		super(datum);
		this.sample = sample;
		this.sampleUnsignedData = (sample != null ? sample.getUnsignedDataMap()
				: Collections.emptyMap());
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
			for ( int i = 1; i <= m.groupCount(); i++ ) {
				String s = m.group(i);
				if ( s != null ) {
					result.add(Integer.parseInt(m.group(i)));
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
	 * @return the sample
	 */
	public ModbusData getSample() {
		return sample;
	}

	/**
	 * Alias for {@code sample.getUnsignedDataMap()}.
	 * 
	 * @return the sample data as unsigned integer values, never {@literal null}
	 */
	public Map<Integer, Integer> getRegs() {
		return sampleUnsignedData;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("ModbusExpressionRoot{");
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
