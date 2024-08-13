/* ==================================================================
 * TariffScheduleUtils.java - 6/08/2024 6:22:44 am
 *
 * Copyright 2024 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.service.support;

import static java.time.format.TextStyle.SHORT;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.time.temporal.ChronoField;
import java.util.Collection;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import org.springframework.context.MessageSource;
import net.solarnetwork.domain.tariff.ChronoFieldsTariff;
import net.solarnetwork.domain.tariff.CompositeTariff;
import net.solarnetwork.domain.tariff.Tariff;
import net.solarnetwork.domain.tariff.Tariff.Rate;
import net.solarnetwork.domain.tariff.TariffSchedule;
import net.solarnetwork.domain.tariff.TemporalTariffEvaluator;

/**
 * Utilities for tariff schedule support.
 *
 * @author matt
 * @version 1.0
 * @since 3.17
 */
public final class TariffScheduleUtils {

	private TariffScheduleUtils() {
		// not available
	}

	/**
	 * Render a tariff schedule of {@link ChronoFieldsTariff} rules as an HTML
	 * table of all available rules.
	 *
	 * <p>
	 * The following message codes are required:
	 * </p>
	 *
	 * <ul>
	 * <li>{tariffScheduleTable.col.idx} - rule number column header label</li>
	 * <li>{tariffScheduleTable.month.idx} - month range column header
	 * label</li>
	 * <li>{tariffScheduleTable.day.idx} - day range column header label</li>
	 * <li>{tariffScheduleTable.weekday.idx} - weekday range column header
	 * label</li>
	 * <li>{tariffScheduleTable.time.idx} - time range column header label</li>
	 * </ul>
	 *
	 *
	 * @param messageSource
	 *        the message source
	 * @param schedule
	 *        the schedule to render
	 * @param date
	 *        the evaluation date
	 * @param evaluator
	 *        the evaluator
	 * @param firstOnly
	 *        {@literal true} to find only the first active rule
	 * @param locale
	 *        the locale to use
	 * @param buf
	 *        the buffer to append the HTML output to
	 * @return a mapping of active rule indexes to the associated tariff rules
	 */
	public static Map<Integer, Tariff> renderTariffScheduleTable(final MessageSource messageSource,
			final TariffSchedule schedule, final LocalDateTime date,
			final TemporalTariffEvaluator evaluator, final boolean firstOnly, final Locale locale,
			final StringBuilder buf) {
		final Collection<? extends Tariff> tariffs = schedule.rules();
		final Map<Integer, Tariff> active = new TreeMap<>();
		final CompositeTariff ct = new CompositeTariff(tariffs);
		final Map<String, Rate> rates = ct.getRates();
		buf.append(String.format(
				"<table class=\"table counts\"><thead><tr><th>%s</th><th>%s</th><th>%s</th><th>%s</th><th>%s</th>",
				messageSource.getMessage("tariffScheduleTable.col.idx", null, "Rule", locale),
				messageSource.getMessage("tariffScheduleTable.col.month", null, "Month", locale),
				messageSource.getMessage("tariffScheduleTable.col.day", null, "Day", locale),
				messageSource.getMessage("tariffScheduleTable.col.weekday", null, "Weekday", locale),
				messageSource.getMessage("tariffScheduleTable.col.time", null, "Time", locale)));
		for ( Rate r : rates.values() ) {
			buf.append("<th>").append(r.getDescription()).append("</th>");
		}
		buf.append("</tr></thead><tbody>");

		int i = 0;
		for ( Tariff tariff : tariffs ) {
			if ( !(tariff instanceof ChronoFieldsTariff) ) {
				continue;
			}
			ChronoFieldsTariff t = (ChronoFieldsTariff) tariff;
			if ( (active.isEmpty() || !firstOnly) && evaluator.applies(tariff, date, null) ) {
				active.put(i, tariff);
			}
			buf.append("<tr>");
			buf.append("<th>").append(++i).append("</th>");
			buf.append("<td>").append(rangeDisplayString(ChronoField.MONTH_OF_YEAR, t, locale))
					.append("</td>");
			buf.append("<td>").append(rangeDisplayString(ChronoField.DAY_OF_MONTH, t, locale))
					.append("</td>");
			buf.append("<td>").append(rangeDisplayString(ChronoField.DAY_OF_WEEK, t, locale))
					.append("</td>");
			buf.append("<td>").append(rangeDisplayString(ChronoField.MINUTE_OF_DAY, t, locale))
					.append("</td>");
			Map<String, Rate> tariffRates = tariff.getRates();
			// iterate over global rates, to keep order consistent in case rows vary
			for ( String id : rates.keySet() ) {
				Rate r = tariffRates.get(id);
				buf.append("<td>");
				if ( r != null ) {
					buf.append(r.getAmount().toPlainString());
				}
				buf.append("</td>");
			}
			buf.append("</tr>");
		}
		buf.append("</tbody></table>");
		return active;
	}

	/**
	 * Render a field of a {@link ChronoFieldsTariff} for display.
	 *
	 * @param field
	 *        the field
	 * @param tariff
	 *        the tariff
	 * @param locale
	 *        the locale
	 * @return the display string
	 */
	public static String rangeDisplayString(ChronoField field, ChronoFieldsTariff tariff,
			Locale locale) {
		String r = tariff.formatChronoField(field, locale, SHORT);
		return (r != null ? r : "*");
	}

	/**
	 * Render a mapping of active tariff rules as an HTML list.
	 *
	 * <p>
	 * The following message codes are required:
	 * </p>
	 *
	 * <ul>
	 * <li>{@code rates.active}</li>
	 * </ul>
	 *
	 * @param messageSource
	 *        the message source
	 * @param active
	 *        the active mapping
	 * @param date
	 *        the evaluation date
	 * @param buf
	 *        the buffer to append the HTML to
	 */
	public static void renderActiveTariffList(final MessageSource messageSource,
			final Map<Integer, Tariff> active, final LocalDateTime date, final StringBuilder buf) {
		if ( active == null || active.isEmpty() ) {
			return;
		}
		Map<String, Rate> activeRates = new CompositeTariff(active.values()).getRates();
		DateTimeFormatter dateFormat = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM,
				FormatStyle.SHORT);
		buf.append("<p>").append(
				messageSource.getMessage("rates.active", new Object[] { dateFormat.format(date) }, null))
				.append("</p><ol>");
		for ( Map.Entry<Integer, Tariff> me : active.entrySet() ) {
			buf.append("<li value=\"").append(me.getKey() + 1).append("\">");
			int rateCount = 0;
			for ( Rate rate : me.getValue().getRates().values() ) {
				if ( rate == activeRates.get(rate.getId()) ) {
					// this rate active for this rule
					if ( rateCount++ > 0 ) {
						buf.append("; ");
					}
					buf.append("<b>").append(rate.getDescription()).append("</b>: ")
							.append(rate.getAmount().toPlainString());
				}
				buf.append("</li>");
			}
		}
		buf.append("</ol>");

	}

}
