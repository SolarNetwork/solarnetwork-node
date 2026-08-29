
package net.solarnetwork.node.openadr.mockven;

import java.time.Duration;
import java.util.Calendar;
import java.util.GregorianCalendar;
import openadr.model.v20b.OadrCreatedEvent;
import openadr.model.v20b.OadrDistributeEvent;
import openadr.model.v20b.OadrDistributeEvent.OadrEvent;
import openadr.model.v20b.OadrPayload;
import openadr.model.v20b.OadrSignedObject;
import openadr.model.v20b.ei.EiEventSignal;
import openadr.model.v20b.ei.EiResponse;
import openadr.model.v20b.ei.EventDescriptor;
import openadr.model.v20b.ei.EventResponses;
import openadr.model.v20b.ei.EventResponses.EventResponse;
import openadr.model.v20b.ei.OptTypeType;
import openadr.model.v20b.ei.QualifiedEventID;
import openadr.model.v20b.ei.ResponseCode;
import openadr.model.v20b.ei.SignalTypeEnumeratedType;
import openadr.model.v20b.pyld.EiCreatedEvent;

/**
 * 
 * Class to generate OadrPayloads containing OadrCreatedEvent. This class is
 * different to my other generators as it requires a OadrDistrubuteEvent object.
 * This class also has some logic in deciding whether to opt into events or not.
 * 
 * @author robert
 * @version 1.0
 */
public class OadrCreatedEventGenerator {

	/**
	 * 
	 * @param params
	 * @param event
	 * @return Payload with a OadrCreatedEvent
	 */
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

		eventResponse.setOptType(OptInLogic(params, event.getOadrEvents().get(0)));
		eventResponse.setQualifiedEventID(
				new QualifiedEventID().withEventID(eventID).withModificationNumber(modNum));

		EventResponses eventResponses = new EventResponses();
		eventResponses.getEventResponses().add(eventResponse);
		eiCreatedEvent.setEventResponses(eventResponses);

		createdEvent.setEiCreatedEvent(eiCreatedEvent);

