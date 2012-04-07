/* ===================================================================
 * JavaBeanWebPostUploadService.java
 * 
 * Created Aug 31, 2008 8:03:22 PM
 * 
 * Copyright (c) 2008 Solarnetwork.net Dev Team.
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
 * $Id$
 * ===================================================================
 */

package net.solarnetwork.node.upload;

import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;

import net.solarnetwork.node.Datum;
import net.solarnetwork.node.UploadService;
import net.solarnetwork.node.support.XmlServiceSupport;

import org.springframework.beans.BeanWrapper;
import org.springframework.beans.PropertyAccessorFactory;
import org.springframework.beans.propertyeditors.CustomDateEditor;

/**
 * Implementation of {@link UploadService} that posts HTTP parameters using
 * JavaBean parameter semantics.
 * 
 * <p>This service will turn {@link Datum} objects into HTTP POST requests, with
 * the JavaBean properties of the Datum turned into HTTP parameters. The response
 * is expected to be a valid XML document with a consistent structure across all
 * Datum types so that a single XPath can be used to extract the remote primary
 * key (tracking ID) value.</p>
 * 
 * <p>The configurable properties of this class are:</p>
 * 
 * <dl class="class-properties">
 *   <dt>datumIdXPath</dt>
 *   <dd>An XPath to use for extracting the remote primary key (tracking ID)
 *   from the web post XML response. This single XPath is used across all
 *   Datum types, so it must be generic enough to handle all types. Defaults
 *   to {@link #DEFAULT_ID_XPATH}.</dd>
 *   
 *   <dt>datumClassNameUrlMapping</dt>
 *   <dd>A mapping of {@link Datum} class names to corresponding String URLs
 *   that those datums should be posted to. If a Datum passed to 
 *   {@link #uploadDatum(Datum)} does not have a key in this map, an
 *   {@code IllegalArgumentException} will be thrown by that method.</dd>
 * </dl>
 *
 * @author matt.magoffin
 * @version $Revision$ $Date$
 */
public class JavaBeanWebPostUploadService extends XmlServiceSupport
implements UploadService {

	/** The date format to use for formatting dates with time. */
	public static final String DATE_TIME_FORMAT = "yyyy-MM-dd HH:mm:ss.SSSZ";

	/** The date format to use for DayDatum dates. */
	public static final String DAY_DATE_FORMAT = "yyyy-MM-dd";

	/** The date format to use for DayDatum times. */
	public static final String DAY_TIME_FORMAT = "HH:mm";

	/** The default value for the {@code datumIdXPath} property. */
	public static final String DEFAULT_ID_XPATH = "/*/@id";

	private Map<String, String> datumClassNameUrlMapping = Collections.emptyMap();
	private String datumIdXPath = DEFAULT_ID_XPATH;
	
	private XPathExpression datumTrackingIdXPath = null;
	
	/**
	 * Initialize this class after properties are set.
	 */
	@Override
	public void init() {
		super.init();
		try {
			XPath xp = getXpathFactory().newXPath();
			if ( getNsContext() != null ) {
				xp.setNamespaceContext(getNsContext());
			}
			this.datumTrackingIdXPath = xp.compile(this.datumIdXPath);
		} catch (XPathExpressionException e) {
			throw new RuntimeException(e);
		}
	}
	
	/* (non-Javadoc)
	 * @see net.solarnetwork.node.UploadService#getKey()
	 */
	public String getKey() {
		return "JavaBeanWebPostUploadService:" +getIdentityService().getSolarNetHostName();
	}

	/* (non-Javadoc)
	 * @see net.solarnetwork.node.UploadService#uploadDatum(net.solarnetwork.node.Datum)
	 */
	public Long uploadDatum(Datum data) {
		String url = datumClassNameUrlMapping.get(data.getClass().getName());
		if ( url == null ) {
			throw new IllegalArgumentException("Datum type [" 
					+data.getClass().getName()
					+"] is not supported");
		}
		BeanWrapper bean = getDefaultBeanWrapper(data);
		Map<String, Object> attributes = new LinkedHashMap<String, Object>(1);
		attributes.put(ATTR_NODE_ID, getIdentityService().getNodeId());
		String postUrl = getIdentityService().getSolarInBaseUrl() +url;
		return webFormPostForTrackingId(bean, postUrl, datumTrackingIdXPath, 
				datumIdXPath, attributes);
	}

	private BeanWrapper getDefaultBeanWrapper(Object data) {
		BeanWrapper bean = PropertyAccessorFactory.forBeanPropertyAccess(data);
		SimpleDateFormat dateFormat = new SimpleDateFormat(DATE_TIME_FORMAT);
		dateFormat.setLenient(false);
		bean.registerCustomEditor(Date.class, new CustomDateEditor(dateFormat, false));

		// TODO need a way to configure these things instead of hard-coding them here
		SimpleDateFormat timeFormat = new SimpleDateFormat(DAY_TIME_FORMAT);
		dateFormat.setLenient(false);
		bean.registerCustomEditor(Date.class, "sunrise", 
				new CustomDateEditor(timeFormat, false));
		bean.registerCustomEditor(Date.class, "sunset", 
				new CustomDateEditor(timeFormat, false));

		SimpleDateFormat dayFormat = new SimpleDateFormat(DAY_DATE_FORMAT);
		dateFormat.setLenient(false);
		bean.registerCustomEditor(Date.class, "day", 
				new CustomDateEditor(dayFormat, false));
		return bean;
	}

	/**
	 * @return the datumClassNameUrlMapping
	 */
	public Map<String, String> getDatumClassNameUrlMapping() {
		return datumClassNameUrlMapping;
	}

	/**
	 * @param datumClassNameUrlMapping the datumClassNameUrlMapping to set
	 */
	public void setDatumClassNameUrlMapping(
			Map<String, String> datumClassNameUrlMapping) {
		this.datumClassNameUrlMapping = datumClassNameUrlMapping;
	}

	/**
	 * @return the datumIdXPath
	 */
	public String getDatumIdXPath() {
		return datumIdXPath;
	}

	/**
	 * @param datumIdXPath the datumIdXPath to set
	 */
	public void setDatumIdXPath(String datumIdXPath) {
		this.datumIdXPath = datumIdXPath;
	}

}
