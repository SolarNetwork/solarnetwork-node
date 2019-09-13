/* ==================================================================
 * KcdLoader.java - 13/09/2019 6:08:07 pm
 * 
 * Copyright 2019 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.io.canbus.support;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PushbackInputStream;
import java.nio.ByteOrder;
import java.util.HashSet;
import java.util.List;
import java.util.zip.GZIPInputStream;
import javax.xml.XMLConstants;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.UnmarshalException;
import javax.xml.bind.Unmarshaller;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;
import com.github.kayak.canio.kcd.BasicLabelType;
import com.github.kayak.canio.kcd.Bus;
import com.github.kayak.canio.kcd.Consumer;
import com.github.kayak.canio.kcd.Label;
import com.github.kayak.canio.kcd.LabelGroup;
import com.github.kayak.canio.kcd.LabelSet;
import com.github.kayak.canio.kcd.Message;
import com.github.kayak.canio.kcd.Multiplex;
import com.github.kayak.canio.kcd.MuxGroup;
import com.github.kayak.canio.kcd.NetworkDefinition;
import com.github.kayak.canio.kcd.Node;
import com.github.kayak.canio.kcd.NodeRef;
import com.github.kayak.canio.kcd.Producer;
import com.github.kayak.canio.kcd.Signal;
import com.github.kayak.canio.kcd.Value;
import com.github.kayak.core.description.BusDescription;
import com.github.kayak.core.description.DescriptionLoader;
import com.github.kayak.core.description.Document;
import com.github.kayak.core.description.MessageDescription;
import com.github.kayak.core.description.MultiplexDescription;
import com.github.kayak.core.description.SignalDescription;

/**
 * Loader of KCD data.
 * 
 * <p>
 * Adapted from https://github.com/dschanoeh/Kayak/tree/master/Kayak-kcd.
 * </p>
 * 
 * @author Jan-Niklas Meier < dschanoeh@googlemail.com >
 * @author matt
 * @version 1.0
 */
public class KcdLoader implements DescriptionLoader {

	private static final Logger logger = LoggerFactory.getLogger(KcdLoader.class);

	private Schema schema;
	private JAXBContext context;

	/**
	 * Constructor.
	 * 
	 * @throws RuntimeException
	 *         if the KCD JAXB context or XML Schema cannot be loaded
	 */
	public KcdLoader() {
		SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
		InputStream resourceAsStream = getClass().getResourceAsStream("Definition.xsd");
		Source s = new StreamSource(resourceAsStream);
		try {
			schema = schemaFactory.newSchema(s);
			context = JAXBContext.newInstance(new Class[] { NetworkDefinition.class });
		} catch ( JAXBException | SAXException ex ) {
			logger.error("Could not create KCD XML context: ", ex);
			throw new RuntimeException(ex);
		}
	}

	private static final int GZIP_MAGIC = 0x1f8b;

	private InputStream inputStreamForStream(InputStream in) throws IOException {
		// checking for GZIP
		if ( in instanceof GZIPInputStream ) {
			return in;
		}
		PushbackInputStream s = new PushbackInputStream(in, 2);
		int count = 0;
		byte[] magic = new byte[] { -1, -1 };
		while ( count < 2 ) {
			int readCount = s.read(magic, count, 2 - count);
			if ( readCount < 0 ) {
				break;
			}
			count += readCount;
		}
		s.unread(magic, 0, count);
		// GZIP magic bytes: 0x1F 0x8B
		if ( magic[0] == (byte) (GZIP_MAGIC >> 8) && magic[1] == (byte) (GZIP_MAGIC) ) {
			return new GZIPInputStream(s);
		}
		return s;
	}

	@Override
	public Document parseFile(File file) {
		try {
			return parse(new BufferedInputStream(new FileInputStream(file)), file.getAbsolutePath());
		} catch ( IOException e ) {
			throw new RuntimeException("Error parsing KCD file [" + file + "]", e);
		}
	}

