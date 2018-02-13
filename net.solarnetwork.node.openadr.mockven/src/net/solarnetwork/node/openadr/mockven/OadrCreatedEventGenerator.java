
package net.solarnetwork.node.openadr.mockven;

import openadr.model.v20b.OadrCreatedEvent;
import openadr.model.v20b.OadrDistributeEvent;
import openadr.model.v20b.OadrPayload;
import openadr.model.v20b.OadrSignedObject;
import openadr.model.v20b.ei.EiResponse;
import openadr.model.v20b.ei.EventDescriptor;
import openadr.model.v20b.ei.EventResponses;
import openadr.model.v20b.ei.EventResponses.EventResponse;
import openadr.model.v20b.ei.OptTypeType;
import openadr.model.v20b.ei.QualifiedEventID;
import openadr.model.v20b.ei.ResponseCode;
import openadr.model.v20b.pyld.EiCreatedEvent;

public class OadrCreatedEventGenerator {

	public OadrPayload createPayload(OadrParams params, OadrDistributeEvent event) {
		//NOTE HARDCODED FOR ONE EVENT
		EventDescriptor eventParams = event.getOadrEvents().get(0).getEiEvent().getEventDescriptor();
		String eventID = eventParams.getEventID();
		Long modNum = eventParams.getModificationNumber();
		String requestId = event.getRequestID();

		//we have received and event 
		OadrPayload payload = new OadrPayload();
		OadrSignedObject signedObject = new OadrSignedObject();
		OadrCreatedEvent createdEvent = new OadrCreatedEvent();
		payload.setOadrSignedObject(signedObject.withOadrCreatedEvent(createdEvent));

		createdEvent.setSchemaVersion(params.getProfileName());
		EiCreatedEvent eiCreatedEvent = new EiCreatedEvent();
		eiCreatedEvent.setVenID(params.getVenID());

		EiResponse eiResponse = new EiResponse();
		eiResponse.setRequestID(requestId);//check that is not from event 
		eiResponse.setResponseCode(new ResponseCode().withValue("200"));

		eiCreatedEvent.setEiResponse(eiResponse);

		EventResponse eventResponse = new EventResponse();

		eventResponse.setOptType(OptTypeType.OPT_IN);
		eventResponse.setQualifiedEventID(
				new QualifiedEventID().withEventID(eventID).withModificationNumber(modNum));

		EventResponses eventResponses = new EventResponses();
		eventResponses.getEventResponses().add(eventResponse);
		eiCreatedEvent.setEventResponses(eventResponses);

		createdEvent.setEiCreatedEvent(eiCreatedEvent);

		return payload;
	}

}
