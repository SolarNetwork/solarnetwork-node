
package net.solarnetwork.node.openadr.mockven;

import openadr.model.v20b.OadrCreatePartyRegistration;
import openadr.model.v20b.OadrPayload;
import openadr.model.v20b.OadrSignedObject;

public class OadrCreatePartyRegistrationGenerator extends OadrPayloadGenerator {

	public OadrPayload createPayload(OadrParams params) {
		OadrPayload payload = new OadrPayload();
		OadrSignedObject signedObject = new OadrSignedObject();
		OadrCreatePartyRegistration partyReg = new OadrCreatePartyRegistration();
		payload.withOadrSignedObject(signedObject.withOadrCreatePartyRegistration(partyReg));

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
