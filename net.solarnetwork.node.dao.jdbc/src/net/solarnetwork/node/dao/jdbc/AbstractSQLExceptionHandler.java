/* ==================================================================
 * AbstractSQLExceptionHandler.java - 22/12/2022 8:38:40 am
 *
 * Copyright 2022 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.dao.jdbc;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;
import net.solarnetwork.dao.jdbc.SQLExceptionHandler;

/**
 * Base {@link SQLExceptionHandler} with SQL state filtering support.
 *
 * @author matt
 * @version 1.0
 * @since 2.5
 */
public abstract class AbstractSQLExceptionHandler implements SQLExceptionHandler {

	/** A class-level logger. */
	protected final Logger log = LoggerFactory.getLogger(getClass());

	private @Nullable List<Pattern> sqlStatePatterns;

	/**
	 * Constructor.
	 */
	public AbstractSQLExceptionHandler() {
		super();
	}

	/**
	 * See if the root SQL exception's state value matches any configured SQL
	 * State pattern.
	 *
	 * <p>
	 * This will search the {@link #getSqlStatePatterns()} list and return the
	 * first one that matches the exception's {@link SQLException#getSQLState()}
	 * value of the root SQL exception.
	 * </p>
	 *
	 * @param e
	 *        the exception to compare
	 * @return the root exception that matched a pattern, or {@code null} if
	 *         none match or no patterns are configured
	 */
	protected @Nullable SQLException exceptionMatchingSqlStatePattern(SQLException e) {
		SQLException root = e;
		while ( root.getNextException() != null ) {
			root = root.getNextException();
		}
		String state = root.getSQLState();
		if ( state == null ) {
			return null;
		}
		List<Pattern> statePatterns = getSqlStatePatterns();
		if ( statePatterns == null || statePatterns.isEmpty() ) {
			return null;
		}
		for ( Pattern pat : statePatterns ) {
			if ( pat.matcher(state).matches() ) {
				return root;
			}
		}
		return null;
	}

	/**
	 * Get the list of regular expressions that should trigger an action by this
	 * handler.
	 *
	 * @return the list of regular expressions
	 */
	public final @Nullable List<Pattern> getSqlStatePatterns() {
		return sqlStatePatterns;
	}

	/**
	 * Set a list of regular expressions that should trigger an action by this
	 * handler.
	 *
	 * @param sqlStatePatterns
	 *        the regular expressions that should trigger an action
	 */
	public final void setSqlStatePatterns(@Nullable List<Pattern> sqlStatePatterns) {
		this.sqlStatePatterns = sqlStatePatterns;
	}

	/**
	 * Set a comma-delimited list of regular expressions that should trigger an
	 * action by this handler.
	 *
	 * @param regexes
	 *        a comma-delimited list of regular expressions that should trigger
	 *        an action
	 * @see #setSqlStatePatterns(List)
	 */
	public final void setSqlStateRegex(@Nullable String regexes) {
		List<Pattern> pats = null;
		String[] list = StringUtils.delimitedListToStringArray(regexes, ",");
		if ( regexes != null && list.length > 0 ) {
			pats = new ArrayList<Pattern>();
			for ( String regex : list ) {
				pats.add(Pattern.compile(regex));
			}
		}
		setSqlStatePatterns(pats);
	}

}
