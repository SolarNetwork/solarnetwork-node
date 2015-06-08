/* ==================================================================
 * DefaultAuthorizationManager.java - 8/06/2015 7:11:10 am
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

package net.solarnetwork.node.ocpp.auth;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.solarnetwork.node.ocpp.Authorization;
import net.solarnetwork.node.ocpp.AuthorizationDao;
import net.solarnetwork.node.ocpp.AuthorizationManager;
import net.solarnetwork.node.ocpp.CentralSystemServiceFactory;
import net.solarnetwork.node.settings.SettingSpecifier;
import net.solarnetwork.node.settings.SettingSpecifierProvider;
import net.solarnetwork.node.settings.support.BasicTextFieldSettingSpecifier;
import net.solarnetwork.node.settings.support.BasicTitleSettingSpecifier;
import net.solarnetwork.util.FilterableService;
import net.solarnetwork.util.OptionalService;
import ocpp.v15.AuthorizationStatus;
import ocpp.v15.AuthorizeRequest;
import ocpp.v15.AuthorizeResponse;
import org.springframework.context.MessageSource;

/**
 * Default implementation of {@link AuthorizationManager}. This implementation
 * supports local caching of ID tags.
 * 
 * @author matt
 * @version 1.0
 */
public class DefaultAuthorizationManager implements AuthorizationManager, SettingSpecifierProvider {

	private CentralSystemServiceFactory centralSystem;
	private FilterableService filterableCentralSystem;
	private OptionalService<AuthorizationDao> authorizationDao;
	private MessageSource messageSource;

	@Override
	public boolean authorize(String idTag) {
		Authorization auth = authorizationForTag(idTag);
		if ( isAuthorized(auth) ) {
			return true;
		} else if ( isCachedAuthorizationValid(auth) ) {
			// no need to validate with central system
			return false;
		}
		AuthorizeRequest req = new AuthorizeRequest();
		req.setIdTag(idTag);
		AuthorizeResponse res = centralSystem.service()
				.authorize(req, centralSystem.chargeBoxIdentity());
		if ( res != null && res.getIdTagInfo() != null ) {
			auth = new Authorization(idTag, res.getIdTagInfo());
			saveAuthorization(auth);
			return auth.isAccepted();
		}
		return false;
	}

	/**
	 * Return <em>true</em> if {@code auth} has an {@code expiryDate} whose date
	 * is in the future.
	 * 
	 * @param auth
	 *        The authorization to check (mey be <em>null</em>).
	 * @return Cached validity flag.
	 */
	private boolean isCachedAuthorizationValid(Authorization auth) {
		return (auth != null && auth.getExpiryDate() != null && auth.getExpiryDate()
				.toGregorianCalendar().getTimeInMillis() > System.currentTimeMillis());
	}

	private boolean isAuthorized(Authorization auth) {
		return (auth != null && auth.isAccepted());
	}

	private Authorization authorizationForTag(String idTag) {
		AuthorizationDao dao = (authorizationDao != null ? authorizationDao.service() : null);
		if ( dao == null ) {
			return null;
		}
		return dao.getAuthorization(idTag);
	}

	private void saveAuthorization(Authorization auth) {
		AuthorizationDao dao = (authorizationDao != null ? authorizationDao.service() : null);
		if ( dao == null ) {
			return;
		}
		dao.storeAuthorization(auth);
	}

	// SettingSpecifierProvider

	@Override
	public String getSettingUID() {
		return "net.solarnetwork.node.ocpp.auth";
	}

	@Override
	public String getDisplayName() {
		return "OCPP Authorization Manager";
	}

	@Override
	public List<SettingSpecifier> getSettingSpecifiers() {
		List<SettingSpecifier> results = new ArrayList<SettingSpecifier>(3);
		results.add(new BasicTitleSettingSpecifier("info", getInfoMessage(), true));
		results.add(new BasicTextFieldSettingSpecifier("filterableCentralSystem.propertyFilters['UID']",
				"OCPP Central System"));
		return results;
	}

	private String getInfoMessage() {
		AuthorizationDao dao = (authorizationDao != null ? authorizationDao.service() : null);
		if ( dao == null ) {
			return messageSource.getMessage("status.noDao", null, Locale.getDefault());
		}
		Map<AuthorizationStatus, Integer> statusCounts = dao.statusCounts();
		if ( statusCounts.size() < 1 ) {
			return messageSource.getMessage("status.none", null, Locale.getDefault());
		}
		StringBuilder buf = new StringBuilder();
		for ( Map.Entry<AuthorizationStatus, Integer> me : statusCounts.entrySet() ) {
			if ( buf.length() > 0 ) {
				buf.append("; ");
			}
			buf.append(me.getKey()).append(": ").append(me.getValue());
		}
		return messageSource.getMessage("status.counts", new Object[] { buf.toString() },
				Locale.getDefault());
	}

	@Override
	public MessageSource getMessageSource() {
		return messageSource;
	}

	public void setMessageSource(MessageSource messageSource) {
		this.messageSource = messageSource;
	}

	public CentralSystemServiceFactory getCentralSystem() {
		return centralSystem;
	}

	public void setCentralSystem(CentralSystemServiceFactory centralSystem) {
		this.centralSystem = centralSystem;
		if ( centralSystem instanceof FilterableService ) {
			setFilterableCentralSystem((FilterableService) centralSystem);
		}
	}

	public OptionalService<AuthorizationDao> getAuthorizationDao() {
		return authorizationDao;
	}

	public void setAuthorizationDao(OptionalService<AuthorizationDao> authorizationDao) {
		this.authorizationDao = authorizationDao;
	}

	public FilterableService getFilterableCentralSystem() {
		return filterableCentralSystem;
	}

	public void setFilterableCentralSystem(FilterableService filterableCentralSystem) {
		this.filterableCentralSystem = filterableCentralSystem;
	}

}
