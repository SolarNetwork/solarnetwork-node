/* ==================================================================
 * LenientAggregationFormatter.java - 13 Nov 2026 3:16:42 pm
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

package net.solarnetwork.node.setup.web.support;

import java.util.Locale;
import org.jspecify.annotations.Nullable;
import org.springframework.format.Formatter;
import net.solarnetwork.domain.datum.Aggregation;

/**
 * A {@link Formatter} for {@link Aggregation} that parses an unknown value as
 * {@code null} rather than failing.
 *
 * <p>
 * Values are parsed with {@link Aggregation#forKey(String)}, so either a key or
 * a name is accepted, in any case.
 * </p>
 *
 * @author matt
 * @version 1.0
 * @since 5.4
 */
public class LenientAggregationFormatter implements Formatter<Aggregation> {

	/** A shared instance. */
	public static final LenientAggregationFormatter INSTANCE = new LenientAggregationFormatter();

	/**
	 * Constructor.
	 */
	public LenientAggregationFormatter() {
		super();
	}

	@Override
	public String print(Aggregation object, Locale locale) {
		return object.name();
	}

	/**
	 * {@inheritDoc}
	 *
	 * @return the aggregation, or {@code null} if {@code text} is not a known
	 *         aggregation
	 */
	@Override
	public @Nullable Aggregation parse(String text, Locale locale) {
		try {
			return Aggregation.forKey(text);
		} catch ( IllegalArgumentException e ) {
			return null;
		}
	}

}
