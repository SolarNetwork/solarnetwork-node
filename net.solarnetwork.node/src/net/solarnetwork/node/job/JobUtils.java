/* ==================================================================
 * JobUtils.java - 29/10/2019 11:00:03 am
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

package net.solarnetwork.node.job;

import java.security.SecureRandom;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.Trigger;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.scheduling.support.PeriodicTrigger;

/**
 * Utility methods for working with scheduled jobs.
 *
 * @author matt
 * @version 2.1
 * @since 1.71
 */
public class JobUtils {

	/** A pattern to match a digit-only second field number value. */
	public static final Pattern CRON_PLAIN_SECOND_FIELD_PATTERN = Pattern.compile("^\\s*\\d+(?=\\s+)");

	private static final Logger log = LoggerFactory.getLogger(JobUtils.class);

	private JobUtils() {
		// not available
	}

	/**
	 * Create a trigger from a schedule expression.
	 *
	 * <p>
	 * The {@code expression} can be either an integer number representing a
	 * {@code timeUnit} frequency or else a cron expression. If
	 * {@code randomize} is {@literal true} then if the cron expression seconds
	 * value is a constant value it
	 * </p>
	 *
	 * @param expression
	 *        the schedule expression
	 * @param timeUnit
	 *        the time unit to use for periodic triggers
	 * @param randomized
	 *        {@literal true} to randomize the second field of cron triggers
	 * @return the trigger, or {@literal null} if the expression cannot be
	 *         parsed into one
	 * @since 2.0
	 */
	public static Trigger triggerForExpression(final String expression, TimeUnit timeUnit,
			boolean randomized) {
		if ( expression != null ) {
			try {
				try {
					long frequency = Long.parseLong(expression);
					PeriodicTrigger trigger = new PeriodicTrigger(
							Duration.of(frequency, timeUnit.toChronoUnit()));
					trigger.setFixedRate(true);
					return trigger;
				} catch ( NumberFormatException e ) {
					// ignore
				}
				String cronExpr = expression;
				if ( randomized ) {
					Matcher m = CRON_PLAIN_SECOND_FIELD_PATTERN.matcher(expression);
					if ( m.find() ) {
						int randSec = new SecureRandom().nextInt(60);
						cronExpr = String.valueOf(randSec) + expression.substring(m.end());
					}
				}
				return new CronTrigger(cronExpr);
			} catch ( IllegalArgumentException e ) {
				log.warn("Error parsing cron expression [{}]: {}", expression, e.getMessage());
			}
		}
		return null;
	}

}
