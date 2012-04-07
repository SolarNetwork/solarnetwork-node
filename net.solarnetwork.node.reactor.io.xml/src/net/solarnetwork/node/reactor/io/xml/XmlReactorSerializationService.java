/* ==================================================================
 * XmlReactorSerializationService.java - Mar 1, 2011 4:43:03 PM
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

package net.solarnetwork.node.reactor.io.xml;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;

import net.solarnetwork.node.reactor.Instruction;
import net.solarnetwork.node.reactor.ReactorSerializationService;
import net.solarnetwork.node.reactor.support.BasicInstruction;
import net.solarnetwork.node.support.XmlServiceSupport;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * XML-based IO support for ReactorService.
 * 
 * <p>Example XML:</p>
 * 
 * <pre>
 * 	 &lt;NodeInstruction 
 * 	 	topic="Mock/Topic" 
 * 	 	instructionDate="2011-03-01T16:23:21.492+13:00" 
 * 	 	id="1">
 *     &lt;parameters>
 *     	&lt;InstructionParameter name="foo" value="bar" />
 *     	&lt;InstructionParameter name="foo" value="bar" />
 *     &lt;/parameters>
 *   &lt;/NodeInstruction>
 * </pre>
 * 
 * <p>The configurable properties of this class are:</p>
 * 
 * <dl class="class-properties">
 *   <dt>instructionXPath</dt>
 *   <dd>The XPath to the {@code NodeInstruction} element. Defaults to
 *   {@link #DEFAULT_INSTRUCTION_XPATH}.</dd>
 * </dl>
 * 
 * @author matt
 * @version $Revision$
 */
public class XmlReactorSerializationService extends XmlServiceSupport
implements ReactorSerializationService {
	
	/** The default XPath to look for XML instruction data. */
	public static final String DEFAULT_INSTRUCTION_XPATH = "//NodeInstruction";
	
	private String instructionXPath = DEFAULT_INSTRUCTION_XPATH;
	
	@Override
	public List<Instruction> decodeInstructions(String instructorId, Object in, String type,
			Map<String, ?> properties) {
		if ( !"text/xml".equalsIgnoreCase(type) ) {
			throw new IllegalArgumentException("The [" +type 
					+"] is not supported.");
		}
		
		if ( in instanceof Node ) {
			try {
				return decodeInstructions(instructorId, (Node)in);
			} catch (XPathExpressionException e) {
				throw new RuntimeException(e);
			} catch (ParseException e) {
				throw new RuntimeException(e);
			}
		} else {
			throw new IllegalArgumentException("The data object [" 
					+in +"] is not supported.");
		}
	}

	private List<Instruction> decodeInstructions(String instructorId, Node in) throws ParseException, XPathExpressionException {
		XPath xpath = getXpathFactory().newXPath();
		NodeList list = (NodeList)xpath.evaluate(instructionXPath, in, XPathConstants.NODESET);
		List<Instruction> results = new ArrayList<Instruction>(list.getLength());
		for ( int i = 0, len = list.getLength(); i < len; i++ ) {
			Element el = (Element)list.item(i);
			Instruction instr = decodeInstruction(instructorId, xpath, el);
			if ( instr != null ) {
				results.add(instr);
			}
		}
		return results;
	}
	
	/* Example XML:
	 <NodeInstruction 
	 	topic="Mock/Topic" 
	 	instructionDate="2011-03-01T16:23:21Z" 
	 	id="1">
      <parameters>
      	 <InstructionParameter name="foo" value="bar" />
      </parameters>
    </NodeInstruction>
	 */

	private Instruction decodeInstruction(String instructorId, XPath xpath, Element in) throws ParseException, XPathExpressionException {
		final String topic = in.getAttribute("topic");
		final String instructionId = in.getAttribute("id");
		
		String dateStr = in.getAttribute("instructionDate");
		SimpleDateFormat sdf = new java.text.SimpleDateFormat();
		if ( dateStr.charAt(dateStr.length() - 1) == 'Z' ) {
			sdf.applyPattern("yyyy-MM-dd'T'HH:mm:ss'Z'");
			sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
		} else {
			sdf.applyPattern("yyyy-MM-dd'T'HH:mm:ssZ");
			if ( dateStr.charAt(dateStr.length() - 3) == ':' ) {
				StringBuffer buf = new StringBuffer(dateStr);
				buf.deleteCharAt(dateStr.length() - 3);
				dateStr = buf.toString();
			}
		}
		final Date instructionDate = sdf.parse(dateStr);

		BasicInstruction result = new BasicInstruction(topic, instructionDate, 
				instructionId, instructorId, null);
		
		// look for parameters
		NodeList entries = (NodeList)xpath.evaluate("parameters/InstructionParameter", in, XPathConstants.NODESET);
		for ( int i = 0, len = entries.getLength(); i < len; i++ ) {
			Element entryNode = (Element)entries.item(i);
			String paramName = entryNode.getAttribute("name");
			String paramValue = entryNode.getAttribute("value");
			result.addParameter(paramName, paramValue);
		}
		return result;
	}

	@Override
	public Object encodeInstructions(Collection<Instruction> instructions,
			String type, Map<String, ?> properties) {
		// TODO Auto-generated method stub
		return null;
	}

	public String getInstructionXPath() {
		return instructionXPath;
	}
	public void setInstructionXPath(String instructionXPath) {
		this.instructionXPath = instructionXPath;
	}

}
