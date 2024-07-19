/* ==================================================================
 * MarketPrice.java - 16/07/2024 2:30:35 pm
 *
 * Copyright 2024 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.datum.price.nz.wits;

import java.math.BigDecimal;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import net.solarnetwork.util.DateUtils;
import net.solarnetwork.util.NumberUtils;

/**
 * WITS market price DTO.
 *
 * @author matt
 * @version 1.0
 */
public class MarketPrice {

	/** The default time zone. */
	public static final ZoneId DEFAULT_TIME_ZONE = ZoneId.of("Pacific/Auckland");

	private final String schedule;
	private final String runType;
	private final ZonedDateTime tradingDateTime;
	private final int tradingPeriod;
	private final MarketType marketType;
	private final String node;
	private final BigDecimal price;
	private final ZonedDateTime lastRunTime;

	/**
	 * Constructor.
	 *
	 * @param schedule
	 *        the schedule
	 * @param runType
	 *        the run time
	 * @param tradingDateTime
	 *        the trading date time
	 * @param tradingPeriod
	 *        the trading period
	 * @param marketType
	 *        the market type
	 * @param node
	 *        the node
	 * @param price
	 *        the price
	 * @param lastRunTime
	 *        the last run time
	 */
	@JsonCreator
	public MarketPrice(@JsonProperty("schedule") String schedule,
			@JsonProperty("runType") String runType,
			@JsonProperty("tradingDateTime") String tradingDateTime,
			@JsonProperty("tradingPeriod") int tradingPeriod,
			@JsonProperty("marketType") String marketType, @JsonProperty("node") String node,
			@JsonProperty("price") Number price, @JsonProperty("lastRunTime") String lastRunTime) {
		super();
		this.schedule = schedule;
		this.runType = runType;
		this.tradingDateTime = DateUtils.parseIsoTimestamp(tradingDateTime, DEFAULT_TIME_ZONE);
		this.tradingPeriod = tradingPeriod;
		this.marketType = MarketType.forKey(marketType);
		this.node = node;
		this.price = NumberUtils.bigDecimalForNumber(price);
		this.lastRunTime = DateUtils.parseIsoTimestamp(lastRunTime, DEFAULT_TIME_ZONE);
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("MarketPrice{tradingDateTime=");
		builder.append(tradingDateTime);
		builder.append(", tradingPeriod=");
		builder.append(tradingPeriod);
		builder.append(", marketType=");
		builder.append(marketType);
		builder.append(", node=");
		builder.append(node);
		builder.append(", price=");
		builder.append(price);
		builder.append("}");
		return builder.toString();
	}

	/**
	 * Get the schedule.
	 *
	 * @return the schedule
	 */
	public final String getSchedule() {
		return schedule;
	}

	/**
	 * Get the run type.
	 *
	 * @return the runType
	 */
	public final String getRunType() {
		return runType;
	}

	/**
	 * Get the trading time.
	 *
	 * @return the trading time
	 */
	public final ZonedDateTime getTradingDateTime() {
		return tradingDateTime;
	}

	/**
	 * Get the trading period
	 *
	 * @return the trading period
	 */
	public final int getTradingPeriod() {
		return tradingPeriod;
	}

	/**
	 * Get the market type.
	 *
	 * @return the marketType
	 */
	public final MarketType getMarketType() {
		return marketType;
	}

	/**
	 * Get the node.
	 *
	 * @return the node
	 */
	public final String getNode() {
		return node;
	}

	/**
	 * Get the price.
	 *
	 * @return the price
	 */
	public final BigDecimal getPrice() {
		return price;
	}

	/**
	 * Get the last run time.
	 *
	 * @return the time
	 */
	public final ZonedDateTime getLastRunTime() {
		return lastRunTime;
	}

}
