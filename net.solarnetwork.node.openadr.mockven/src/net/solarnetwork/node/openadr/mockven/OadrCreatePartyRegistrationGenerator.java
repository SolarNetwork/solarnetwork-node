
package net.solarnetwork.node.openadr.mockven;

import openadr.model.v20b.OadrCreatePartyRegistration;
import openadr.model.v20b.OadrPayload;
import openadr.model.v20b.OadrSignedObject;

/**
 * Class to generate OadrPayloads with OadrCreatePartyRegistration used for
 * registering with the VTN
 * 
 * @author robert
 * @version 1.0
 */
public class OadrCreatePartyRegistrationGenerator extends OadrPayloadGenerator {

	public OadrPayload createPayload(OadrParams params) {
		OadrPayload payload = new OadrPayload();
		OadrSignedObject signedObject = new OadrSignedObject();
		OadrCreatePartyRegistration partyReg = new OadrCreatePartyRegistration();
		payload.withOadrSignedObject(signedObject.withOadrCreatePartyRegistration(partyReg));

		//most of these values are defaults, I don't know what some of them mean I just say the EPRI VEN use them so im using them
		partyReg.setSchemaVersion(params.getProfileName());
		partyReg.setRequestID(genRandomRequestID());
		partyReg.setOadrHttpPullModel(params.isHttpPullModel());
		partyReg.setOadrVenName(params.getVenName());
		partyReg.setOadrXmlSignature(params.isXmlSignature());
		partyReg.setOadrReportOnly(params.isReportOnly());
		partyReg.setOadrTransportName(params.getTransportName());

		return payload;

	}
}