	/**
	 * Parse KCD data from an input stream, using a given file name.
	 * 
	 * <p>
	 * GZIP encoded streams are supported and will be automatically detected.
	 * </p>
	 * 
	 * @param in
	 *        the input stream to read
	 * @param fileName
	 *        the file name to associate with the stream
	 * @return the parsed document
	 * @throws IOException
	 *         if any error occurs reading the data
	 */
	public Document parse(InputStream in, String fileName) throws IOException {
		try (InputStream input = inputStreamForStream(in)) {
			Unmarshaller umarshall = context.createUnmarshaller();
			umarshall.setSchema(schema);

			Object object = umarshall.unmarshal(input);

			if ( object instanceof NetworkDefinition ) {
				return createDocument((NetworkDefinition) object, fileName);
			} else {
				throw new IOException("Unexpected KCD data found.");
			}
		} catch ( IOException e ) {
			throw e;
		} catch ( UnmarshalException e ) {
			logger.warn("Invalid KCD data.", e);
			throw new IOException(e);
		} catch ( Exception e ) {
			logger.warn("Error loading KCD data.", e);
			throw new IOException(e);
		}
	}

	private Document createDocument(NetworkDefinition netdef, String fileName) {
		Document doc = new Document();

		com.github.kayak.canio.kcd.Document documentInfo = netdef.getDocument();
		doc.setVersion(documentInfo.getVersion());
		doc.setAuthor(documentInfo.getAuthor());
		doc.setCompany(documentInfo.getCompany());
		doc.setDate(documentInfo.getDate());
		doc.setName(documentInfo.getName());
		doc.setFileName(fileName);

		for ( Node n : netdef.getNode() ) {
			doc.createNode(n.getId(), n.getName());
		}

		for ( Bus b : netdef.getBus() ) {
			BusDescription description = doc.createBusDescription();
			description.setName(b.getName());
			description.setBaudrate(b.getBaudrate());

			/* Messages for each bus */
			for ( Message m : b.getMessage() ) {
				MessageDescription messageDescription;

				if ( m.getFormat().equals("extended") )
					messageDescription = new MessageDescription(
							Integer.parseInt(m.getId().substring(2), 16), true);
				else
					messageDescription = new MessageDescription(
							Integer.parseInt(m.getId().substring(2), 16), false);

				messageDescription.setInterval(m.getInterval());
				messageDescription.setName(m.getName());

				/* set producer */
				Producer producer = m.getProducer();
				if ( producer != null ) {
					List<NodeRef> ref = producer.getNodeRef();
					if ( ref.size() > 1 ) {

					} else if ( ref.size() == 1 ) {
						String id = ref.get(0).getId();
						com.github.kayak.core.description.Node n = doc.getNodeWithID(id);
						if ( n != null )
							messageDescription.setProducer(n);
					}
				}

				for ( Multiplex multiplex : m.getMultiplex() ) {
					MultiplexDescription multiplexDescription = messageDescription
							.createMultiplexDescription();

					/* Set multiplex values */
					if ( multiplex.getEndianess().equals("big") ) {
						multiplexDescription.setByteOrder(ByteOrder.BIG_ENDIAN);
					} else {
						multiplexDescription.setByteOrder(ByteOrder.LITTLE_ENDIAN);
					}
					multiplexDescription.setLength(multiplex.getLength());
					multiplexDescription.setOffset(multiplex.getOffset());
					multiplexDescription.setName(multiplex.getName());

					if ( multiplex.getValue() != null ) {
						String typeString = multiplex.getValue().getType();
						if ( typeString.equals("signed") ) {
							multiplexDescription.setType(SignalDescription.Type.SIGNED);
						} else if ( typeString.equals("double") ) {
							multiplexDescription.setType(SignalDescription.Type.DOUBLE);
						} else if ( typeString.equals("float") ) {
							multiplexDescription.setType(SignalDescription.Type.SINGLE);
						} else {
							multiplexDescription.setType(SignalDescription.Type.UNSIGNED);
						}
					}

					/* Transform MuxGroups to Signal lists */
					for ( MuxGroup group : multiplex.getMuxGroup() ) {
						long value = group.getCount();

						for ( Signal s : group.getSignal() ) {
							SignalDescription signalDescription = multiplexDescription
									.createMultiplexedSignal(value);
							signalToSignalDescription(s, signalDescription);

							/* set consumers */
							Consumer c = s.getConsumer();
							if ( c != null && c.getNodeRef() != null ) {
								List<NodeRef> signalRef = c.getNodeRef();
								HashSet<com.github.kayak.core.description.Node> consumers = new HashSet<com.github.kayak.core.description.Node>();

								for ( NodeRef nr : signalRef ) {
									com.github.kayak.core.description.Node n = doc
											.getNodeWithID(nr.getId());
									if ( n != null )
										consumers.add(n);
								}
							}
						}
					}
				}

				for ( Signal s : m.getSignal() ) {
					SignalDescription signalDescription = messageDescription.createSignalDescription();
					signalToSignalDescription(s, signalDescription);

					/* set consumers */
					Consumer c = s.getConsumer();
					if ( c != null && c.getNodeRef() != null ) {
						List<NodeRef> signalRef = c.getNodeRef();
						HashSet<com.github.kayak.core.description.Node> consumers = new HashSet<com.github.kayak.core.description.Node>();

						for ( NodeRef nr : signalRef ) {
							com.github.kayak.core.description.Node n = doc.getNodeWithID(nr.getId());
							if ( n != null )
								consumers.add(n);
						}
					}

				}
				description.addMessageDescription(messageDescription);
			}
		}

		return doc;
	}

