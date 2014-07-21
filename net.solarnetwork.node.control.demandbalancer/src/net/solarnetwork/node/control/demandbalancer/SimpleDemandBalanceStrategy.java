/* ==================================================================
s * SimpleDemandBalanceStrategy.java - Mar 23, 2014 7:46:01 PM
 * 
 * Copyright 2007-2014 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.control.demandbalancer;

import java.util.Collections;
import java.util.List;
import net.solarnetwork.node.settings.SettingSpecifier;
import net.solarnetwork.node.settings.SettingSpecifierProvider;
import net.solarnetwork.node.settings.support.BasicTextFieldSettingSpecifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSource;

/**
 * Very basic implementation of {@link DemandBalanceStrategy} that simply
 * enforces a generation limit on the current demand.
 * 
 * <p>
 * The configurable properties of this class are:
 * </p>
 * 
 * <dl class="class-properties">
 * <dt>unknownDemandLimit</dt>
 * <dd>If {@bold -1} is passed as the {@code demandWatts} to
 * {@link #evaluateBalance(String, int, int, int, int)} then this value will be
 * returned. Set to {@bold -1} to do nothing.</dd>
 * </dl>
 * 
 * @author matt
 * @version 1.1
 */
public class SimpleDemandBalanceStrategy implements DemandBalanceStrategy, SettingSpecifierProvider {

	/** The UID for this strategy: {@code Simple}. */
	public static final String UID = "Simple";

	private final Logger log = LoggerFactory.getLogger(getClass());

	private MessageSource messageSource;
	private int unknownDemandLimit = -1;

	@Override
	public String getUID() {
		return UID;
	}

	@Override
	public int evaluateBalance(final String powerControlId, final int demandWatts,
			final int generationWatts, final int generationCapacityWatts, final int currentLimit) {
		int desiredLimit = currentLimit;
		if ( demandWatts < 0 ) {
			// unknown demand... set to unknownDemandLimit
			desiredLimit = unknownDemandLimit;
		} else if ( demandWatts < generationCapacityWatts ) {
			// demand is less than generation capacity... enforce limit
			desiredLimit = (int) Math.floor(100.0 * demandWatts / generationCapacityWatts);
			log.debug("Demand of {} is less than {} capacity of {}, limiting to {}%", demandWatts,
					powerControlId, generationCapacityWatts, desiredLimit);
		}
		return desiredLimit;
	}

	@Override
	public String getSettingUID() {
		return getClass().getName();
	}

	@Override
	public String getDisplayName() {
		return "Simple Demand Balance Strategy";
	}

	@Override
	public MessageSource getMessageSource() {
		return messageSource;
	}

	@Override
	public List<SettingSpecifier> getSettingSpecifiers() {
		SimpleDemandBalanceStrategy defaults = new SimpleDemandBalanceStrategy();
		return Collections.singletonList((SettingSpecifier) new BasicTextFieldSettingSpecifier(
				"unknownDemandLimit", String.valueOf(defaults.getUnknownDemandLimit())));
	}

	public int getUnknownDemandLimit() {
		return unknownDemandLimit;
	}

	public void setUnknownDemandLimit(int unknownDemandLimit) {
		this.unknownDemandLimit = unknownDemandLimit;
	}

	public void setMessageSource(MessageSource messageSource) {
		this.messageSource = messageSource;
	}

}
