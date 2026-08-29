
package net.solarnetwork.node.openadr.mockven;

import openadr.model.v20b.OadrTransportType;

/**
 * This abstract class just has a bunch of getters and setters of parameters
 * that often go into OadrPayloads. These are used in the generators of the
 * various payloads.
 * 
 * @author robert
 * @version 1.0
 */
public class OadrParams {

	private String venName;

	private String venID;
	private String registrationID;
	private String requestID;

	private String vtnID;
	private int modificationNumber;
	private String eventID;

	//default values I copied from the EPRI VEN 
	private OadrTransportType transportType = OadrTransportType.SIMPLE_HTTP;
	private String profileName = "2.0b";
	private Boolean reportOnly = false;
	private Boolean xmlSignature = false;
	private Boolean httpPullModel = true;

	public String getVenName() {
		return venName;
	}

	public void setVenName(String venName) {
		this.venName = venName;
	}

	public String getVenID() {
		return venID;
	}

	public void setVenID(String venID) {
		this.venID = venID;
	}

	public String getRegistrationID() {
		return registrationID;
	}

	public void setRegistrationID(String registrationID) {
		this.registrationID = registrationID;
	}

	public String getRequestID() {
		return requestID;
	}

	public void setRequestID(String requestID) {
		this.requestID = requestID;
	}

	public OadrTransportType getTransportName() {
		return transportType;
	}

	public void setTransportName(OadrTransportType transportName) {
		this.transportType = transportName;
	}

	public String getProfileName() {
		return profileName;
	}

	public void setProfileName(String profileName) {
		this.profileName = profileName;
	}

	public Boolean isReportOnly() {
		return reportOnly;
	}

	public void setReportOnly(boolean reportOnly) {
		this.reportOnly = reportOnly;
	}

	public Boolean isXmlSignature() {
		return xmlSignature;
	}

	public void setXmlSignature(boolean xmlSignature) {
		this.xmlSignature = xmlSignature;
	}

	public Boolean isHttpPullModel() {
		return httpPullModel;
	}

	public void setHttpPullModel(boolean httpPullModel) {
		this.httpPullModel = httpPullModel;
	}

	public String getVtnID() {
		return vtnID;
	}

	public void setVtnID(String vtnID) {
		this.vtnID = vtnID;
	}

	public int getModificationNumber() {
		return modificationNumber;
	}

	public void setModificationNumber(int modificationNumber) {
		this.modificationNumber = modificationNumber;
	}

	public String getEventID() {
		return eventID;
	}

	public void setEventID(String eventID) {
		this.eventID = eventID;
	}
}
