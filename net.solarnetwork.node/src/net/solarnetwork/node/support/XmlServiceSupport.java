/* ===================================================================
 * XmlServiceSupport.java
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

package net.solarnetwork.node.support;

import java.beans.PropertyDescriptor;
import java.beans.PropertyEditor;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TimeZone;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import javax.xml.namespace.NamespaceContext;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Source;
import javax.xml.transform.Templates;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import net.solarnetwork.domain.GeneralDatumMetadata;
import net.solarnetwork.node.DatumDataSource;
import net.solarnetwork.node.DatumMetadataService;
import net.solarnetwork.node.IdentityService;
import net.solarnetwork.node.domain.Datum;
import net.solarnetwork.node.util.ClassUtils;
import net.solarnetwork.util.OptionalService;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.PropertyAccessorFactory;
import org.springframework.core.io.Resource;
import org.springframework.util.FileCopyUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 * An abstract class to support services that use XML.
 * 
 * <p>
 * The configurable properties of this class are:
 * </p>
 * 
 * <dl class="class-properties">
 * <dt>docBuilderFactory</dt>
 * <dd>A JAXP {@link DocumentBuilderFactory} to use. If not configured, the
 * {@link DocumentBuilderFactory#newInstance()} method will be used to create a
 * default one.</p>
 * 
 * <dt>transformerFactory</dt>
 * <dd>A JAXP {@link TransformerFactory} for handling XSLT transformations with.
 * If not configured, the {@link TransformerFactory#newInstance()} method will
 * be used to create a default one.</p>
 * 
 * <dt>xpathFactory</dt>
 * <dd>A JAXP {@link XPathFactory} for handling XPath operations with. If not
 * configured the {@link XPathFactory#newInstance()} method will be used to
 * create a default one.</dd>
 * 
 * <dt>nsContext</dt>
 * <dd>An optional {@link NamespaceContext} to use for proper XML namespace
 * handling in some contexts, such as XPath.</dd>
 * 
 * <dt>identityService</dt>
 * <dd>The {@link IdentityService} for identifying node details.</dd>
 * 
 * <dt>eventAdmin</dt>
 * <dd>An optional {@link EventAdmin} service to use for posting events.</dd>
 * 
 * <dt>datumMetadataService</dt>
 * <dd>An optional {@link DatumMetadataService} to use for managing metadata.</dd>
 * </dl>
 * 
 * @author matt.magoffin
 * @version 1.2
 */
public abstract class XmlServiceSupport extends HttpClientSupport {

	/** Special attribute key for a node ID value. */
	public static final String ATTR_NODE_ID = "node-id";

	private NamespaceContext nsContext = null;
	private DocumentBuilderFactory docBuilderFactory = null;
	private XPathFactory xpathFactory = null;
	private TransformerFactory transformerFactory = null;
	private OptionalService<EventAdmin> eventAdmin;
	private OptionalService<DatumMetadataService> datumMetadataService;

	private final ConcurrentMap<String, GeneralDatumMetadata> sourceMetadataCache = new ConcurrentHashMap<String, GeneralDatumMetadata>(
			4);

	/**
	 * Initialize this class after properties are set.
	 */
	public void init() {
		if ( this.docBuilderFactory == null ) {
			this.docBuilderFactory = DocumentBuilderFactory.newInstance();
			this.docBuilderFactory.setNamespaceAware(true);
		}
		if ( this.xpathFactory == null ) {
			this.xpathFactory = XPathFactory.newInstance();
		}
		if ( this.transformerFactory == null ) {
			this.transformerFactory = TransformerFactory.newInstance();
		}
	}

	/**
	 * Compile XPathExpression mappings from String XPath expressions.
	 * 
	 * @param xpathMap
	 *        the XPath string expressions
	 * @return the XPathExperssion mapping
	 */
	protected Map<String, XPathExpression> getXPathExpressionMap(Map<String, String> xpathMap) {
		Map<String, XPathExpression> datumXPathMap = new LinkedHashMap<String, XPathExpression>();
		for ( Map.Entry<String, String> me : xpathMap.entrySet() ) {
			try {
				XPath xp = getXpathFactory().newXPath();
				if ( getNsContext() != null ) {
					xp.setNamespaceContext(getNsContext());
				}
				datumXPathMap.put(me.getKey(), xp.compile(me.getValue()));
			} catch ( XPathExpressionException e ) {
				throw new RuntimeException(e);
			}
		}
		return datumXPathMap;
	}

