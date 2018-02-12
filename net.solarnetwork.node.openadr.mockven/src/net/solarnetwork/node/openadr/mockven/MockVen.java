
package net.solarnetwork.node.openadr.mockven;

import openadr.model.v20b.OadrCreatedEvent;
import openadr.model.v20b.OadrCreatedPartyRegistration;
import openadr.model.v20b.OadrDistributeEvent;
import openadr.model.v20b.OadrPayload;
import openadr.model.v20b.OadrPoll;
import openadr.model.v20b.OadrSignedObject;
import openadr.model.v20b.ei.EiResponse;
import openadr.model.v20b.ei.EventDescriptor;
import openadr.model.v20b.ei.EventResponses.EventResponse;
import openadr.model.v20b.ei.OptTypeType;
import openadr.model.v20b.ei.QualifiedEventID;
import openadr.model.v20b.ei.ResponseCode;
import openadr.model.v20b.pyld.EiCreatedEvent;

public class MockVen extends OadrParams {

	private boolean registered = false;
	private String url;
	private TopNodeConnection connection;

	public MockVen() {
		connection = new TopNodeConnection();
	}

	@Override
	public void setVenID(String venID) {
		if ( !venID.equals(getVenID()) ) {
			registered = false;
		}
		super.setVenID(venID);
	}

	@Override
	public void setVenName(String venName) {
		if ( !venName.equals(getVenName()) ) {
			registered = false;
		}
		super.setVenName(venName);
	}

	public boolean isRegisterd() {

		//this should query registration
		return registered;
	}

	public void setVtnURL(String url) {
		//ensure the URL ends with a / as we will be needing to call subdomains
		if ( !url.endsWith("/") ) {
			url = url + "/";
		}
		this.url = url;
	}

	//Polls the VTN and responds to events
	public void pollAndRespond() {
		OadrSignedObject response = pollVTN().getOadrSignedObject();
		if ( response.getOadrDistributeEvent() != null ) {
			//would it be better to have the response behavior expendable? 
			//I would say probably but for now lets hardcode some behavior
			//TODO
			respondToDistributeEvent(response.getOadrDistributeEvent());
		}
	}

	//in a good design I guess this would be modifiable to change response behavior 
	//I need to figure out the best class structure for not but for now lets hardcode
	//TODO
	private void respondToDistributeEvent(OadrDistributeEvent event) {
		//I should be using a generator but for now leave it here
		//I currently don't have a pattern for passing in values to a generator 
		//TODO

		//NOTE HARDCODED FOR ONE EVENT
		EventDescriptor params = event.getOadrEvents().get(0).getEiEvent().getEventDescriptor();
		String eventID = params.getEventID();
		Long modNum = params.getModificationNumber();
		String requestId = event.getRequestID();

		//we have received and event 
		OadrPayload payload = new OadrPayload();
		OadrSignedObject signedObject = new OadrSignedObject();
		OadrCreatedEvent createdEvent = new OadrCreatedEvent();
		payload.setOadrSignedObject(signedObject.withOadrCreatedEvent(createdEvent));

		createdEvent.setSchemaVersion(getProfileName());
		EiCreatedEvent eiCreatedEvent = new EiCreatedEvent();
		eiCreatedEvent.setVenID(getVenID());

		EiResponse eiResponse = new EiResponse();
		eiResponse.setRequestID(requestId);//check that is not from event 
		eiResponse.setResponseCode(new ResponseCode().withValue("200"));

		eiCreatedEvent.setEiResponse(eiResponse);

		EventResponse eventResponse = new EventResponse();
		eventResponse.setOptType(OptTypeType.OPT_IN);
		eventResponse.setQualifiedEventID(
				new QualifiedEventID().withEventID(eventID).withModificationNumber(modNum));
		//eiCreatedEvent.setEventResponses(value);

	}

	//Polls the VTN and does nothing
	public void poll() {
		//this method returns something but we ignore it
		pollVTN();
	}

	//only to be called when ven is registered to VTN 
	private OadrPayload pollVTN() {
		if ( registered == false ) {
			queryAndRegister();
		}

		if ( registered == false ) {
			throw new RuntimeException("Failed at registering with VTN");
		}
		//this should be in its own generator
		//TODO move this code
		OadrPayload payload = new OadrPayload();
		OadrSignedObject signedObject = new OadrSignedObject();
		OadrPoll poll = new OadrPoll();
		payload.withOadrSignedObject(signedObject.withOadrPoll(poll));

		//These values should be set from the registration
		poll.setVenID(this.getVenID());
		poll.setSchemaVersion(this.getProfileName());

		OadrPayload response = connection.postPayload(url + OadrSubDomains.OadrPoll, payload);
		return response;

	}

	//asks VTN if we are registered if not register
	public void queryAndRegister() {
		OadrQueryRegistrationGenerator payloadGen = new OadrQueryRegistrationGenerator();
		OadrPayload payload = payloadGen.createPayload(this);

		//should have better error handling.
		System.out.println(url + OadrSubDomains.EiRegisterParty);
		OadrPayload response = connection.postPayload(url + OadrSubDomains.EiRegisterParty, payload);
		OadrCreatedPartyRegistration partyReg = response.getOadrSignedObject()
				.getOadrCreatedPartyRegistration();

		//Check to see if that the VTN is okay before proceeding
		if ( partyReg.getEiResponse().getResponseDescription().equalsIgnoreCase("OK") ) {
			setVtnID(partyReg.getVtnID());
			OadrCreatePartyRegistrationGenerator payloadGen2 = new OadrCreatePartyRegistrationGenerator();
			payload = payloadGen2.createPayload(this);
			response = connection.postPayload(url + OadrSubDomains.EiRegisterParty, payload);
			partyReg = response.getOadrSignedObject().getOadrCreatedPartyRegistration();
			setRegistrationID(partyReg.getRegistrationID());
			setVenID(partyReg.getVenID());
			registered = true;
			//skip register report for now
		} else {
			//something went wrong 
		}
	}

}
