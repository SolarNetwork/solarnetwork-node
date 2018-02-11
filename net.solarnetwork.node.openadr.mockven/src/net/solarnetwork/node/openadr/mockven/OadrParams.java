
package net.solarnetwork.node.openadr.mockven;

import openadr.model.v20b.OadrTransportType;

public abstract class OadrParams {

	private String venName;

	//not sure about these constants
	private String venID;
	private String registrationID;
	private String requestID;

	private String vtnID;
	private int modificationNumber;
	private String eventID;

	private OadrTransportType transportType;
	private String profileName;
	private Boolean reportOnly;
	private Boolean xmlSignature;
	private Boolean httpPullModel;

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