	/**
	 * Get an XSLT Templates object from an XSLT Resource.
	 * 
	 * @param resource
	 *        the XSLT Resource to load
	 * @return the compiled Templates
	 */
	protected Templates getTemplates(Resource resource) {
		TransformerFactory tf = TransformerFactory.newInstance();
		try {
			return tf.newTemplates(new StreamSource(resource.getInputStream()));
		} catch ( TransformerConfigurationException e ) {
			throw new RuntimeException("Unable to load XSLT from resource [" + resource + ']');
		} catch ( IOException e ) {
			throw new RuntimeException("Unable to load XSLT from resource [" + resource + ']');
		}
	}

	/**
	 * Turn an object into a simple XML Document.
	 * 
	 * <p>
	 * The returned XML will be a single element with all JavaBean properties
	 * turned into attributed. For example:
	 * <p>
	 * 
	 * <pre>
	 * &lt;powerDatum
	 *   id="123"
	 *   pvVolts="123.123"
	 *   ... /&gt;
	 * </pre>
	 * 
	 * @param o
	 *        the object to turn into XML
	 * @param elementName
	 *        the name of the XML element
	 * @return the element, as XSLT Source
	 * @see #getSimpleDocument(Object, String)
	 */
	protected Source getSimpleSource(Object o, String elementName) {
		Document dom = getSimpleDocument(o, elementName);
		return getSource(dom);
	}

	/**
	 * Turn an object into a simple XML Document.
	 * 
	 * <p>
	 * The returned XML will be a single element with all JavaBean properties
	 * turned into attributes. For example:
	 * <p>
	 * 
	 * <pre>
	 * &lt;powerDatum
	 *   id="123"
	 *   pvVolts="123.123"
	 *   ... /&gt;
	 * </pre>
	 * 
	 * @param o
	 *        the object to turn into XML
	 * @param elementName
	 *        the name of the XML element
	 * @return the element, as an XML DOM Document
	 */
	protected Document getSimpleDocument(Object o, String elementName) {
		Document dom = null;
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
		sdf.setTimeZone(TimeZone.getTimeZone("GMT"));
		try {
			dom = getDocBuilderFactory().newDocumentBuilder().newDocument();
			Element root = dom.createElement(elementName);
			dom.appendChild(root);
			Map<String, Object> props = ClassUtils.getBeanProperties(o, null);
			for ( Map.Entry<String, Object> me : props.entrySet() ) {
				Object val = me.getValue();
				if ( val instanceof Date ) {
					val = sdf.format((Date) val);
				}
				root.setAttribute(me.getKey(), val.toString());

				if ( log.isTraceEnabled() ) {
					log.trace("attribute name: " + me.getKey() + " attribute value: " + val.toString());
				}

			}
		} catch ( ParserConfigurationException e ) {
			throw new RuntimeException(e);
		}
		return dom;
	}

	/**
	 * Turn an object into a simple XML Document, supporting custom property
	 * editors.
	 * 
	 * <p>
	 * The returned XML will be a document with a single element with all
	 * JavaBean properties turned into attributes. For example:
	 * <p>
	 * 
	 * <pre>
	 * &lt;powerDatum
	 *   id="123"
	 *   pvVolts="123.123"
	 *   ... /&gt;
	 * </pre>
	 * 
	 * <p>
	 * {@link PropertyEditor} instances can be registered with the supplied
	 * {@link BeanWrapper} for custom handling of properties, e.g. dates.
	 * </p>
	 * 
	 * @param bean
	 *        the object to turn into XML
	 * @param elementName
	 *        the name of the XML element
	 * @return the element, as an XML DOM Document
	 */
	protected Document getDocument(BeanWrapper bean, String elementName) {
		Document dom = null;
		try {
			dom = getDocBuilderFactory().newDocumentBuilder().newDocument();
			dom.appendChild(getElement(bean, elementName, dom));
		} catch ( ParserConfigurationException e ) {
			throw new RuntimeException(e);
		}
		return dom;
	}

