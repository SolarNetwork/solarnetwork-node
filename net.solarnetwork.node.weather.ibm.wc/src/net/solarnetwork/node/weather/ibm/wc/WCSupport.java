
package net.solarnetwork.node.weather.ibm.wc;

import org.springframework.context.MessageSource;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.solarnetwork.node.DatumDataSource;
import net.solarnetwork.node.MultiDatumDataSource;
import net.solarnetwork.node.domain.Datum;
import net.solarnetwork.node.settings.SettingSpecifierProvider;

/**
 * Simplifies the datum data source classes by providing shared methods.
 * 
 * @author matt frost
 *
 * @param <T>
 */
public abstract class WCSupport<T extends Datum>
		implements SettingSpecifierProvider, DatumDataSource<T>, MultiDatumDataSource<T> {

	private String uid;
	private String groupUID;
	private String locationIdentifier;
	private WCClient client;
	private String apiKey;
	private String datumPeriod;
	private MessageSource messageSource;
	private String language;

	public WCSupport() {
		locationIdentifier = "";
		client = new BasicWCClient();
	}

	//TODO add a dropdown specifier for the period of the data (ie. 7day, 10day)

	public void setMessageSource(MessageSource messageSource) {
		this.messageSource = messageSource;
	}

	public String getUID() {
		return getUid();
	}

	public String getUid() {
		return uid;
	}

	public void setUid(String uid) {
		this.uid = uid;
	}

	public String getApiKey() {
		return this.apiKey;
	}

	public void setApiKey(String apiKey) {
		this.apiKey = apiKey;
	}

	public String getDatumPeriod() {
		return this.datumPeriod;
	}

	public void setDatumPeriod(String datumPeriod) {
		this.datumPeriod = datumPeriod;
	}

	/**
	 * Get a {@link MessageSource} for supporting message resolution.
	 * 
	 * @return the message source
	 */
	public MessageSource getMessageSource() {
		return messageSource;
	}

	public WCClient getClient() {
		return this.client;
	}

	public void setClient(WCClient client) {
		this.client = client;
	}

	public String getGroupUID() {
		return groupUID;
	}

	public void setGroupUID(String groupUID) {
		this.groupUID = groupUID;
	}

	public void setObjectMapper(ObjectMapper objectMapper) {
		if ( client instanceof BasicWCClient ) {
			((BasicWCClient) client).setObjectMapper(objectMapper);
		}
	}

	public String getLocationIdentifier() {
		return locationIdentifier;
	}

	public void setLocationIdentifier(String locationIdentifier) {
		this.locationIdentifier = locationIdentifier;
	}

	public String getLanguage() {
		return language;
	}

	public void setLanguage(String language) {
		this.language = language;
	}

}
