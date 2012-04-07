/* ===================================================================
 * WeatherDotComSupport.java
 * 
 * Created Dec 3, 2009 11:41:52 AM
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
 * $Id$
 * ===================================================================
 */

package net.solarnetwork.node.weather.impl;

import java.beans.PropertyEditor;
import java.beans.PropertyEditorSupport;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.text.MessageFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Map;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.dom.DOMSource;
import javax.xml.xpath.XPathExpression;

import net.solarnetwork.node.support.XmlServiceSupport;

import org.w3c.dom.Document;
import org.xml.sax.SAXException;

/**
 * Base class to support Weather.com DatumDataSource implementations.
 * 
 * <p>The configurable properties of this class are:</p>
 * 
 * <dl class="class-properties">
 *   <dt>location</dt>
 *   <dd>The Weather.com location ID to query.</dd>
 *   
 *   <dt>partnerId</dt>
 *   <dd>The Weather.com partner ID to authenticate with.</dd>
 *   
 *   <dt>licenseKey</dt>
 *   <dd>The Weather.com license key to authenticate with.</dd>
 *   
 *   <dt>baseUrl</dt>
 *   <dd>A URL template to use to query the Weather.com XML data service.
 *   This is a {@link MessageFormat} template that will be passed the 
 *   {@code location}, {@code partnerId}, and {@code licenseKey}
 *   as parameters (in that order). The default value is probably
 *   suitable for most cases.</dd>
 *   
 *   <dt>datumXPathMapping</dt>
 *   <dd>A mapping of Datum JavaBean property names to associated XPath
 *   expressions for extracting the relevant value from the Weather.com
 *   XML response.</dd>
 *   
 *   <dt>infoDateFormat</dt>
 *   <dd>An array of date formats that will be used to parse dates
 *   returned by the {@link PropertyEditor} in 
 *   {@link #newStandardDateEditor()}. The default value is probably
 *   suitable for most cases.</dd>
 *   
 *   <dt>
 * </dl>
 *
 * @author matt
 * @version $Revision$ $Date$
 */
public abstract class WeatherDotComSupport extends XmlServiceSupport {

	private String location = null;
	private String partnerId = null;
	private String licenseKey = null;
	private String baseUrl = "http://xoap.weather.com/weather/local/{0}?cc=*&dayf=5&link=xoap&prod=xoap&par={1}&key={2}&unit=m";
	private Map<String, String> datumXPathMapping = null;
	private String[] infoDateFormat = new String[] {"M/d/yy h:mm a 'Local Time'", "M/d/yy h:mm a Z"};
	
	private MessageFormat urlFormat;
	private Map<String, XPathExpression> datumXPathMap = null;
	
	/**
	 * Initialize this class after properties are set.
	 */
	@Override
	public void init() {
		super.init();
		
		this.urlFormat = new MessageFormat(this.baseUrl);
		this.datumXPathMap = getXPathExpressionMap(datumXPathMapping);
	}
	
	/**
	 * Get a PropertyEditor that is capable of parsing the date format
	 * Weather.com uses in their date fields.
	 * 
	 * @return new PropertyEditor instance
	 */
	protected PropertyEditor newStandardDateEditor() {
		PropertyEditor infoDateEditor = new PropertyEditorSupport() {
			@Override
			public String getAsText() {
				return getValue() == null ? "" : getValue().toString();
			}

			@Override
			public void setAsText(String text) throws IllegalArgumentException {
				for ( String format : infoDateFormat ) {
					try {
						SimpleDateFormat sdf = new SimpleDateFormat(format);
						setValue(sdf.parse(text));
						break;
					} catch ( ParseException e ) {
						if ( log.isTraceEnabled() ) {
							log.trace("Unable to parse date [" +text +"] with format ["
									+format +']');
						}
					}
				}
			}
		};
		return infoDateEditor;
	}

	/**
	 * Populate JavaBean properties on an object by extracting values 
	 * from the Weather.com XML response.
	 * 
	 * <p>This method will make a request to the Weather.com URL and
	 * parse the response, populating JavaBean properties according
	 * to the {@link #getDatumXPathMapping()} mapping.</p>
	 * 
	 * @param theLocation the Weather.com location to use
	 * @param datum the bean to populate values on
	 */
	protected void populateDatum(String theLocation, Object datum) {
		Document doc;
		try {
			String url = this.urlFormat.format(new Object[] {
					URLEncoder.encode(theLocation, "UTF-8"),
					URLEncoder.encode(this.partnerId, "UTF-8"),
					URLEncoder.encode(this.licenseKey, "UTF-8"),
			});
			if ( log.isDebugEnabled() ) {
				log.debug("Calling weather url [" +url +']');
			}
			doc = getDocBuilderFactory().newDocumentBuilder().parse(url);
		} catch ( UnsupportedEncodingException e ) {
			throw new RuntimeException(e);
		} catch ( IOException e ) {
			throw new RuntimeException(e);
		} catch (SAXException e) {
			throw new RuntimeException(e);
		} catch (ParserConfigurationException e) {
			throw new RuntimeException(e);
		}

		if ( log.isTraceEnabled() ) {
			log.trace("Got weather XML: " +getXmlAsString(
					new DOMSource(doc), true));
		}
		
		extractBeanDataFromXml(datum, doc.getDocumentElement(), this.datumXPathMap);
	}

	/**
	 * @return the location
	 */
	public String getLocation() {
		return location;
	}

	/**
	 * @param location the location to set
	 */
	public void setLocation(String location) {
		this.location = location;
	}

	/**
	 * @return the partnerId
	 */
	public String getPartnerId() {
		return partnerId;
	}

	/**
	 * @param partnerId the partnerId to set
	 */
	public void setPartnerId(String partnerId) {
		this.partnerId = partnerId;
	}

	/**
	 * @return the licenseKey
	 */
	public String getLicenseKey() {
		return licenseKey;
	}

	/**
	 * @param licenseKey the licenseKey to set
	 */
	public void setLicenseKey(String licenseKey) {
		this.licenseKey = licenseKey;
	}

	/**
	 * @return the baseUrl
	 */
	public String getBaseUrl() {
		return baseUrl;
	}

	/**
	 * @param baseUrl the baseUrl to set
	 */
	public void setBaseUrl(String baseUrl) {
		this.baseUrl = baseUrl;
	}
		
	/**
	 * @return the infoDateFormat
	 */
	public String[] getInfoDateFormat() {
		return infoDateFormat;
	}
	
	/**
	 * @param infoDateFormat the infoDateFormat to set
	 */
	public void setInfoDateFormat(String[] infoDateFormat) {
		this.infoDateFormat = infoDateFormat;
	}
	
	/**
	 * @return the urlFormat
	 */
	public MessageFormat getUrlFormat() {
		return urlFormat;
	}
	
	/**
	 * @param urlFormat the urlFormat to set
	 */
	public void setUrlFormat(MessageFormat urlFormat) {
		this.urlFormat = urlFormat;
	}

	/**
	 * @return the datumXPathMapping
	 */
	public Map<String, String> getDatumXPathMapping() {
		return datumXPathMapping;
	}

	/**
	 * @param datumXPathMapping the datumXPathMapping to set
	 */
	public void setDatumXPathMapping(Map<String, String> datumXPathMapping) {
		this.datumXPathMapping = datumXPathMapping;
	}

}