	/**
	 * Turn an object into a simple XML Element, supporting custom property
	 * editors.
	 * 
	 * <p>
	 * The returned XML will be a single element with all JavaBean properties
	 * turned into attributes and the element named after the bean object's
	 * class name. For example:
	 * <p>
	 * 
	 * <pre>
	 * &lt;PowerDatum
	 *   id="123"
	 *   pvVolts="123.123"
	 *   ... /&gt;
	 * </pre>
	 * 
	 * <p>
	 * {@link PropertyEditor} instances can be registered with the supplied
	 * {@link BeanWrapper} for custom handling of properties, e.g. dates.
	 * </p>
	 * 
	 * @param bean
	 *        the object to turn into XML
	 * @return the element, as an XML DOM Document
	 */
	protected Element getElement(BeanWrapper bean, Document dom) {
		String elementName = bean.getWrappedInstance().getClass().getSimpleName();
		return getElement(bean, elementName, dom);
	}

	/**
	 * Turn an object into a simple XML Element, supporting custom property
	 * editors.
	 * 
	 * <p>
	 * The returned XML will be a single element with all JavaBean properties
	 * turned into attributes. For example:
	 * <p>
	 * 
	 * <pre>
	 * &lt;powerDatum
	 *   id="123"
	 *   pvVolts="123.123"
	 *   ... /&gt;
	 * </pre>
	 * 
	 * <p>
	 * {@link PropertyEditor} instances can be registered with the supplied
	 * {@link BeanWrapper} for custom handling of properties, e.g. dates.
	 * </p>
	 * 
	 * @param bean
	 *        the object to turn into XML
	 * @param elementName
	 *        the name of the XML element
	 * @return the element, as an XML DOM Element
	 */
	protected Element getElement(BeanWrapper bean, String elementName, Document dom) {
		PropertyDescriptor[] props = bean.getPropertyDescriptors();
		Element root = null;
		root = dom.createElement(elementName);
		for ( int i = 0; i < props.length; i++ ) {
			PropertyDescriptor prop = props[i];
			if ( prop.getReadMethod() == null ) {
				continue;
			}
			String propName = prop.getName();
			if ( "class".equals(propName) ) {
				continue;
			}
			Object propValue = null;
			PropertyEditor editor = bean.findCustomEditor(prop.getPropertyType(), prop.getName());
			if ( editor != null ) {
				editor.setValue(bean.getPropertyValue(propName));
				propValue = editor.getAsText();
			} else {
				propValue = bean.getPropertyValue(propName);
			}
			if ( propValue == null ) {
				continue;
			}
			if ( log.isTraceEnabled() ) {
				log.trace("attribute name: " + propName + " attribute value: " + propValue);
			}
			root.setAttribute(propName, propValue.toString());
		}
		return root;
	}

	/**
	 * Turn an object into a simple XML Document, supporting custom property
	 * editors.
	 * 
	 * <p>
	 * The returned XML will be a single element with all JavaBean properties
	 * turned into attributed. For example:
	 * <p>
	 * 
	 * <pre>
	 * &lt;powerDatum
	 *   id="123"
	 *   pvVolts="123.123"
	 *   ... /&gt;
	 * </pre>
	 * 
	 * @param bean
	 *        the object to turn into XML
	 * @param elementName
	 *        the name of the XML element
	 * @return the element, as XSLT Source
	 * @see #getDocument(BeanWrapper, String)
	 */
	protected Source getSource(BeanWrapper bean, String elementName) {
		Document dom = getDocument(bean, elementName);
		return getSource(dom);
	}

	/**
	 * Turn a Document into a Source.
	 * 
	 * <p>
	 * This method will log the XML document at the FINEST level.
	 * </p>
	 * 
	 * @param dom
	 *        the Document to turn into XSLT source
	 * @return the document, as XSLT Source
	 */
	protected Source getSource(Document dom) {
		DOMSource result = new DOMSource(dom);
		if ( log.isDebugEnabled() ) {
			log.debug("XML: " + getXmlAsString(result, true));
		}
		return result;
	}

