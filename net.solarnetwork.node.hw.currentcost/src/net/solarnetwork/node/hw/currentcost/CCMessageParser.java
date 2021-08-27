/* ==================================================================
 * CCMessageParser.java - Apr 26, 2013 8:57:13 AM
 * 
 * Copyright 2007-2013 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.hw.currentcost;

import java.io.ByteArrayInputStream;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;
import javax.xml.xpath.XPathExpression;
import org.joda.time.LocalTime;
import org.springframework.beans.BeanWrapper;
import net.solarnetwork.node.support.XmlServiceSupport;
import net.solarnetwork.util.JodaDateFormatEditor;
import net.solarnetwork.util.JodaDateFormatEditor.ParseMode;

/**
 * Helper class that can parse Current Cost XML messages into {@link CCDatum}
 * instances.
 * 
 * <p>
 * This parser supports both <b>Classic</b> and <b>CC128</b> message formats.
 * </p>
 * 
 * @author matt
 * @version 1.2
 */
public class CCMessageParser extends XmlServiceSupport {

	private static JodaDateFormatEditor LOCAL_TIME_EDITOR = new JodaDateFormatEditor("HH:mm:ss",
			ParseMode.LocalTime);

	private final Map<String, XPathExpression> xpathMapping;

	public CCMessageParser() {
		super();
		init();
		Map<String, String> mapping = new HashMap<String, String>();
		mapping.put("deviceAddress", "(/msg/src/id|/msg/id)[1]");
		mapping.put("deviceName", "(/msg/src/name|/msg/src[count(child::*) = 0])[1]");
		mapping.put("deviceType", "(/msg/src/type|/msg/type[count(child::*) = 0])[1]");
		mapping.put("deviceSoftwareVersion", "/msg/src/sver");
		mapping.put("daysSinceBegin", "(/msg/date/dsb|/msg/dsb)[1]");
		mapping.put("localTime",
				"concat(/msg/date/hr, ':', /msg/date/min, ':', /msg/date/sec, /msg/time)");
		mapping.put("channel1Watts", "/msg/ch1/watts");
		mapping.put("channel2Watts", "/msg/ch2/watts");
		mapping.put("channel3Watts", "/msg/ch3/watts");
		mapping.put("temperature", "/msg/tmpr");
		xpathMapping = getXPathExpressionMap(mapping);
	}

	/**
	 * Parse a CurrentCost XML message into a CCDatum object.
	 * 
	 * @param messageXML
	 *        the message bytes to parse
	 * @return the CCDatum instance, or <em>null</em> if any parsing error
	 *         occurs
	 */
	public CCDatum parseMessage(byte[] messageXML) {
		CCDatum d = new CCDatum();
		if ( log.isDebugEnabled() ) {
			try {
				log.debug("Parsing CC XML: {}", new String(messageXML, "UTF-8"));
			} catch ( UnsupportedEncodingException e ) {
				// shouldn't get here
			}
		}
		try {
			extractBeanDataFromXml(d, getDocBuilderFactory().newDocumentBuilder()
					.parse(new ByteArrayInputStream(messageXML)), xpathMapping);
		} catch ( Exception e ) {
			try {
				log.debug("XML parsing exception: {}; message: {}", e.getMessage(),
						new String(messageXML, "US-ASCII"));
			} catch ( UnsupportedEncodingException e1 ) {
				log.debug("XML parsing exception: {}", e.getMessage());
			}
			return null;
		}
		return d;
	}

	@Override
	public void registerCustomEditors(BeanWrapper bean) {
		bean.registerCustomEditor(LocalTime.class, LOCAL_TIME_EDITOR);
	}

}
