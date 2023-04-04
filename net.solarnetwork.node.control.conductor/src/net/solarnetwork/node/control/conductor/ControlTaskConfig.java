/* ==================================================================
 * ControlTaskConfig.java - 4/04/2023 9:03:29 am
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

package net.solarnetwork.node.control.conductor;

import static net.solarnetwork.node.service.support.ExpressionConfig.expressionReferenceLink;
import static net.solarnetwork.service.ExpressionService.getGeneralExpressionReferenceLink;
import java.time.Duration;
import java.time.Instant;
import java.time.Period;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.expression.Expression;
import net.solarnetwork.node.service.PlaceholderService;
import net.solarnetwork.service.ExpressionService;
import net.solarnetwork.service.support.ExpressionServiceExpression;
import net.solarnetwork.settings.SettingSpecifier;
import net.solarnetwork.settings.support.BasicMultiValueSettingSpecifier;
import net.solarnetwork.settings.support.BasicTextAreaSettingSpecifier;
import net.solarnetwork.settings.support.BasicTextFieldSettingSpecifier;

/**
 * A control-related task to perform a time offset from some execution date.
 * 
 * @author matt
 * @version 1.0
 */
public class ControlTaskConfig {

	private String controlId;
	private String executionOffset;
	private String value;
	private String expressionServiceId;

	private Expression cachedExpression;

	/**
	 * Constructor.
	 */
	public ControlTaskConfig() {
		super();
	}

	/**
	 * Constructor.
	 * 
	 * @param controlId
	 *        the control ID
	 * @param executionOffset
	 *        the execution offset
	 * @param value
	 *        the value
	 * @param expressionServiceId
	 *        the expression service ID
	 */
	public ControlTaskConfig(String controlId, String executionOffset, String value,
			String expressionServiceId) {
		super();
		this.controlId = controlId;
		this.executionOffset = executionOffset;
		this.value = value;
		this.expressionServiceId = expressionServiceId;
	}

	/**
	 * Create new instance.
	 * 
	 * @param controlId
	 *        the control ID
	 * @param executionOffset
	 *        the execution offset
	 * @param value
	 *        the value
	 * @return the new instance
	 */
	public static ControlTaskConfig taskConfig(String controlId, String executionOffset, String value) {
		return new ControlTaskConfig(controlId, executionOffset, value, null);
	}

	/**
	 * Create new instance.
	 * 
	 * @param controlId
	 *        the control ID
	 * @param executionOffset
	 *        the execution offset
	 * @param value
	 *        the value
	 * @return the new instance
	 * @param expressionServiceId
	 *        the expression service ID
	 */
	public static ControlTaskConfig taskConfig(String controlId, String executionOffset, String value,
			String expressionServiceId) {
		return new ControlTaskConfig(controlId, executionOffset, value, expressionServiceId);
	}

	/**
	 * Get settings suitable for configuring an instance of this class.
	 * 
	 * @param prefix
	 *        a setting key prefix to use
	 * @param expressionServices
	 *        the available expression services
	 * @return the settings, never {@literal null}
	 */
	public static List<SettingSpecifier> settings(String prefix,
			Iterable<ExpressionService> expressionServices) {
		List<SettingSpecifier> results = new ArrayList<>(4);

		results.add(new BasicTextFieldSettingSpecifier(prefix + "controlId", null));
		results.add(new BasicTextFieldSettingSpecifier(prefix + "executionOffset", null));
		BasicTextAreaSettingSpecifier valueSetting = new BasicTextAreaSettingSpecifier(prefix + "value",
				null, true);
		valueSetting.setDescriptionArguments(new Object[] { getGeneralExpressionReferenceLink(),
				expressionReferenceLink(ControlTaskConfig.class) });
		results.add(valueSetting);

		// the expression drop-down menu (if we have expression services available)
		if ( expressionServices != null ) {
			BasicMultiValueSettingSpecifier expressionServiceId = new BasicMultiValueSettingSpecifier(
					prefix + "expressionServiceId", "");
			Map<String, String> exprServiceTitles = new LinkedHashMap<>();
			exprServiceTitles.put("", "");
			for ( ExpressionService service : expressionServices ) {
				exprServiceTitles.put(service.getUid(), service.getDisplayName());
			}
			expressionServiceId.setValueTitles(exprServiceTitles);

			// only populate settings for expressions if we have at least one ExpressionService available
			if ( !exprServiceTitles.isEmpty() ) {
				results.add(expressionServiceId);
			}
		}

		return results;
	}

