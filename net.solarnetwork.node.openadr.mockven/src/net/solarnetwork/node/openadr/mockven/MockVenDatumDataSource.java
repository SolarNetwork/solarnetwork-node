
package net.solarnetwork.node.openadr.mockven;

import java.util.List;
import net.solarnetwork.node.DatumDataSource;
import net.solarnetwork.node.domain.GeneralNodeDatum;
import net.solarnetwork.node.settings.SettingSpecifier;
import net.solarnetwork.node.settings.SettingSpecifierProvider;
import net.solarnetwork.node.settings.support.BasicTextFieldSettingSpecifier;
import net.solarnetwork.node.support.DatumDataSourceSupport;

public class MockVenDatumDataSource extends DatumDataSourceSupport
		implements DatumDataSource<GeneralNodeDatum>, SettingSpecifierProvider {

	private String venName;
	private String venID;
	private String vtnAddress;
	private String vtnName;
	private MockVen mockVen = null;

	@Override
	public Class<? extends GeneralNodeDatum> getDatumType() {
		return GeneralNodeDatum.class;
	}

	@Override
	public GeneralNodeDatum readCurrentDatum() {
		if ( mockVen == null ) {
			mockVen = new MockVen();
		}
		mockVen.pollAndRespond();

		// I don't understand how to configure in OSGI how to do a job so instead we will be using this to summon the MockVen
		return null;
	}

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
		results.add(new BasicTextFieldSettingSpecifier("venName", null));
		results.add(new BasicTextFieldSettingSpecifier("venID", null));
		results.add(new BasicTextFieldSettingSpecifier("vtnAddress", null));
		results.add(new BasicTextFieldSettingSpecifier("vtnName", null));
		return results;
	}

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

}
