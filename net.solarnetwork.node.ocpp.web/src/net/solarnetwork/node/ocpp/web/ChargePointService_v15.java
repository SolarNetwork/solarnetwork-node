/* ==================================================================
 * ChargePointService_v15.java - 13/06/2015 5:17:45 pm
 * 
 * Copyright 2007-2015 SolarNetwork.net Dev Team
 * 
 * This program is free software; you can redistribute it and/or 
 * modify it under the terms of the GNU General Public License as 
 * published by the Free Software Foundation; either version 2 of 
 * the License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful, 
 * but WITHOUT ANY WARRANTY; without even the implied warranty of 
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU 
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License 
 * along with this program; if not, write to the Free Software 
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 
 * 02111-1307 USA
 * ==================================================================
 */

package net.solarnetwork.node.ocpp.web;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import javax.jws.WebService;
import net.solarnetwork.node.ocpp.ChargeSessionManager;
import net.solarnetwork.node.settings.SettingSpecifier;
import net.solarnetwork.node.settings.SettingSpecifierProvider;
import net.solarnetwork.node.settings.support.BasicTextFieldSettingSpecifier;
import net.solarnetwork.node.settings.support.BasicTitleSettingSpecifier;
import net.solarnetwork.util.FilterableService;
import ocpp.v15.cp.AvailabilityStatus;
import ocpp.v15.cp.AvailabilityType;
import ocpp.v15.cp.CancelReservationRequest;
import ocpp.v15.cp.CancelReservationResponse;
import ocpp.v15.cp.ChangeAvailabilityRequest;
import ocpp.v15.cp.ChangeAvailabilityResponse;
import ocpp.v15.cp.ChangeConfigurationRequest;
import ocpp.v15.cp.ChangeConfigurationResponse;
import ocpp.v15.cp.ChargePointService;
import ocpp.v15.cp.ClearCacheRequest;
import ocpp.v15.cp.ClearCacheResponse;
import ocpp.v15.cp.DataTransferRequest;
import ocpp.v15.cp.DataTransferResponse;
import ocpp.v15.cp.GetConfigurationRequest;
import ocpp.v15.cp.GetConfigurationResponse;
import ocpp.v15.cp.GetDiagnosticsRequest;
import ocpp.v15.cp.GetDiagnosticsResponse;
import ocpp.v15.cp.GetLocalListVersionRequest;
import ocpp.v15.cp.GetLocalListVersionResponse;
import ocpp.v15.cp.RemoteStartTransactionRequest;
import ocpp.v15.cp.RemoteStartTransactionResponse;
import ocpp.v15.cp.RemoteStopTransactionRequest;
import ocpp.v15.cp.RemoteStopTransactionResponse;
import ocpp.v15.cp.ReserveNowRequest;
import ocpp.v15.cp.ReserveNowResponse;
import ocpp.v15.cp.ResetRequest;
import ocpp.v15.cp.ResetResponse;
import ocpp.v15.cp.SendLocalListRequest;
import ocpp.v15.cp.SendLocalListResponse;
import ocpp.v15.cp.UnlockConnectorRequest;
import ocpp.v15.cp.UnlockConnectorResponse;
import ocpp.v15.cp.UnlockStatus;
import ocpp.v15.cp.UpdateFirmwareRequest;
import ocpp.v15.cp.UpdateFirmwareResponse;
import org.springframework.context.MessageSource;

/**
 * SolarNode implementation of {@link ChargePointService}
 * 
 * @author matt
 * @version 1.0
 */
@WebService(serviceName = "ChargePointService", targetNamespace = "urn://Ocpp/Cp/2012/06/")
public class ChargePointService_v15 implements ChargePointService, SettingSpecifierProvider {

	private ChargeSessionManager chargeSessionManager;

	private MessageSource messageSource;

	@Override
	public UnlockConnectorResponse unlockConnector(UnlockConnectorRequest parameters,
			String chargeBoxIdentity) {
		final Integer connId = parameters.getConnectorId();
		final String socketId = chargeSessionManager.socketIdForConnectorId(connId);
		final UnlockConnectorResponse resp = new UnlockConnectorResponse();
		if ( socketId == null ) {
			resp.setStatus(UnlockStatus.REJECTED);
		} else {
			chargeSessionManager.configureSocketEnabledState(Collections.singleton(socketId), true);
			resp.setStatus(UnlockStatus.ACCEPTED);
		}
		return resp;
	}

