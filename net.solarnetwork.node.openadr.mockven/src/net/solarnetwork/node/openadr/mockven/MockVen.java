
package net.solarnetwork.node.openadr.mockven;

public class MockVen extends OadrParams {

	private boolean registered = false;
	private String url;
	private TopNodeConnection connection;

	public MockVen() {
		connection = new TopNodeConnection();
	}

	@Override
	public void setVenID(String venID) {
		if ( !getVenID().equals(venID) ) {
			registered = false;
		}
		super.setVenID(venID);
	}

	@Override
	public void setVenName(String venName) {
		if ( !getVenName().equals(venName) ) {
			registered = false;
		}
		super.setVenName(venName);
	}

	public boolean isRegisterd() {

		//this should query registration
		return registered;
	}

	public void setVtnURL(String url) {
		this.url = url;
	}

	//Polls the VTN and responds to events
	public void pollAndRespond() {
		if ( registered == false ) {
			queryAndRegister();
		}
	}

	//asks VTN if we are registered if not register
	public void queryAndRegister() {

	}

}