	/**
	 * Turn an XML Source into a String.
	 * 
	 * @param source
	 *        the XML Source
	 * @param indent
	 *        if <em>true</em> then indent the result
	 * @return the XML, as a String
	 */
	protected String getXmlAsString(Source source, boolean indent) {
		ByteArrayOutputStream byos = new ByteArrayOutputStream();
		try {
			Transformer xform = getTransformerFactory().newTransformer();
			if ( indent ) {
				xform.setOutputProperty(OutputKeys.INDENT, "yes");
				xform.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
			}
			xform.transform(source, new StreamResult(byos));
		} catch ( TransformerConfigurationException e ) {
			throw new RuntimeException(e);
		} catch ( TransformerException e ) {
			throw new RuntimeException(e);
		}
		return byos.toString();
	}

	/**
	 * Populate JavaBean properties via XPath extraction.
	 * 
	 * <p>
	 * This method will call {@link #registerCustomEditors(BeanWrapper)} so
	 * custom editors can be registered if desired.
	 * </p>
	 * 
	 * @param obj
	 *        the object to set properties on, or a BeanWrapper
	 * @param xml
	 *        the XML
	 * @param xpathMap
	 *        the mapping of JavaBean property names to XPaths
	 */
	protected void extractBeanDataFromXml(Object obj, Node xml, Map<String, XPathExpression> xpathMap) {
		BeanWrapper bean;
		if ( obj instanceof BeanWrapper ) {
			bean = (BeanWrapper) obj;
		} else {
			bean = PropertyAccessorFactory.forBeanPropertyAccess(obj);
		}
		registerCustomEditors(bean);
		for ( Map.Entry<String, XPathExpression> me : xpathMap.entrySet() ) {
			try {
				String val = (String) me.getValue().evaluate(xml, XPathConstants.STRING);
				if ( val != null && !"".equals(val) ) {
					bean.setPropertyValue(me.getKey(), val);
				}
			} catch ( XPathExpressionException e ) {
				throw new RuntimeException(e);
			}
		}
	}

	/**
	 * Extending classes can override this method to register custom bean
	 * editors.
	 * 
	 * <p>
	 * This method does nothing itself, and is designed to have custom
	 * implementation in extending classes.
	 * </p>
	 * 
	 * @param bean
	 *        the bean in question
	 */
	protected void registerCustomEditors(BeanWrapper bean) {
		// nothing here... bean.registerCustomEditor(...)
	}

	/**
	 * Get a SAX InputSource from a URLConnection's InputStream.
	 * 
	 * <p>
	 * This method handles {@code gzip} and {@code deflate} decoding
	 * automatically, if the {@code contentType} is reported as such.
	 * </p>
	 * 
	 * @param conn
	 *        the URLConnection
	 * @return the InputSource
	 * @throws IOException
	 *         if any IO error occurs
	 */
	protected InputSource getInputSourceFromURLConnection(URLConnection conn) throws IOException {
		Reader resp = null;
		try {
			resp = getUnicodeReaderFromURLConnection(conn);

			// for now we are reading entire response into memory... might want
			// to save to a temporary file if response very large, but we assume
			// for now the responses will be fairly small
			String respXml = FileCopyUtils.copyToString(resp);

			log.trace("Got response XML from URL [{}]:\n{}", conn.getURL(), respXml);
			if ( respXml.length() < 1 ) {
				// no data in response... can't be valid so throw IOException
				throw new IOException("Empty response from [" + conn.getURL() + "]");
			}
			return new InputSource(new StringReader(respXml));
		} finally {
			if ( resp != null ) {
				try {
					resp.close();
				} catch ( IOException e ) {
					// ignore me
				}
			}
		}
	}

