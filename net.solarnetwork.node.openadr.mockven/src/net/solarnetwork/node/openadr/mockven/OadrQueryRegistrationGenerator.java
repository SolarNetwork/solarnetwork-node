
package net.solarnetwork.node.openadr.mockven;

import openadr.model.v20b.OadrPayload;
import openadr.model.v20b.OadrQueryRegistration;
import openadr.model.v20b.OadrSignedObject;

public class OadrQueryRegistrationGenerator extends OadrPayloadGenerator {

	OadrPayload createPayload(OadrParams params) {
		OadrPayload payload = new OadrPayload();
		OadrSignedObject signedObject = new OadrSignedObject();
		OadrQueryRegistration queryReg = new OadrQueryRegistration();
		payload.withOadrSignedObject(signedObject.withOadrQueryRegistration(queryReg));

		queryReg.setRequestID(genRandomRequestID());

		//TODO check if profile name and scheme version are the same thing
		queryReg.setSchemaVersion(params.getProfileName());

		return payload;

	}
}
