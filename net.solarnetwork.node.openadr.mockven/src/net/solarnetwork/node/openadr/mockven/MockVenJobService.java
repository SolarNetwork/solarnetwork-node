
package net.solarnetwork.node.openadr.mockven;

import java.util.List;
import net.solarnetwork.node.job.JobService;
import net.solarnetwork.node.settings.SettingSpecifier;
import net.solarnetwork.node.settings.SettingSpecifierProvider;
import net.solarnetwork.node.settings.support.BasicTextFieldSettingSpecifier;
import net.solarnetwork.node.support.DatumDataSourceSupport;

/**
 * 
 * Setting specifier for the MockVEN it also implements JobService to allow
 * periodic polling of the VTN
 * 
 * @author robert
 * @version 1.0
 */
public class MockVenJobService extends DatumDataSourceSupport
		implements JobService, SettingSpecifierProvider {

	private String venName;
	private String vtnAddress;
	private String vtnName;
	private MockVen mockVen = null;

	@Override
	public String getSettingUID() {
		return "net.solarnetwork.node.openadr.mockven";
	}

	@Override
	public String getDisplayName() {
		return "OpenADR Ven Mock";
	}

	@Override
	public List<SettingSpecifier> getSettingSpecifiers() {
		List<SettingSpecifier> results = getIdentifiableSettingSpecifiers();
		//default names used by EPRI's VEN and VTN
		//see results.add(new BasicTextFieldSettingSpecifier("vtnName", "EPRI"));
		//results.add(new BasicTextFieldSettingSpecifier("vtnName", "EPRI"));
		results.add(new BasicTextFieldSettingSpecifier("venName", "Test_VEN_Name"));
		results.add(new BasicTextFieldSettingSpecifier("vtnName", "EPRI"));

		results.add(new BasicTextFieldSettingSpecifier("vtnAddress", null));
		return results;
	}

	public String getVenName() {
		return venName;
	}

	public void setVenName(String venName) {
		this.venName = venName;
	}

	public String getVtnAddress() {
		return vtnAddress;
	}

	public void setVtnAddress(String vtnAddress) {
		this.vtnAddress = vtnAddress;
	}

	public String getVtnName() {
		return vtnName;
	}

	public void setVtnName(String vtnName) {
		this.vtnName = vtnName;
	}

	private void runMockVen() {
		//create a MockVen instance when we don't have one
		if ( mockVen == null ) {
			mockVen = new MockVen();
		}
		//put the settings into the mock ven
		mockVen.setVtnURL(vtnAddress);
		mockVen.setVenName(venName);

		//the poll and respond method will register to the VTN if it is not already registered
		//otherwise it will send poll request to the VTN and respond to any events
		mockVen.pollAndRespond();
	}

	@Override
	public void executeJobService() throws Exception {
		runMockVen();
	}

}