		return payload;
	}

	//Feel free to overwrite this method to change opt in behavior.
	/**
	 * This method is used to decide whether to opt into the event or not it
	 * should not be used for applying a demand response instead put that logic
	 * in MocVen.respondToDistributeEvent()
	 * 
	 * @param params
	 * @param event
	 * @return
	 */
	public OptTypeType OptInLogic(OadrParams params, OadrEvent event) {

		//ensure that the signals are valid if not opt out. This is the behavior of the EPRI VEN
		for ( EiEventSignal eventSig : event.getEiEvent().getEiEventSignals().getEiEventSignals() ) {
			if ( !validSignalNameAndType(eventSig) ) {
				return OptTypeType.OPT_OUT;
			}
		}
		if ( eventOver(event) ) {
			return OptTypeType.OPT_OUT;
		}

		//proof of concept showing how to select a specific signalName
		if ( event.getEiEvent().getEiEventSignals().getEiEventSignals().get(0).getSignalName()
				.equals(SignalNameEnumeratedType.LOAD_CONTROL.toString()) ) {
			return OptTypeType.OPT_IN;
		}
		return OptTypeType.OPT_IN;
	}

	/**
	 * Returns true if the EiEventSignal has a singal name compatible with the
	 * signal type. This is a translation of the C# implementation in the EPRI
	 * mock VEN.
	 * 
	 * @param event
	 * @return
	 */
	public boolean validSignalNameAndType(EiEventSignal event) {
		String singalName = event.getSignalName();
		SignalTypeEnumeratedType signalType = event.getSignalType();

		SignalNameEnumeratedType sigName = SignalNameEnumeratedType.valueOf(singalName);

		switch (sigName) {
			case simple:
				if ( signalType.equals(SignalTypeEnumeratedType.LEVEL) ) {
					break;
				}
				return false;
			case SIMPLE:
				if ( signalType.equals(SignalTypeEnumeratedType.LEVEL) ) {
					break;
				}
				return false;
			case ELECTRICITY_PRICE:
				if ( signalType.equals(SignalTypeEnumeratedType.PRICE)
						|| signalType.equals(SignalTypeEnumeratedType.PRICE_RELATIVE)
						|| signalType.equals(SignalTypeEnumeratedType.PRICE_MULTIPLIER) ) {
					break;
				}
				return false;
			case ENERGY_PRICE:
				if ( signalType.equals(SignalTypeEnumeratedType.PRICE)
						|| signalType.equals(SignalTypeEnumeratedType.PRICE_RELATIVE)
						|| signalType.equals(SignalTypeEnumeratedType.PRICE_MULTIPLIER) ) {
					break;
				}
				return false;
			case DEMAND_CHARGE:
				if ( signalType.equals(SignalTypeEnumeratedType.PRICE)
						|| signalType.equals(SignalTypeEnumeratedType.PRICE_RELATIVE)
						|| signalType.equals(SignalTypeEnumeratedType.PRICE_MULTIPLIER) ) {
					break;
				}
				return false;
			case BID_PRICE:
				if ( signalType.equals(SignalTypeEnumeratedType.PRICE) ) {
					break;
				}
				return false;
			case BID_LOAD:
				if ( signalType.equals(SignalTypeEnumeratedType.SETPOINT) ) {
					break;
				}
				return false;
			case BID_ENERGY:
				if ( signalType.equals(SignalTypeEnumeratedType.SETPOINT) ) {
					break;
				}
				return false;
			case CHARGE_STATE:
				if ( signalType.equals(SignalTypeEnumeratedType.SETPOINT)
						|| signalType.equals(SignalTypeEnumeratedType.DELTA)
						|| signalType.equals(SignalTypeEnumeratedType.MULTIPLIER) ) {
					break;
				}
				return false;
			case LOAD_DISPATCH:
				if ( signalType.equals(SignalTypeEnumeratedType.SETPOINT)
						|| signalType.equals(SignalTypeEnumeratedType.DELTA)
						|| signalType.equals(SignalTypeEnumeratedType.MULTIPLIER)
						|| signalType.equals(SignalTypeEnumeratedType.LEVEL) ) {
					break;
				}
				return false;
			case LOAD_CONTROL:
				if ( signalType.equals(SignalTypeEnumeratedType.X_LOAD_CONTROL_CAPACITY)
						|| signalType.equals(SignalTypeEnumeratedType.X_LOAD_CONTROL_LEVEL_OFFSET)
						|| signalType.equals(SignalTypeEnumeratedType.X_LOAD_CONTROL_PERCENT_OFFSET)
						|| signalType.equals(SignalTypeEnumeratedType.X_LOAD_CONTROL_SETPOINT) ) {
					break;
				}
				return false;
		}

		return true;
	}

	/**
	 * returns true if the OadrEvent has already ended. A event is ended when
	 * the starting time plus the duration is less than the current time. This
	 * method is included to match the behaviour
	 * 
	 * @param event
	 * @return
	 */
	public boolean eventOver(OadrEvent event) {

		GregorianCalendar now = (GregorianCalendar) GregorianCalendar.getInstance();

		String durationString = event.getEiEvent().getEiActivePeriod().getProperties().getDuration()
				.getDuration().getValue();

		GregorianCalendar eventdate = event.getEiEvent().getEiActivePeriod().getProperties().getDtstart()
				.getDateTime().getValue().toGregorianCalendar();

		Duration d = Duration.parse(durationString);

		eventdate.add(Calendar.SECOND, (int) d.getSeconds());

		int dateCompare = eventdate.compareTo(now);
		if ( dateCompare > 0 ) {
			//event still has some time to go
			return true;
		} else {
			//event has already been completed
			return false;
		}

	}

}