	/**
	 * Send a bean as a web form POST and return an XML InputSource from the
	 * response content.
	 * 
	 * @param bean
	 *        the bean
	 * @param url
	 *        the URL to POST to
	 * @param attributes
	 *        extra POST attributes and bean override values
	 * @return an InputSource to the response content XML
	 */
	protected InputSource webFormPost(BeanWrapper bean, String url, Map<String, ?> attributes) {
		try {
			URLConnection conn = getURLConnection(url, HTTP_METHOD_POST);

			OutputStreamWriter out = new OutputStreamWriter(conn.getOutputStream(), "UTF-8");
			writeURLEncodedBeanProperties(bean, attributes, out);

			return getInputSourceFromURLConnection(conn);
		} catch ( IOException e ) {
			if ( log.isTraceEnabled() ) {
				log.trace("IOException posting " + bean + " to " + url, e);
			} else if ( log.isDebugEnabled() ) {
				log.debug("Unable to post data to " + url + ": " + e.getMessage());
			}
			throw new RuntimeException(e);
		}
	}

	private void writeURLEncodedBeanProperties(BeanWrapper bean, Map<String, ?> attributes, Writer out)
			throws IOException {
		PropertyDescriptor[] props = bean.getPropertyDescriptors();
		boolean propsWritten = false;
		if ( attributes != null && attributes.containsKey(ATTR_NODE_ID) ) {
			out.write("nodeId=" + attributes.get(ATTR_NODE_ID));
			propsWritten = true;
		}
		for ( int i = 0; i < props.length; i++ ) {
			PropertyDescriptor prop = props[i];
			if ( prop.getReadMethod() == null ) {
				continue;
			}
			String propName = prop.getName();
			if ( "class".equals(propName) ) {
				continue;
			}
			Object propValue = null;
			if ( attributes != null && attributes.containsKey(propName) ) {
				propValue = attributes.get(propName);
			} else {
				PropertyEditor editor = bean.findCustomEditor(prop.getPropertyType(), prop.getName());
				if ( editor != null ) {
					editor.setValue(bean.getPropertyValue(propName));
					propValue = editor.getAsText();
				} else {
					propValue = bean.getPropertyValue(propName);
				}
			}
			if ( propValue == null ) {
				continue;
			}
			if ( propsWritten ) {
				out.write("&");
			}
			out.write(propName);
			out.write("=");
			out.write(URLEncoder.encode(propValue.toString(), "UTF-8"));
			propsWritten = true;
		}
		out.flush();
		out.close();
	}

	/**
	 * Send a bean as a web form GET and return an XML InputSource from the
	 * response content.
	 * 
	 * @param bean
	 *        the bean to extract GET parameters from, or <em>null</em> for no
	 *        parameters
	 * @param url
	 *        the URL to GET to
	 * @param attributes
	 *        extra GET attributes and bean override values
	 * @return an InputSource to the response content XML
	 */
	protected InputSource webFormGet(BeanWrapper bean, String url, Map<String, ?> attributes) {
		try {
			String getUrl = url;
			if ( bean != null ) {
				StringWriter out = new StringWriter();
				writeURLEncodedBeanProperties(bean, attributes, out);
				getUrl = getUrl + '?' + out.toString();
			}

			URLConnection conn = getURLConnection(getUrl, HTTP_METHOD_GET);

			return getInputSourceFromURLConnection(conn);
		} catch ( IOException e ) {
			if ( log.isTraceEnabled() ) {
				log.trace("IOException getting " + bean + " from " + url, e);
			} else if ( log.isDebugEnabled() ) {
				log.debug("Unable to get data: " + e.getMessage());
			}
			throw new RuntimeException(e);
		}
	}

	/**
	 * Send a bean as a web form POST and parse the XML response as bean
	 * properties.
	 * 
	 * @param bean
	 *        the bean to POST
	 * @param obj
	 *        the result bean to populate from the HTTP response XML
	 * @param url
	 *        the URL to POST to
	 * @param attributes
	 *        extra POST attributes and bean override values
	 * @param xpathMap
	 *        the mapping of JavaBean property names to XPaths
	 */
	protected void webFormPostForBean(BeanWrapper bean, Object obj, String url,
			Map<String, ?> attributes, Map<String, XPathExpression> xpathMap) {
		InputSource is = webFormPost(bean, url, attributes);
		Document doc;
		try {
			doc = getDocBuilderFactory().newDocumentBuilder().parse(is);
		} catch ( SAXException e ) {
			throw new RuntimeException(e);
		} catch ( IOException e ) {
			throw new RuntimeException(e);
		} catch ( ParserConfigurationException e ) {
			throw new RuntimeException(e);
		}

		extractBeanDataFromXml(obj, doc.getDocumentElement(), xpathMap);
	}

