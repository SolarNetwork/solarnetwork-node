
package net.solarnetwork.node.openadr.mockven;

import openadr.model.v20b.OadrPayload;
import openadr.model.v20b.OadrQueryRegistration;
import openadr.model.v20b.OadrSignedObject;

/**
 * 
 * Creates a OadrPayload with a OadrQueryRegistration used in registering with
 * the VTN
 * 
 * @author robert
 * @version 1.0
 */
public class OadrQueryRegistrationGenerator extends OadrPayloadGenerator {

	public OadrPayload createPayload(OadrParams params) {
		OadrPayload payload = new OadrPayload();
		OadrSignedObject signedObject = new OadrSignedObject();
		OadrQueryRegistration queryReg = new OadrQueryRegistration();
		payload.withOadrSignedObject(signedObject.withOadrQueryRegistration(queryReg));

		queryReg.setRequestID(genRandomRequestID());

		//TODO I not sure if profilename and schema version are the same thing from what I saw it is always "2.0b"
		queryReg.setSchemaVersion(params.getProfileName());

		return payload;

	}
}
