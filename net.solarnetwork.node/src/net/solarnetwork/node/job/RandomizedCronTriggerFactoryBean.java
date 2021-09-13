/* ==================================================================
 * RandomizedCronTriggerFactoryBean.java - Oct 8, 2011 7:36:23 PM
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

package net.solarnetwork.node.job;

import java.text.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.quartz.CronTriggerFactoryBean;

/**
 * Extension of {@link CronTriggerFactoryBean} that can randomize specific
 * fields of the cron expression to distribute a job across several nodes over
 * time.
 * 
 * <p>
 * The cron expression is only randomized if the
 * {@link #setCronExpression(String)} method is used to set the cron expression.
 * Additionally, if a simple long string value is configured, a cron expression
 * of {@literal 0 * * * * ?} will be used, but the
 * {@link #BASE_CRON_EXPRESSION_KEY} job map value will be set to the original
 * long string so the code scheduling the job might look for that and use a
 * simple trigger instead of the cron.
 * </p>
 * 
 * @author matt
 * @version 2.2
 */
public class RandomizedCronTriggerFactoryBean extends CronTriggerFactoryBean {

	/**
	 * A trigger job data map key for the original cron expression before
	 * randomization was applied.
	 */
	public static final String BASE_CRON_EXPRESSION_KEY = "net.solarnetwork.node.job.BASE_CRON_EXPRESSION";

	/**
	 * A trigger job data map key for a boolean flag indicating a random second
	 * should be used.
	 */
	public static final String RANDOMIZED_SECOND = "net.solarnetwork.node.job.RANDOM_SECOND";

	private boolean randomSecond = true;
	private String baseCronExpression;
	private String finalCronExpression;

	private final Logger log = LoggerFactory.getLogger(getClass());

	@Override
	public void setCronExpression(String cronExpression) {
		this.baseCronExpression = cronExpression;
		this.finalCronExpression = cronExpression;
		// look for plain number as milliseconds; we will trick ourselves into millisecond interval
		try {
			Long.parseLong(cronExpression);

			// convert to "cron" of every minute, will generate random second
			cronExpression = "0 * * * * ?";
		} catch ( NumberFormatException e ) {
			// ignore
		}
		if ( randomSecond ) {
			String newExpression = randomizeCronSecond(cronExpression);
			if ( !newExpression.equals(cronExpression) ) {
				log.debug("Randomized seconds of cron expression to {}", newExpression);
				finalCronExpression = newExpression;
			}
		}
		super.setCronExpression(finalCronExpression);
	}

	/**
	 * Randomize the seconds of a cron expression.
	 * 
	 * @param expression
	 *        the expression to randomize the seconds on
	 * @return the new expression, or the original expression if
	 *         {@code expression} does not appear to be a cron expression
	 * @since 2.2
	 */
	public static String randomizeCronSecond(String expression) {
		try {
			// don't randomize value if already just a number
			Long.parseLong(expression);
			return expression;
		} catch ( NumberFormatException e ) {
			// ignore
		}
		int seconds = (int) Math.floor(Math.random() * 60);
		return expression.replaceAll("^\\s*\\d+(\\s+)", String.valueOf(seconds) + "$1");
	}

	@Override
	public void afterPropertiesSet() throws ParseException {
		if ( baseCronExpression != null && !baseCronExpression.equals(finalCronExpression) ) {
			// add our base expression to the job data map, so we know at runtime it was altered
			getJobDataMap().put(BASE_CRON_EXPRESSION_KEY, baseCronExpression);
		}
		getJobDataMap().put(RANDOMIZED_SECOND, randomSecond);
		super.afterPropertiesSet();
	}

	public boolean isRandomSecond() {
		return randomSecond;
	}

	/**
	 * Set a flag to randomize the second field of the configured cron
	 * expression.
	 * 
	 * If {@literal true} then set the seconds value of the cron expression will
	 * be set to a random value between 0-59. Will only replace the second value
	 * if a simple second value is specified in the cron expression. For example
	 * if the second value is {@code 0} it will be randomized, while if it is
	 * {@code 0/5} it will not. Defaults to {@literal true}.
	 * 
	 * @param randomSecond
	 *        The random second flag value to set.
	 */
	public void setRandomSecond(boolean randomSecond) {
		this.randomSecond = randomSecond;
	}

	/**
	 * Get the random second flag.
	 * 
	 * @return {@literal true} if seconds should be randomized.
	 */
	public String getBaseCronExpression() {
		return baseCronExpression;
	}

}
