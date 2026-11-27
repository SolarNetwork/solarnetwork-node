
package net.solarnetwork.node.openadr.mockven;

import openadr.model.v20b.OadrCreatedPartyRegistration;
import openadr.model.v20b.OadrDistributeEvent;
import openadr.model.v20b.OadrPayload;
import openadr.model.v20b.OadrSignedObject;

/**
 * 
 * Class to simulate a Virtual End Node (VEN). This class is designed to talk to
 * a Virtual Top Node (VTN) via OpenADR 2.0b
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

		/**
		 * currently the only supported response is for a OadrDistrubuteEvent.
		 * There are other possible message a VTN can send to a VEN such as
		 * asking for a report.
		 * 
		 */
		if ( response.getOadrDistributeEvent() != null ) {
			respondToDistributeEvent(response.getOadrDistributeEvent());
		}
	}

	/**
	 * This method takes a OadrDistrubuteEvent and sends the VTN a response on
	 * whether the VEN is opting in or not. As well as applying a demand
	 * response strategy if the VTN has chosen to opt into said event.
	 * 
	 * Modify this method for custom behaviours
	 * 
	 * @param event
	 */
	public void respondToDistributeEvent(OadrDistributeEvent event) {

		//I don't think it makes sense for the decision to opt in to be in the generator
		OadrCreatedEventGenerator generator = new OadrCreatedEventGenerator();

		/**
		 * You can potentially use this location for interacting with a demand
		 * response engine or some other mechanism for doing demand response on
		 * the solarnetwork all of the parameters for the event are inside the
		 * OadrDistributeEvent
		 * 
		 * https://pastebin.com/zrNwcQAX
		 * 
		 * e.g. Link above is an XML generated from the EPRI VTN asking for
		 * 250kW of demand response (specific LOAD_CONTROL with
		 * X_LOAD_CONTROL_CAPACITY)
		 * 
		 * if this was to be implemented here you can write some code to extract
		 * the "payloadFloat" value and pass it to a demand response engine
		 * 
		 * 
		 */
		OadrPayload payload = generator.createPayload(this, event);

		OadrPayload response = connection.postPayload(url + OadrSubDomains.EiEvent, payload);

	}

	/**
	 * Polls the VTN with an OadrPoll payload and returns the OadrPayload
	 * response. If the VEN is not registered to the VTN it tries to register
	 * first before polling.
	 * 
	 * @return
	 */
	private OadrPayload pollVTN() {
		if ( registered == false ) {
			queryAndRegister();
		}

		OadrPollGenerator generator = new OadrPollGenerator();

		OadrPayload payload = generator.createPayload(this);

		OadrPayload response = connection.postPayload(url + OadrSubDomains.OadrPoll, payload);
		return response;

	}

	/**
	 * sends a OadrQueryRegistration payload to the VTN and then proceeds to
	 * register with the VTN. Currently there is no error correction this method
	 * can fail if the VTN refuses registration, or if the VEN fails to make a
	 * connection to the VTN.
	 */
	public void queryAndRegister() {
		OadrQueryRegistrationGenerator payloadGen = new OadrQueryRegistrationGenerator();
		OadrPayload payload = payloadGen.createPayload(this);

		//Currently there is no procedure in place to handle if the VTN is unreachable eg invalid url
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
			/**
			 * TODO normally at this stage the VTN returns a register report and
			 * the VEN does a response to that, I have noticed that with the
			 * EPRI mocks that this step is not needed and one can just go and
			 * start polling. I don't know if this is allowed in the standard so
			 * it is best for in the future to follow proper steps.
			 */

		} else {
			//Perhaps in future this could be a checked exception
			throw new RuntimeException("Failed at registering with VTN");
		}
	}

}
