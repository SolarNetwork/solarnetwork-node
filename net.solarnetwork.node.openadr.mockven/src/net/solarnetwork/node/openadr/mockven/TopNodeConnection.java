
package net.solarnetwork.node.openadr.mockven;

import java.io.StringReader;
import java.io.StringWriter;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;
import org.xml.sax.InputSource;
import openadr.model.v20b.OadrPayload;
import openadr.model.v20b.ObjectFactory;

/**
 * 
 * Class that handles the connection to the VTN. Supports posting OadrPayloads
 * getting the response.
 * 
 * @author robert
 * @version 1.0
 */
public class TopNodeConnection {

	private HttpHeaders headers;
	private RestTemplate rest;
	private JAXBContext jaxbContext;
	private Marshaller marshaller;

	public TopNodeConnection() {
		headers = new HttpHeaders();
		rest = new RestTemplate();
		headers.setContentType(MediaType.TEXT_XML);

		//Require a class loader when using OSGI otherwise you get JAXBException
		//https://stackoverflow.com/a/1043807
		ClassLoader cl = ObjectFactory.class.getClassLoader();
		try {
			jaxbContext = JAXBContext.newInstance("openadr.model.v20b:" + "openadr.model.v20b.atom:"
					+ "openadr.model.v20b.currency:" + "openadr.model.v20b.ei:"
					+ "openadr.model.v20b.emix:" + "openadr.model.v20b.gml:"
					+ "openadr.model.v20b.greenbutton:" + "openadr.model.v20b.power:"
					+ "openadr.model.v20b.pyld:" + "openadr.model.v20b.siscale:"
					+ "openadr.model.v20b.strm:" + "openadr.model.v20b.xcal:"
					+ "openadr.model.v20b.xmldsig:" + "openadr.model.v20b.xmldsig11", cl);

			marshaller = jaxbContext.createMarshaller();
		} catch ( JAXBException e ) {
			//have no way of recovering from exception. So I turn it into a RuntimeException so I don't have to declare that this class throws and exception
			throw new RuntimeException(e);
		}
	}

	//I assume I have to synchronize this method to prevent trying to connect at the same time. Never tested multipal connections.
	public synchronized OadrPayload postPayload(String url, OadrPayload payload) {
		StringWriter out = new StringWriter();
		try {
			//turns the payload into a XML string and writes it to the StringWriter
			marshaller.marshal(payload, out);

			//send the xml to the VTN and get the response
			ResponseEntity<String> response = rest.postForEntity(url, out.toString(), String.class);

			Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();

			//convert the response into a OadrPayload object
			OadrPayload returnPayload = (OadrPayload) jaxbUnmarshaller
					.unmarshal(new InputSource(new StringReader(response.getBody())));

			//TODO remove print statment
			System.out.println(response.getBody());
			return returnPayload;

		} catch ( JAXBException e ) {
			//have no way of recovering from exception. So I turn it into a RuntimeException so I don't have to declare that this class throws and exception
			throw new RuntimeException(e);
		}

	}

}
