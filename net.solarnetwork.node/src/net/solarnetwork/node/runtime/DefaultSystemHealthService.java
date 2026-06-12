/* ==================================================================
 * DefaultSystemHealthService.java - 14/12/2021 11:24:45 AM
 *
 * Copyright 2021 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.runtime;

import static net.solarnetwork.util.ObjectUtils.nonnull;
import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import org.jspecify.annotations.Nullable;
import org.springframework.context.MessageSource;
import org.springframework.scheduling.concurrent.CustomizableThreadFactory;
import net.solarnetwork.node.service.SystemHealthService;
import net.solarnetwork.service.PingTest;
import net.solarnetwork.service.PingTestResult;
import net.solarnetwork.service.PingTestResultDisplay;
import net.solarnetwork.service.support.BasicIdentifiable;
import net.solarnetwork.util.StringUtils;

/**
 * Default implementation of {@link SystemHealthService}.
 *
 * @author matt
 * @version 1.0
 * @since 2.2
 */
public class DefaultSystemHealthService extends BasicIdentifiable implements SystemHealthService {

	private static final ExecutorService EXECUTOR = Executors
			.newCachedThreadPool(new CustomizableThreadFactory("Ping-"));

	private final List<PingTest> tests;

	/**
	 * Constructor.
	 *
	 * @param tests
	 *        the list of all available tests
	 * @throws IllegalArgumentException
	 *         if any argument is {@code null}
	 */
	public DefaultSystemHealthService(List<PingTest> tests) {
		super();
		this.tests = requireNonNullArgument(tests, "tests");
	}

	@Override
	public PingTestResults performPingTests(@Nullable Set<String> pingTestIds) {
		final Instant start = Instant.now();
		Map<String, PingTestResultDisplay> results = new TreeMap<>();
		List<PingTest> runTests = new ArrayList<>();
		if ( pingTestIds == null || pingTestIds.isEmpty() ) {
			runTests.addAll(tests);
		} else {
			Pattern[] idExprs = nonnull(StringUtils.patterns(pingTestIds.stream().toArray(String[]::new),
					Pattern.CASE_INSENSITIVE), "Test ID patterns");
			for ( PingTest test : tests ) {
				try {
					for ( Pattern p : idExprs ) {
						if ( p.matcher(test.getPingTestId()).find() ) {
							runTests.add(test);
							break;
						}
					}
				} catch ( PatternSyntaxException e ) {
					final MessageSource msgSrc = getMessageSource();
					final String defaultMsg = "Invalid test ID pattern.";
					String msg = (msgSrc != null ? msgSrc.getMessage("error.invalidIdPattern",
							new Object[] { e.getPattern(), e.getMessage() }, defaultMsg,
							Locale.getDefault()) : defaultMsg);
					PingTestResult r = new PingTestResult(false, msg);
					PingTestResultDisplay rd = new PingTestResultDisplay(test, r, Instant.now());
					results.put("__error.idPattern", rd);
				}
			}
		}
		for ( PingTest t : runTests ) {
			final Instant testStart = Instant.now();
			PingTest.Result pingTestResult = null;
			Future<PingTest.Result> f = null;
			try {
				f = EXECUTOR.submit(new Callable<PingTest.Result>() {

					@Override
					public PingTest.Result call() throws Exception {
						return t.performPingTest();
					}
				});
				pingTestResult = f.get(t.getPingTestMaximumExecutionMilliseconds(),
						TimeUnit.MILLISECONDS);
			} catch ( TimeoutException e ) {
				if ( f != null ) {
					f.cancel(true);
				}
				pingTestResult = new PingTestResult(false, "Timeout: no result provided within "
						+ t.getPingTestMaximumExecutionMilliseconds() + "ms");
			} catch ( Throwable e ) {
				Throwable root = e;
				while ( root.getCause() != null ) {
					root = root.getCause();
				}
				pingTestResult = new PingTestResult(false, "Exception: " + root.toString());
			} finally {
				if ( pingTestResult != null ) {
					results.put(t.getPingTestId(),
							new PingTestResultDisplay(t, pingTestResult, testStart));
				}
			}
		}
		return new PingTestResults(start, results);
	}

}
