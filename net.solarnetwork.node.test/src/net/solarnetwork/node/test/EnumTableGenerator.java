/* ==================================================================
 * EnumTableGenerator.java - 16/03/2023 6:53:37 am
 * 
 * Copyright 2023 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.test;

import java.io.PrintStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import net.solarnetwork.domain.Bitmaskable;
import net.solarnetwork.domain.CodedValue;
import net.solarnetwork.domain.GroupedBitmaskable;

/**
 * Utility to generate a Markdown table out of enum descriptions.
 * 
 * <p>
 * Special support for {@link GroupedBitmaskable}, {@link Bitmaskable}, and
 * {@link CodedValue} is provided. If an enum class implements one of those
 * interfaces, the generated index will be derived from
 * {@link GroupedBitmaskable#getOverallIndex()},
 * {@link Bitmaskable#bitmaskBitOffset()}, or {@link CodedValue#getCode()}.
 * </p>
 * 
 * @author matt
 * @version 1.0
 */
public class EnumTableGenerator {

	private static final String[] HEADERS = new String[] { "Index", "Name", "Description" };

	private final int[] widths = new int[] { HEADERS[0].length(), HEADERS[1].length(),
			HEADERS[2].length() };
	private final List<Class<? extends Enum<?>>> enumClasses = new ArrayList<>(4);
	private final List<Method> descriptionMethods = new ArrayList<>(4);
	private final PrintStream out;

	private final List<Row> rows = new ArrayList<>(16);
	private boolean withoutGroups;

	/**
	 * Constructor.
	 * 
	 * @param out
	 *        the output stream
	 */
	public EnumTableGenerator(PrintStream out) {
		super();
		this.out = Objects.requireNonNull(out);
	}

	private static final class Row {

		private final int index;
		private final String[] data;

		private Row(int index, String[] data) {
			super();
			this.index = index;
			this.data = data;
		}
	}

	/**
	 * Add an enum.
	 * 
	 * @param enumClass
	 *        the enum class to add
	 */
	public void add(Class<? extends Enum<?>> enumClass) {
		enumClasses.add(enumClass);
		descriptionMethods.add(descriptionMethod(enumClass));
	}

	private static final Method descriptionMethod(Class<? extends Enum<?>> enumClass) {
		try {
			return enumClass.getMethod("getDescription");
		} catch ( NoSuchMethodException | SecurityException e ) {
			// ignore
		}
		return null;
	}

	private int index(Enum<?> e) {
		if ( !withoutGroups && e instanceof GroupedBitmaskable ) {
			return ((GroupedBitmaskable) e).getOverallIndex();
		} else if ( e instanceof Bitmaskable ) {
			return ((Bitmaskable) e).bitmaskBitOffset();
		} else if ( e instanceof CodedValue ) {
			return ((CodedValue) e).getCode();
		}
		return e.ordinal();
	}

	private String description(int i, Enum<?> e) {
		Method m = descriptionMethods.get(i);
		if ( m != null ) {
			try {
				Object desc = m.invoke(e);
				if ( desc != null ) {
					return desc.toString();
				}
			} catch ( IllegalAccessException | IllegalArgumentException
					| InvocationTargetException e1 ) {
				// ignore
			}
		}
		return e.toString();
	}

	/**
	 * Generate the Markdown table.
	 */
	public void generate() {
		for ( int enumIndex = 0, enumCount = enumClasses.size(); enumIndex < enumCount; enumIndex++ ) {
			Enum<?>[] enums = enumClasses.get(enumIndex).getEnumConstants();

			// calculate row content
			for ( int i = 0, len = enums.length; i < len; i++ ) {
				final String[] row = new String[3];
				final Enum<?> e = enums[i];
				final int idx = index(e);

				row[0] = Integer.toString(idx);
				int w = row[0].length();
				if ( w > widths[0] ) {
					widths[i] = w;
				}

				row[1] = "`" + e.name() + "`";
				w = row[1].length();
				if ( w > widths[1] ) {
					widths[1] = w;
				}

				row[2] = description(enumIndex, e);
				w = row[2].length();
				if ( w > widths[2] ) {
					widths[2] = w;
				}

				rows.add(new Row(idx, row));
			}
		}

		// sort
		Collections.sort(rows, new Comparator<Row>() {

			@Override
			public int compare(Row o1, Row o2) {
				int result = Integer.compare(o1.index, o2.index);
				if ( result != 0 ) {
					return result;
				}
				return o1.data[2].compareTo(o2.data[2]);
			}
		});

		// print header
		String fmt = "| %-" + widths[0] + "s | %-" + widths[1] + "s | %-" + widths[2] + "s |";
		out.println(String.format(fmt, (Object[]) HEADERS));
		for ( int i = 0; i < widths.length; i++ ) {
			out.print("|:");
			int len = widths[i] + 1;
			for ( int j = 0; j < len; j++ ) {
				out.print('-');
			}
		}
		out.println('|');

		// print rows
		for ( Row row : rows ) {
			out.println(String.format(fmt, (Object[]) row.data));
		}
	}

	/**
	 * Turn off grouped bitmask support.
	 * 
	 * @param withoutGroups
	 *        {@literal true} to ignore grouped bitmasks
	 */
	public void setWithoutGroups(boolean withoutGroups) {
		this.withoutGroups = withoutGroups;
	}

	/**
	 * Generate a Markdown table out of enum descriptions.
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		if ( args.length < 1 ) {
			System.err.println("Pass enum class name arguments to generate table(s) for.");
			System.exit(1);
		}
		final EnumTableGenerator gen = new EnumTableGenerator(System.out);
		for ( String arg : args ) {
			if ( "--nogroups".equals(arg) ) {
				gen.setWithoutGroups(true);
			}
			if ( arg.startsWith("--") ) {
				continue;
			}
			try {
				@SuppressWarnings({ "rawtypes", "unchecked" })
				Class<? extends Enum<?>> enumClass = (Class) Class.forName(arg);
				Object[] enums = enumClass.getEnumConstants();
				if ( enums == null ) {
					System.err.println("Class [" + arg + "] is not an enum.");
					continue;
				}
				gen.add(enumClass);
			} catch ( ClassNotFoundException e ) {
				System.err.println("Class [" + arg + "] not found.");
			} catch ( ClassCastException e ) {
				System.err.println("Class [" + arg + "] is not an enum.");
				continue;
			}
		}
		gen.generate();
	}

}
