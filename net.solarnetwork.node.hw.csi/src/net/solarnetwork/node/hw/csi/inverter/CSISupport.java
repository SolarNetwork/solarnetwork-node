package net.solarnetwork.node.hw.csi.inverter;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.solarnetwork.node.io.modbus.ModbusConnection;
import net.solarnetwork.node.io.modbus.ModbusDeviceDatumDataSourceSupport;
import net.solarnetwork.node.settings.SettingSpecifier;
import net.solarnetwork.node.settings.support.BasicTextFieldSettingSpecifier;

/**
 * @author maxieduncan
 * @version 1.0
 */
public class CSISupport extends ModbusDeviceDatumDataSourceSupport {
	private CSIData sample = new SI60KTLCTData();

	
	public CSIData getSample() {
		return sample;
	}

	@Override
	protected Map<String, Object> readDeviceInfo(ModbusConnection conn) {
		sample.readInverterData(conn);
		return null;
	}	
	
	public List<SettingSpecifier> getSettingSpecifiers() {
		CSISupport defaults = new CSISupport();
		List<SettingSpecifier> results = new ArrayList<SettingSpecifier>(10);

		results.add(new BasicTextFieldSettingSpecifier("uid", defaults.getUid()));
		results.add(new BasicTextFieldSettingSpecifier("groupUID", defaults.getGroupUID()));
		results.add(new BasicTextFieldSettingSpecifier("modbusNetwork.propertyFilters['UID']",
				"Modbus Port"));
		results.add(new BasicTextFieldSettingSpecifier("unitId", String.valueOf(defaults.getUnitId())));

		return results;
	}

}