	/**
	 * Send a bean as a web GET and parse the XML response as bean properties.
	 * 
	 * <p>
	 * This method calls {@link #webFormGet(BeanWrapper, String, Map)} followed
	 * by {@link #extractBeanDataFromXml(Object, Node, Map)}.
	 * </p>
	 * 
	 * @param bean
	 *        the bean whose properties to send as GET parameters, or
	 *        <em>null</em> for no parameters
	 * @param obj
	 *        the result bean to populate from the HTTP response XML
	 * @param url
	 *        the URL to GET
	 * @param attributes
	 *        extra GET attributes and bean override values
	 * @param xpathMap
	 *        the mapping of JavaBean property names to XPaths
	 * @see #webFormGet(BeanWrapper, String, Map)
	 */
	protected void webFormGetForBean(BeanWrapper bean, Object obj, String url,
			Map<String, ?> attributes, Map<String, XPathExpression> xpathMap) {
		InputSource is = webFormGet(bean, url, attributes);
		Document doc;
		try {
			doc = getDocBuilderFactory().newDocumentBuilder().parse(is);
		} catch ( SAXException e ) {
			throw new RuntimeException(e);
		} catch ( IOException e ) {
			throw new RuntimeException(e);
		} catch ( ParserConfigurationException e ) {
			throw new RuntimeException(e);
		}

		extractBeanDataFromXml(obj, doc.getDocumentElement(), xpathMap);
	}

	/**
	 * Extract a tracking ID from an XML string.
	 * 
	 * @param xml
	 *        the XML to extract from
	 * @param xp
	 *        the XPath to use that returns a number
	 * @param xpath
	 *        the XPath as a string (for debugging)
	 * @return the tracking ID, or <em>null</em> if not found
	 */
	protected Long extractTrackingId(InputSource xml, XPathExpression xp, String xpath) {
		Double tid;
		try {
			tid = (Double) xp.evaluate(xml, XPathConstants.NUMBER);
		} catch ( XPathExpressionException e ) {
			throw new RuntimeException(e);
		}
		if ( tid.isNaN() ) {
			log.warn("Unable to extract tracking ID via XPath [{}]", xpath);
			return null;
		}
		return tid.longValue();
	}

	/**
	 * Send a bean as a web form POST and parse the XML response for a bean.
	 * 
	 * @param bean
	 *        the bean
	 * @param url
	 *        the URL to POST to
	 * @param trackingIdXPath
	 *        the XPath for extracting the tracking ID
	 * @param xpath
	 *        the trackingIdXPath as a String (for debugging)
	 * @param attributes
	 *        extra POST attributes and bean override values
	 * @return the extracted tracking ID, or <em>null</em> if none found
	 */
	protected Long webFormPostForTrackingId(BeanWrapper bean, String url,
			XPathExpression trackingIdXPath, String xpath, Map<String, ?> attributes) {
		InputSource is = webFormPost(bean, url, attributes);

		// extract the returned tracking ID via XPath
		return extractTrackingId(is, trackingIdXPath, xpath);
	}

	/**
	 * Add source metadata using the configured {@link DatumMetadataService} (if
	 * available). The metadata will be cached so that subseqent calls to this
	 * method with the same metadata value will not try to re-save the unchanged
	 * value. This method will catch all exceptions and silently discard them.
	 * 
	 * @param sourceId
	 *        the source ID to add metadata to
	 * @param meta
	 *        the metadata to add
	 * @param returns
	 *        <em>true</em> if the metadata was saved successfully, or does not
	 *        need to be updated
	 */
	protected boolean addSourceMetadata(final String sourceId, final GeneralDatumMetadata meta) {
		if ( sourceId == null ) {
			return false;
		}
		GeneralDatumMetadata cached = sourceMetadataCache.get(sourceId);
		if ( cached != null && meta.equals(cached) ) {
			// we've already posted this metadata... don't bother doing it again
			log.debug("Source {} metadata already added, not posting again", sourceId);
			return true;
		}
		DatumMetadataService service = null;
		if ( datumMetadataService != null ) {
			service = datumMetadataService.service();
		}
		if ( service == null ) {
			return false;
		}
		try {
			service.addSourceMetadata(sourceId, meta);
			sourceMetadataCache.put(sourceId, meta);
			return true;
		} catch ( Exception e ) {
			log.debug("Error saving source {} metadata: {}", sourceId, e.getMessage());
		}
		return false;
	}