	/**
	 * Test if the configuration is valid.
	 * 
	 * @return {@literal true} if the configuration is valid
	 */
	public boolean isValid() {
		return controlId != null && !controlId.trim().isEmpty() && value != null
				&& !value.trim().isEmpty();
	}

	/**
	 * Get the execution time of this task, relative to a given start time.
	 * 
	 * @param start
	 *        the start time
	 * @param placeholderService
	 *        an optional service to resolve placeholders with
	 * @param parameters
	 *        optional parameters to pass to the placeholder service
	 * @return the execution time for this task, or {@literal null} if
	 *         {@code start} is {@literal null} or {@code executionOffset}
	 *         cannot be parsed as a millisecond or {@link Period} based offset
	 */
	public Instant executionTime(Instant start, PlaceholderService placeholderService,
			Map<String, ?> parameters) {
		if ( start == null || executionOffset == null ) {
			return null;
		}
		final String offset = (placeholderService != null
				? placeholderService.resolvePlaceholders(executionOffset, parameters)
				: executionOffset);
		try {
			long ms = Long.parseLong(offset);
			return start.plusMillis(ms);
		} catch ( NumberFormatException e ) {
			// ignore
		}
		try {
			Period p = Period.parse(offset);
			return start.plus(p);
		} catch ( DateTimeParseException e ) {
			// ignore
		}
		try {
			Duration d = Duration.parse(offset);
			return start.plus(d);
		} catch ( DateTimeParseException e ) {
			// ignore
		}
		return null;
	}

	/**
	 * Get the appropriate {@link Expression} to use for the {@link #getValue()}
	 * configuration, if {@link #getExpressionServiceId()} is configured and the
	 * matching service is available.
	 * 
	 * @param services
	 *        the available services
	 * @return the expression instance, or {@literal null} if no expression
	 *         configured or the appropriate service is not found
	 */
	public synchronized ExpressionServiceExpression valueExpression(
			Iterable<ExpressionService> services) {
		final String serviceId = getExpressionServiceId();
		if ( serviceId == null ) {
			return null;
		}
		for ( ExpressionService service : services ) {
			if ( service != null && service.getUid().equalsIgnoreCase(serviceId) ) {
				Expression expr = cachedExpression;
				if ( expr == null ) {
					expr = service.parseExpression(getValue());
					if ( expr != null ) {
						cachedExpression = expr;
					}
				}
				if ( expr != null ) {
					return new ExpressionServiceExpression(service, expr);
				}
			}
		}
		return null;
	}

	/**
	 * Get the control ID.
	 * 
	 * @return the control ID
	 */
	public String getControlId() {
		return controlId;
	}

	/**
	 * Set the control ID.
	 * 
	 * @param controlId
	 *        the control ID to set
	 */
	public void setControlId(String controlId) {
		this.controlId = controlId;
	}

	/**
	 * Get the execution time offset.
	 * 
	 * @return the time offset
	 */
	public String getExecutionOffset() {
		return executionOffset;
	}

	/**
	 * Set the execution time offset.
	 * 
	 * @param executionOffset
	 *        the offset to set; can be an ISO 8601 period (for example
	 *        {@literal PT1H} or an integer millisecond value, both supporting a
	 *        {@code -} prefix for negation
	 * @see Period#parse(CharSequence)
	 */
	public void setExecutionOffset(String executionOffset) {
		this.executionOffset = executionOffset;
	}

	/**
	 * Get the desired control value.
	 * 
	 * @return the value; if {@link #getExpressionServiceId()} is configured
	 *         this value is assumed to be an expression
	 */
	public String getValue() {
		return value;
	}

	/**
	 * Set the desired control value.
	 * 
	 * @param value
	 *        the value to set; if {@link #getExpressionServiceId()} is
	 *        configured this value is assumed to be an expression
	 */
	public void setValue(String value) {
		this.value = value;
		this.cachedExpression = null;
	}

	/**
	 * Get the {@link ExpressionService} ID to use when evaluating
	 * {@link #getValue()}.
	 * 
	 * @return the service ID
	 */
	public String getExpressionServiceId() {
		return expressionServiceId;
	}

	/**
	 * Set the {@link ExpressionService} ID to use when evaluating
	 * {@link #getValue()}.
	 * 
	 * @param expressionServiceId
	 *        the service ID, or {@literal null} to not evaluate
	 */
	public synchronized void setExpressionServiceId(String expressionServiceId) {
		this.expressionServiceId = expressionServiceId;
		this.cachedExpression = null;
	}

}
