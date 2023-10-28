/* ==================================================================
 * ExceptionUtils.java - 11/08/2022 3:14:52 pm
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

package net.solarnetwork.node.setup.web.support;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import javax.validation.Validator;
import org.springframework.context.MessageSource;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindingResult;
import org.springframework.validation.Errors;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.validation.beanvalidation.SpringValidatorAdapter;
import net.solarnetwork.domain.Result;

/**
 * Helpers for dealing with exceptions.
 * 
 * @author matt
 * @version 1.0
 * @since 3.3
 */
public final class ExceptionUtils {

	private ExceptionUtils() {
		// not available
	}

	/**
	 * Generate an error message from an {@link Errors} instance.
	 * 
	 * @param e
	 *        the errors
	 * @param locale
	 *        the locale
	 * @param msgSrc
	 *        the message source
	 * @return the error message
	 */
	public static String generateErrorsMessage(Errors e, Locale locale, MessageSource msgSrc) {
		String msg = (msgSrc == null ? "Validation error"
				: msgSrc.getMessage("error.validation", null, "Validation error", locale));
		if ( msgSrc != null && e != null && e.hasErrors() ) {
			StringBuilder buf = new StringBuilder();
			for ( ObjectError error : e.getAllErrors() ) {
				if ( buf.length() > 0 ) {
					buf.append(" ");
				}
				buf.append(msgSrc.getMessage(error, locale));
			}
			msg = buf.toString();
		}
		return msg;
	}

	/**
	 * Generate an error result from an {@link Errors} instance.
	 * 
	 * <p>
	 * The {@code message} will be generated via
	 * {@link #generateErrorsMessage(Errors, Locale, MessageSource)}.
	 * </p>
	 * 
	 * @param <V>
	 *        the result type
	 * @param e
	 *        the errors
	 * @param code
	 *        the optional error code
	 * @param locale
	 *        the locale
	 * @param msgSrc
	 *        the message source
	 * @return the result
	 */
	public static <V> Result<V> generateErrorsResult(Errors e, String code, Locale locale,
			MessageSource msgSrc) {
		return generateErrorsResult(e, code, generateErrorsMessage(e, locale, msgSrc), locale, msgSrc);
	}

	/**
	 * Generate an error result from an {@link Errors} instance.
	 * 
	 * <p>
	 * Special handling of {@link ConstraintViolation} is performed to extract
	 * the error path, and rejected value.
	 * </p>
	 * 
	 * @param <V>
	 *        the result type
	 * @param e
	 *        the errors
	 * @param code
	 *        the optional error code
	 * @param message
	 *        the optional error message
	 * @param locale
	 *        the locale
	 * @param msgSrc
	 *        the message source
	 * @return the result
	 */
	public static <V> Result<V> generateErrorsResult(Errors e, String code, String message,
			Locale locale, MessageSource msgSrc) {
		List<Result.ErrorDetail> details = null;
		if ( msgSrc != null && e != null && e.hasErrors() ) {
			for ( ObjectError error : e.getGlobalErrors() ) {
				if ( details == null ) {
					details = new ArrayList<>(4);
				}
				details.add(new Result.ErrorDetail(error.getObjectName(), null,
						msgSrc.getMessage(error, locale)));
			}
			for ( FieldError error : e.getFieldErrors() ) {
				if ( details == null ) {
					details = new ArrayList<>(4);
				}
				ConstraintViolation<?> violation = null;
				if ( error.contains(ConstraintViolation.class) ) {
					violation = error.unwrap(ConstraintViolation.class);
				}
				String location = (violation != null ? violation.getPropertyPath().toString()
						: String.format("%s.%s", error.getObjectName(), error.getField()));
				String violationCode = (violation != null
						? violation.getConstraintDescriptor().getAnnotation().annotationType()
								.getSimpleName()
						: null);
				String rejectedValueDescription = (error.getRejectedValue() != null
						? rejectedValueDescription = error.getRejectedValue().toString()
						: null);
				details.add(new Result.ErrorDetail(location, violationCode, rejectedValueDescription,
						msgSrc.getMessage(error, locale)));
			}
		}
		return Result.error(code, message, details);
	}

	/**
	 * Convert a constraint violation exception into a binding result.
	 * 
	 * @param e
	 *        the exception
	 * @param validator
	 *        the validator
	 * @return the result
	 */
	public static BindingResult toBindingResult(ConstraintViolationException e, Validator validator) {
		Object object = null;
		for ( ConstraintViolation<?> violation : e.getConstraintViolations() ) {
			if ( violation.getLeafBean() != null ) {
				object = violation.getLeafBean();
			} else {
				object = violation.getRootBean();
			}
			break;
		}
		try {
			BindingResult bindingResult = new BeanPropertyBindingResult(object, "input");
			new SpringValidatorAdapterSupport(validator, e, bindingResult);
			return bindingResult;
		} catch ( IllegalStateException e2 ) {
			// try with root bean instead of leaf
			for ( ConstraintViolation<?> violation : e.getConstraintViolations() ) {
				object = violation.getRootBean();
				break;
			}
			BindingResult bindingResult = new BeanPropertyBindingResult(object, "input");
			try {
				new SpringValidatorAdapterSupport(validator, e, bindingResult);
				return bindingResult;
			} catch ( IllegalStateException e3 ) {
				// fall back to generic message
				bindingResult.addError(new ObjectError("input", "Input is invalid."));
				return bindingResult;
			}
		}
	}

	private static class SpringValidatorAdapterSupport extends SpringValidatorAdapter {

		@SuppressWarnings({ "rawtypes", "unchecked" })
		private SpringValidatorAdapterSupport(Validator validator,
				ConstraintViolationException exception, BindingResult errors) {
			super(validator);
			processConstraintViolations((Set) exception.getConstraintViolations(), errors);
		}
	}

}
