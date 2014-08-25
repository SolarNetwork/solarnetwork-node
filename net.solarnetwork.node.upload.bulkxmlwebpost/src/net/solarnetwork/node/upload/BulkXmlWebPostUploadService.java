/* ==================================================================
 * BulkXmlWebPostUploadService.java - Feb 23, 2011 2:01:06 PM
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
 * $Id$
 * ==================================================================
 */

package net.solarnetwork.node.upload;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URLConnection;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.zip.GZIPOutputStream;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import net.solarnetwork.domain.NodeControlInfo;
import net.solarnetwork.node.BulkUploadResult;
import net.solarnetwork.node.BulkUploadService;
import net.solarnetwork.node.IdentityService;
import net.solarnetwork.node.domain.Datum;
import net.solarnetwork.node.reactor.Instruction;
import net.solarnetwork.node.reactor.InstructionAcknowledgementService;
import net.solarnetwork.node.reactor.InstructionStatus;
import net.solarnetwork.node.reactor.ReactorService;
import net.solarnetwork.node.reactor.support.InstructionStatusPropertyEditor;
import net.solarnetwork.node.support.XmlServiceSupport;
import net.solarnetwork.util.OptionalServiceTracker;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.PropertyAccessorFactory;
import org.springframework.beans.propertyeditors.CustomDateEditor;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 * {@link BulkUploadService} that uses an HTTP POST with body content formed as
 * an XML document containing all data to upload.
 * 
 * <p>
 * The response is expected to contain an XML document with one result element
 * for each uploaded datum, in the same order. Each datum that was successfully
 * uploaded must have an attribute in the corresponding result element that
 * contains a Long value; the {@code datumIdXPath} is used to extract these
 * values from the XML response.
 * </p>
 * 
 * <p>
 * The configurable properties of this class are:
 * </p>
 * 
 * <dl class="class-properties">
 * <dt>key</dt>
 * <dd>The unique key to assign to this upload service. The {@link #getKey()}
 * method will append this value to {@code BulkXmlWebPostUploadService:}.</dd>
 * 
 * <dt>datumIdXPath</dt>
 * <dd>An XPath to use for extracting the remote primary keys (tracking IDs)
 * from the web post XML response. This single XPath is used across all Datum
 * types and the entire XML response, so it must be generic enough to handle all
 * types. Defaults to {@link #DEFAULT_ID_XPATH}.</dd>
 * 
 * <dt>url</dt>
 * <dd>The URL to bulk post to.</dd>
 * 
 * <dt>compressXml</dt>
 * <dd>If <em>true</em> then GZip compress the XML document and encode as Base64
 * when uploading. Defaults to <em>true</em>.</dd>
 * 
 * <dt>identityService</dt>
 * <dd>The {@link IdentityService} for node details.</dd>
 * </dl>
 * 
 * @author matt
 * @version 1.2
 */
public class BulkXmlWebPostUploadService extends XmlServiceSupport implements BulkUploadService,
		InstructionAcknowledgementService {

	/** The date format to use for formatting dates with time. */
	public static final String DATE_TIME_FORMAT = "yyyy-MM-dd HH:mm:ss.SSSZ";

	/** The date format to use for DayDatum dates. */
	public static final String DAY_DATE_FORMAT = "yyyy-MM-dd";

	/** The date format to use for DayDatum times. */
	public static final String DAY_TIME_FORMAT = "HH:mm";

	/** The default value for the {@code datumIdXPath} property. */
	public static final String DEFAULT_ID_XPATH = "//result/*/@id";

	/** The default value for the {@code instructionXPath} property. */
	public static final String DEFAULT_INSTRUCTION_XPATH = "//NodeInstruction[1]";

	private String url = null;
	private String datumIdXPath = DEFAULT_ID_XPATH;
	private String instructionXPath = DEFAULT_INSTRUCTION_XPATH;
	private boolean compressXml = true;
	private OptionalServiceTracker<ReactorService> reactorService;

	@Override
	public String getKey() {
		return "BulkXmlWebPostUploadService:" + getIdentityService().getSolarNetHostName();
	}

	@Override
	public List<BulkUploadResult> uploadBulkDatum(Collection<Datum> data) {
		NodeList resultIds = upload(data);
		int count = (resultIds == null ? 0 : resultIds.getLength());
		List<BulkUploadResult> uploads = new ArrayList<BulkUploadResult>(count);
		Datum[] dataArray = data.toArray(new Datum[count]);
		for ( int i = 0; i < count; i++ ) {
			Node n = resultIds.item(i);
			String idStr = n.getNodeValue();
			Datum datum = dataArray[i];
			uploads.add(new BulkUploadResult(datum, idStr));
		}
		return uploads;
	}

	@Override
	public void acknowledgeInstructions(Collection<Instruction> instructions) {
		upload(instructions);
	}

	private NodeList upload(Collection<?> data) {
		// this is getting built in memory at the moment, it might be required
		// to build to a temp file to conserve memory?
		Document dom = null;
		Element root = null;
		try {
			dom = getDocBuilderFactory().newDocumentBuilder().newDocument();
			root = dom.createElement("bulkDatumUpload");
			dom.appendChild(root);

			// setup root attributes
			Long nodeId = getIdentityService().getNodeId();
			if ( nodeId != null ) {
				root.setAttribute("nodeId", nodeId.toString());
			}

		} catch ( ParserConfigurationException e ) {
			throw new RuntimeException(e);
		}
		for ( Object datum : data ) {
			Element datumElement = null;
			if ( datum instanceof Instruction ) {
				datumElement = getInstructionElement((Instruction) datum, dom);
			} else if ( datum instanceof NodeControlInfo ) {
				datumElement = getNodeControlInfoElement((NodeControlInfo) datum, dom);
			} else {
				BeanWrapper bean = getDefaultBeanWrapper(datum);
				datumElement = getElement(bean, dom);
			}
			if ( datumElement != null ) {
				root.appendChild(datumElement);
			}
		}

		InputSource is = handlePost(dom);
		Document resultDom = null;
		NodeList resultIds = null;
		Node instruction = null;
		try {
			resultDom = getDocBuilderFactory().newDocumentBuilder().parse(is);
			XPath xp = getXpathFactory().newXPath();
			if ( getNsContext() != null ) {
				xp.setNamespaceContext(getNsContext());
			}
			XPathExpression xpExp = xp.compile(datumIdXPath);
			resultIds = (NodeList) xpExp
					.evaluate(resultDom.getDocumentElement(), XPathConstants.NODESET);
			xpExp = xp.compile(instructionXPath);
			instruction = (Node) xpExp.evaluate(resultDom.getDocumentElement(), XPathConstants.NODE);
		} catch ( XPathExpressionException e ) {
			throw new RuntimeException(e);
		} catch ( SAXException e ) {
			throw new RuntimeException(e);
		} catch ( IOException e ) {
			throw new RuntimeException(e);
		} catch ( ParserConfigurationException e ) {
			throw new RuntimeException(e);
		}
		if ( resultIds != null ) {
			int count = resultIds.getLength();
			if ( count != data.size() ) {
				throw new RuntimeException("Expected " + data.size() + " result IDs, but found " + count);
			}
		}
		if ( instruction != null && reactorService != null && reactorService.isAvailable() ) {
			reactorService.getService().processInstruction(getIdentityService().getSolarInBaseUrl(),
					resultDom, "text/xml", null);
		}

		return resultIds;
	}

	private Element getInstructionElement(Instruction instruction, Document dom) {
		if ( instruction.getTopic() == null || instruction.getStatus() == null ) {
			return null;
		}
		Element el = dom.createElement("InstructionStatus");
		if ( instruction.getId() != null ) {
			el.setAttribute("id", instruction.getId().toString());
		}
		if ( instruction.getRemoteInstructionId() != null ) {
			el.setAttribute("instructionId", instruction.getRemoteInstructionId());
		}
		el.setAttribute("topic", instruction.getTopic());
		el.setAttribute("status", instruction.getStatus().getInstructionState().toString());
		/*
		 * For now, not posting parameters in ack... Iterable<String> paramNames
		 * = instruction.getParameterNames(); if ( paramNames != null ) { for (
		 * String paramName : instruction.getParameterNames() ) { String[]
		 * values = instruction.getAllParameterValues(paramName); for ( String
		 * value : values ) { Element param = dom.createElement("Parameter");
		 * el.appendChild(param); param.setAttribute("name", paramName);
		 * param.setAttribute("value", value); } } }
		 */
		return el;
	}

	private Element getNodeControlInfoElement(NodeControlInfo controlInfo, Document dom) {
		Element el = dom.createElement("NodeControlInfo");
		el.setAttribute("controlId", controlInfo.getControlId());
		if ( controlInfo.getType() != null ) {
			el.setAttribute("type", controlInfo.getType().toString());
		}
		if ( controlInfo.getPropertyName() != null ) {
			el.setAttribute("propertyName", controlInfo.getPropertyName());
		}
		if ( controlInfo.getUnit() != null ) {
			el.setAttribute("unit", controlInfo.getUnit());
		}
		if ( controlInfo.getValue() != null ) {
			el.setAttribute("value", controlInfo.getValue());
		}
		if ( controlInfo.getReadonly() != null ) {
			el.setAttribute("readonly", controlInfo.getReadonly().toString());
		}
		return el;
	}

	private InputSource handlePost(Document dom) {
		String postUrl = getIdentityService().getSolarInBaseUrl() + url;
		try {
			URLConnection conn = getURLConnection(postUrl, "POST");
			conn.setRequestProperty("Content-Type", "text/xml;charset=UTF-8");
			if ( compressXml ) {
				conn.setRequestProperty("Content-Encoding", "gzip");
			}
			OutputStream out = conn.getOutputStream();
			if ( compressXml ) {
				out = new GZIPOutputStream(out);
			}

			Source source = getSource(dom);
			Result result = new StreamResult(out);
			try {
				Transformer xform = getTransformerFactory().newTransformer();
				xform.transform(source, result);
			} catch ( TransformerConfigurationException e ) {
				throw new RuntimeException(e);
			} catch ( TransformerException e ) {
				throw new RuntimeException(e);
			}

			out.flush();
			out.close();

			return getInputSourceFromURLConnection(conn);
		} catch ( IOException e ) {
			if ( log.isTraceEnabled() ) {
				log.trace("IOException bulk posting data to " + postUrl, e);
			} else if ( log.isDebugEnabled() ) {
				log.debug("Unable to post data: " + e.getMessage());
			}
			throw new RuntimeException(e);
		}
	}

	private BeanWrapper getDefaultBeanWrapper(Object data) {
		BeanWrapper bean = PropertyAccessorFactory.forBeanPropertyAccess(data);
		SimpleDateFormat dateFormat = new SimpleDateFormat(DATE_TIME_FORMAT);
		dateFormat.setLenient(false);
		bean.registerCustomEditor(Date.class, new CustomDateEditor(dateFormat, false));

		// TODO need a way to configure these things instead of hard-coding them here
		SimpleDateFormat timeFormat = new SimpleDateFormat(DAY_TIME_FORMAT);
		dateFormat.setLenient(false);
		bean.registerCustomEditor(Date.class, "sunrise", new CustomDateEditor(timeFormat, false));
		bean.registerCustomEditor(Date.class, "sunset", new CustomDateEditor(timeFormat, false));

		SimpleDateFormat dayFormat = new SimpleDateFormat(DAY_DATE_FORMAT);
		dateFormat.setLenient(false);
		bean.registerCustomEditor(Date.class, "day", new CustomDateEditor(dayFormat, false));

		bean.registerCustomEditor(InstructionStatus.class, new InstructionStatusPropertyEditor());
		return bean;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public void setDatumIdXPath(String datumIdXPath) {
		this.datumIdXPath = datumIdXPath;
	}

	public void setCompressXml(boolean compressXml) {
		this.compressXml = compressXml;
	}

	public void setReactorService(OptionalServiceTracker<ReactorService> reactorService) {
		this.reactorService = reactorService;
	}

	public void setInstructionXPath(String instructionXPath) {
		this.instructionXPath = instructionXPath;
	}

}
