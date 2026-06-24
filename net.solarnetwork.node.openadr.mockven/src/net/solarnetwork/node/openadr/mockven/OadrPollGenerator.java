
package net.solarnetwork.node.openadr.mockven;

import openadr.model.v20b.OadrPayload;
import openadr.model.v20b.OadrPoll;
import openadr.model.v20b.OadrSignedObject;

/**
 * 
 * Creates a OadrPayload containing a OadrPoll. An OadrPoll is used to ask the
 * VTN has anything happened and to ensure there is still communication with the
 * VTN.
 * 
 * @author robert
 * @version 1.0
 */
public class OadrPollGenerator extends OadrPayloadGenerator {

	public OadrPayload createPayload(OadrParams params) {
		OadrPayload payload = new OadrPayload();
		OadrSignedObject signedObject = new OadrSignedObject();
		OadrPoll poll = new OadrPoll();

		payload.withOadrSignedObject(signedObject.withOadrPoll(poll));

		//These values should be set from the registration
		poll.setVenID(params.getVenID());
		poll.setSchemaVersion(params.getProfileName());

		return payload;
	}
}
