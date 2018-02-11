
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

public class TopNodeConnection {

	private HttpHeaders headers;
	private RestTemplate rest;
	private JAXBContext jaxbContext;
	private Marshaller marshaller;

	public TopNodeConnection() {
		headers = new HttpHeaders();
		rest = new RestTemplate();
		headers.setContentType(MediaType.TEXT_XML);
		try {
			jaxbContext = JAXBContext.newInstance("openadr.model.v20b:" + "openadr.model.v20b.atom:"
					+ "openadr.model.v20b.currency:" + "openadr.model.v20b.ei:"
					+ "openadr.model.v20b.emix:" + "openadr.model.v20b.gml:"
					+ "openadr.model.v20b.greenbutton:" + "openadr.model.v20b.power:"
					+ "openadr.model.v20b.pyld:" + "openadr.model.v20b.siscale:"
					+ "openadr.model.v20b.strm:" + "openadr.model.v20b.xcal:"
					+ "openadr.model.v20b.xmldsig:" + "openadr.model.v20b.xmldsig11");

			marshaller = jaxbContext.createMarshaller();
		} catch ( JAXBException e ) {
			throw new RuntimeException(e);
		}
	}

	public OadrPayload postPayload(String url, OadrPayload payload) {
		StringWriter out = new StringWriter();
		try {
			marshaller.marshal(payload, out);

			ResponseEntity<String> response = rest.postForEntity(url, out.toString(), String.class);

			Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();

			OadrPayload returnPayload = (OadrPayload) jaxbUnmarshaller
					.unmarshal(new InputSource(new StringReader(response.getBody())));

			//TODO remove print statment
			System.out.println(response.getBody());
			return returnPayload;

		} catch ( JAXBException e ) {
			throw new RuntimeException(e);
		}

	}

}
