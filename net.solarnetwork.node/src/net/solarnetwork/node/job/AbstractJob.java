/* ===================================================================
 * AbstractJob.java
 * 
 * Created Dec 1, 2009 10:26:45 AM
 * 
 * Copyright 2007-2009 SolarNetwork.net Dev Team
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
 * ===================================================================
 */

package net.solarnetwork.node.job;

import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.ClassUtils;

/**
 * Abstract Quartz job to handle exceptions in a consistent fashion.
 * 
 * @author matt
 * @version 1.2
 */
public abstract class AbstractJob implements Job {

	/** A class-level logger. */
	protected final Logger log = LoggerFactory.getLogger(getClass());

	private String name = ClassUtils.getShortName(getClass());

	private boolean throwExceptions = false;

	@Override
	public final void execute(JobExecutionContext jobContext) throws JobExecutionException {
		try {
			executeInternal(jobContext);
		} catch ( Throwable e ) {
			logThrowable(e);
			if ( throwExceptions ) {
				if ( e instanceof JobExecutionException ) {
					throw (JobExecutionException) e;
				} else if ( e instanceof RuntimeException ) {
					throw (RuntimeException) e;
				} else if ( e instanceof Error ) {
					throw (Error) e;
				}
			}
		}
	}

	/**
	 * Helper method for logging a Throwable.
	 * 
	 * @param e
	 *        the exception
	 */
	protected void logThrowable(Throwable e) {
		final String name = (getName() == null ? getClass().getSimpleName() : getName());
		Throwable root = e;
		while ( root.getCause() != null ) {
			root = root.getCause();
		}
		final Object[] logParams;
		if ( log.isInfoEnabled() ) {
			// include stack trace with log message in Info
			logParams = new Object[] { root.getClass().getSimpleName(), name, e.toString(), e };
		} else {
			logParams = new Object[] { root.getClass().getSimpleName(), name, e.getMessage() };
		}
		log.error("{} in job {}: {}", logParams);
	}

	/**
	 * Execute the job.
	 * 
	 * @param jobContext
	 *        the job context
	 * @throws Exception
	 *         for any error
	 */
	protected abstract void executeInternal(JobExecutionContext jobContext) throws Exception;

	/**
	 * @return the name
	 */
	public String getName() {
		return name;
	}

	/**
	 * @param name
	 *        the name to set
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * Get the {@code throwExceptions} property value.
	 * 
	 * @return the value
	 * @since 1.2
	 */
	public boolean isThrowExceptions() {
		return throwExceptions;
	}

	/**
	 * Control if captured exceptions should be re-thrown or simply logged.
	 * 
	 * @param throwExceptions
	 *        if {@code false} (the default) then simply log exceptions,
	 *        otherwise re-throw exceptions after logging them
	 * @since 1.2
	 */
	public void setThrowExceptions(boolean throwExceptions) {
		this.throwExceptions = throwExceptions;
	}

}
