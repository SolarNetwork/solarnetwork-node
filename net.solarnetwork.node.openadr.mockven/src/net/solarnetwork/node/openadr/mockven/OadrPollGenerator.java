
package net.solarnetwork.node.openadr.mockven;

import openadr.model.v20b.OadrPayload;
import openadr.model.v20b.OadrPoll;
import openadr.model.v20b.OadrSignedObject;

public class OadrPollGenerator extends OadrPayloadGenerator {

	OadrPayload createPayload(OadrParams params) {
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