	/**
	 * Post a {@link DatumDataSource#EVENT_TOPIC_DATUM_CAPTURED} {@link Event}.
	 * 
	 * <p>
	 * This method calls {@link #createDatumCapturedEvent(Datum, Class)} to
	 * create the actual Event, which may be overridden by extending classes.
	 * </p>
	 * 
	 * @param datum
	 *        the {@link Datum} to post the event for
	 * @param eventDatumType
	 *        the Datum class to use for the
	 *        {@link DatumDataSource#EVENT_DATUM_CAPTURED_DATUM_TYPE} property
	 * @since 2.1
	 */
	protected final void postDatumCapturedEvent(final Datum datum,
			final Class<? extends Datum> eventDatumType) {
		EventAdmin ea = (eventAdmin == null ? null : eventAdmin.service());
		if ( ea == null || datum == null ) {
			return;
		}
		Event event = createDatumCapturedEvent(datum, eventDatumType);
		ea.postEvent(event);
	}

	/**
	 * Create a new {@link DatumDataSource#EVENT_TOPIC_DATUM_CAPTURED}
	 * {@link Event} object out of a {@link Datum}.
	 * 
	 * <p>
	 * This method will populate all simple properties of the given
	 * {@link Datum} into the event properties, along with the
	 * {@link DatumDataSource#EVENT_DATUM_CAPTURED_DATUM_TYPE}.
	 * 
	 * @param datum
	 *        the datum to create the event for
	 * @param eventDatumType
	 *        the Datum class to use for the
	 *        {@link DatumDataSource#EVENT_DATUM_CAPTURED_DATUM_TYPE} property
	 * @return the new Event instance
	 * @since 2.1
	 */
	protected Event createDatumCapturedEvent(final Datum datum,
			final Class<? extends Datum> eventDatumType) {
		Map<String, Object> props = ClassUtils.getSimpleBeanProperties(datum, null);
		props.put(DatumDataSource.EVENT_DATUM_CAPTURED_DATUM_TYPE, eventDatumType.getName());
		log.debug("Created {} event with props {}", DatumDataSource.EVENT_TOPIC_DATUM_CAPTURED, props);
		return new Event(DatumDataSource.EVENT_TOPIC_DATUM_CAPTURED, props);
	}

	public NamespaceContext getNsContext() {
		return nsContext;
	}

	public void setNsContext(NamespaceContext nsContext) {
		this.nsContext = nsContext;
	}

	public DocumentBuilderFactory getDocBuilderFactory() {
		return docBuilderFactory;
	}

	public void setDocBuilderFactory(DocumentBuilderFactory docBuilderFactory) {
		this.docBuilderFactory = docBuilderFactory;
	}

	public XPathFactory getXpathFactory() {
		return xpathFactory;
	}

	public void setXpathFactory(XPathFactory xpathFactory) {
		this.xpathFactory = xpathFactory;
	}

	public TransformerFactory getTransformerFactory() {
		return transformerFactory;
	}

	public void setTransformerFactory(TransformerFactory transformerFactory) {
		this.transformerFactory = transformerFactory;
	}

	public OptionalService<EventAdmin> getEventAdmin() {
		return eventAdmin;
	}

	public void setEventAdmin(OptionalService<EventAdmin> eventAdmin) {
		this.eventAdmin = eventAdmin;
	}

	public OptionalService<DatumMetadataService> getDatumMetadataService() {
		return datumMetadataService;
	}

	public void setDatumMetadataService(OptionalService<DatumMetadataService> datumMetadataService) {
		this.datumMetadataService = datumMetadataService;
	}

}
