
package net.solarnetwork.node.weather.ibm.wc;

import java.math.BigDecimal;
import net.solarnetwork.node.domain.GeneralAtmosphericDatum;

public class WCHourlyDatum extends GeneralAtmosphericDatum {

	public void setCloudCover(BigDecimal i) {
		this.putInstantaneousSampleValue("cloudCover", i);
	}

	public BigDecimal getCloudCover() {
		return this.getInstantaneousSampleBigDecimal("cloudCover");
	}

}