	private SignalDescription signalToSignalDescription(Signal s, SignalDescription signalDescription) {
		if ( s.getEndianess().equals("big") ) {
			signalDescription.setByteOrder(ByteOrder.BIG_ENDIAN);
		} else {
			signalDescription.setByteOrder(ByteOrder.LITTLE_ENDIAN);
		}

		Value value = s.getValue();
		if ( value != null ) {
			double intercept = value.getIntercept();
			signalDescription.setIntercept(intercept);

			double slope = value.getSlope();
			signalDescription.setSlope(slope);

			String typeString = value.getType();
			if ( typeString.equals("signed") ) {
				signalDescription.setType(SignalDescription.Type.SIGNED);
			} else if ( typeString.equals("double") ) {
				signalDescription.setType(SignalDescription.Type.DOUBLE);
			} else if ( typeString.equals("float") ) {
				signalDescription.setType(SignalDescription.Type.SINGLE);
			} else {
				signalDescription.setType(SignalDescription.Type.UNSIGNED);
			}

			signalDescription.setUnit(value.getUnit());
		}

		signalDescription.setLength(s.getLength());
		signalDescription.setName(s.getName());
		signalDescription.setNotes(s.getNotes());
		signalDescription.setOffset(s.getOffset());

		LabelSet ls = s.getLabelSet();
		if ( ls != null ) {
			List<BasicLabelType> labels = ls.getLabelOrLabelGroup();
			if ( labels != null ) {
				for ( BasicLabelType basicLabel : labels ) {
					if ( basicLabel instanceof Label ) {
						Label l = (Label) basicLabel;
						com.github.kayak.core.description.Label label = new com.github.kayak.core.description.Label(
								l.getValue().longValue(), l.getName());
						signalDescription.addLabel(label);
					} else if ( basicLabel instanceof LabelGroup ) {
						LabelGroup l = (LabelGroup) basicLabel;
						com.github.kayak.core.description.Label label = new com.github.kayak.core.description.Label(
								l.getFrom().longValue(), l.getTo().longValue(), l.getName());
						signalDescription.addLabel(label);
					}
				}
			}
		}

		return signalDescription;
	}

	@Override
	public String[] getSupportedExtensions() {
		return new String[] { "kcd", "kcd.gz" };
	}

}
