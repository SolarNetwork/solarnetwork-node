
package net.solarnetwork.node.openadr.mockven;

import openadr.model.v20b.OadrCreatedPartyRegistration;
import openadr.model.v20b.OadrDistributeEvent;
import openadr.model.v20b.OadrPayload;
import openadr.model.v20b.OadrSignedObject;

/**
 * 
 * Class to simulate a Virtual End Node (VEN). This class is designed to talk to
 * a VTN via OpenADR 2.0b
 * 
 * 
 * @author robert
 * @version 1.0
 */
public class MockVen extends OadrParams {

	private boolean registered = false;
	private String url;
	private TopNodeConnection connection;

	public MockVen() {
		connection = new TopNodeConnection();
	}

	@Override
	public void setVenName(String venName) {
		//if this parameter changes we assume we are no longer registered
		if ( !venName.equals(getVenName()) ) {
			registered = false;
		}
		super.setVenName(venName);
	}

	public void setVtnURL(String url) {
		//ensure the URL ends with a / as we will be needing to call subdomains from OadrSubDomains
		if ( !url.endsWith("/") ) {
			url = url + "/";
		}
		//if this parameter changes we assume we are no longer registered
		if ( !url.equals(this.url) ) {
			registered = false;
		}

		this.url = url;
	}

	/**
	 * sends a OadrPoll message to the VTN and acts according to the message
	 * sent back
	 */
	public void pollAndRespond() {

		if ( registered == false ) {
			queryAndRegister();
		}

		OadrSignedObject response = pollVTN().getOadrSignedObject();

		//currently the only supported response is for a OadrDistrubuteEvent
		if ( response.getOadrDistributeEvent() != null ) {
			respondToDistributeEvent(response.getOadrDistributeEvent());
		}
	}

	/**
	 * The
	 * 
	 * @param event
	 */
	public void respondToDistributeEvent(OadrDistributeEvent event) {

		//I don't think it makes sense for the decision to opt in to be in the generator
		OadrCreatedEventGenerator generator = new OadrCreatedEventGenerator();
		OadrPayload payload = generator.createPayload(this, event);

		OadrPayload response = connection.postPayload(url + OadrSubDomains.EiEvent, payload);

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

		OadrPollGenerator generator = new OadrPollGenerator();

		OadrPayload payload = generator.createPayload(this);

		OadrPayload response = connection.postPayload(url + OadrSubDomains.OadrPoll, payload);
		return response;

	}

	//asks VTN if we are registered if not register
	public void queryAndRegister() {
		OadrQueryRegistrationGenerator payloadGen = new OadrQueryRegistrationGenerator();
		OadrPayload payload = payloadGen.createPayload(this);

		//should have better error handling.
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
			throw new RuntimeException("Failed at registering with VTN");
		}
	}

}