	@Override
	public ResetResponse reset(ResetRequest parameters, String chargeBoxIdentity) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ChangeAvailabilityResponse changeAvailability(ChangeAvailabilityRequest parameters,
			String chargeBoxIdentity) {
		final Integer connId = parameters.getConnectorId();
		final Collection<String> socketIds;
		final ChangeAvailabilityResponse resp = new ChangeAvailabilityResponse();
		if ( Integer.valueOf(0).equals(connId) ) {
			// this means ALL connectors
			socketIds = chargeSessionManager.availableSocketIds();
		} else {
			String socketId = chargeSessionManager.socketIdForConnectorId(connId);
			if ( socketId == null ) {
				socketIds = Collections.emptySet();
			} else {
				socketIds = Collections.singleton(socketId);
			}
		}
		if ( socketIds.isEmpty() ) {
			resp.setStatus(AvailabilityStatus.REJECTED);
		} else {
			chargeSessionManager.configureSocketEnabledState(socketIds,
					AvailabilityType.OPERATIVE.equals(parameters.getType()));
			resp.setStatus(AvailabilityStatus.ACCEPTED);
		}
		return resp;
	}

	@Override
	public GetDiagnosticsResponse getDiagnostics(GetDiagnosticsRequest parameters,
			String chargeBoxIdentity) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ClearCacheResponse clearCache(ClearCacheRequest parameters, String chargeBoxIdentity) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public UpdateFirmwareResponse updateFirmware(UpdateFirmwareRequest parameters,
			String chargeBoxIdentity) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ChangeConfigurationResponse changeConfiguration(ChangeConfigurationRequest parameters,
			String chargeBoxIdentity) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public RemoteStartTransactionResponse remoteStartTransaction(
			RemoteStartTransactionRequest parameters, String chargeBoxIdentity) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public RemoteStopTransactionResponse remoteStopTransaction(RemoteStopTransactionRequest parameters,
			String chargeBoxIdentity) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public CancelReservationResponse cancelReservation(CancelReservationRequest parameters,
			String chargeBoxIdentity) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public DataTransferResponse dataTransfer(DataTransferRequest parameters, String chargeBoxIdentity) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public GetConfigurationResponse getConfiguration(GetConfigurationRequest parameters,
			String chargeBoxIdentity) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public GetLocalListVersionResponse getLocalListVersion(GetLocalListVersionRequest parameters,
			String chargeBoxIdentity) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ReserveNowResponse reserveNow(ReserveNowRequest parameters, String chargeBoxIdentity) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public SendLocalListResponse sendLocalList(SendLocalListRequest parameters, String chargeBoxIdentity) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getSettingUID() {
		return "net.solarnetwork.node.ocpp.web.chargepoint";
	}

	@Override
	public String getDisplayName() {
		return getClass().getSimpleName();
	}

	@Override
	public MessageSource getMessageSource() {
		return messageSource;
	}

	@Override
	public List<SettingSpecifier> getSettingSpecifiers() {
		//ChargePointService_v15 defaults = new ChargePointService_v15();
		List<SettingSpecifier> results = new ArrayList<SettingSpecifier>(4);
		results.add(new BasicTitleSettingSpecifier("info", getInfoMessage(Locale.getDefault()), true));
		results.add(new BasicTextFieldSettingSpecifier(
				"filterableChargeSessionManager.propertyFilters['UID']", "OCPP Central System"));
		return results;
	}

	private String getInfoMessage(Locale locale) {
		return ""; // TODO
	}

	public ChargeSessionManager getChargeSessionManager() {
		return chargeSessionManager;
	}

	public void setChargeSessionManager(ChargeSessionManager chargeSessionManager) {
		this.chargeSessionManager = chargeSessionManager;
	}

	public FilterableService getFilterableChargeSessionManager() {
		ChargeSessionManager mgr = chargeSessionManager;
		if ( mgr instanceof FilterableService ) {
			return (FilterableService) mgr;
		}
		return null;
	}

	public void setMessageSource(MessageSource messageSource) {
		this.messageSource = messageSource;
	}

}
