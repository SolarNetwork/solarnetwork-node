/* ==================================================================
 * PrecipitationType.java - 10/07/2023 7:56:32 am
 * 
 * Copyright 2023 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.hw.sunspec.environmental;

import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import net.solarnetwork.domain.CodedValue;

/**
 * Precipitation types, from WMO 4680 SYNOP code reference.
 * 
 * @author matt
 * @version 1.0
 */
public enum PrecipitationType implements CodedValue {

	/** Clear. */
	Clear(0),

	/** Haze or smoke. */
	Haze(4),

	/** Haze or smoke, thick. */
	HazeThick(5),

	/** Mist. */
	Mist(10),

	/** Recent fog. */
	RecentFog(20),

	/** Recent precipitation. */
	RecentPrecipitation(21),

	/** Recent drizzle. */
	RecentDrizzle(22),

	/** Recent rain. */
	RecentRain(23),

	/** Recent snow. */
	RecentSnow(24),

	/** Recent freezing rain. */
	RecentFreezingRain(25),

	/** Fog. */
	Fog(30),

	/** Patchy fog. */
	PatchyFog(31),

	/** Decreasing fog. */
	DecreasingFog(32),

	/** Increasing fog. */
	SteadyFog(33),

	/** Increasing fog. */
	IncreasingFog(34),

	/** Precipitation. */
	Precipitation(40),

	/** Precipitation, light. */
	LightPrecipitation(41),

	/** Precipitation, heavy. */
	HeavyPrecipitation(42),

	/** Drizzle. */
	Drizzle(50),

	/** Drizzle, light. */
	LightDrizzle(51),

	/** Drizzle, moderate. */
	ModerateDrizzle(52),

	/** Drizzle, heavy. */
	HeavyDrizzle(53),

	/** Freezing drizzle, heavy. */
	SlightFreezingDrizzle(54),

	/** Freezing drizzle, heavy. */
	ModerateFreezingDrizzle(55),

	/** Freezing drizzle, heavy. */
	HeavyFreezingDrizzle(56),

	/** Rain. */
	Rain(60),

	/** Rain, light. */
	LightRain(61),

	/** Rain, moderate. */
	ModerateRain(62),

	/** Rain, heavy. */
	HeavyRain(63),

	/** Freezing rain, light. */
	FreezingLightRain(64),

	/** Freezing rain, moderate. */
	FreezingModerateRain(65),

	/** Freezing rain, heavy. */
	FreezingHeavyRain(66),

	/** Rain and snow, light. */
	LightRainAndSnow(67),

	/** Rain (or drizzle) and snow, moderate or heavy. */
	RainAndSnow(68),

	/** Snow. */
	Snow(70),

	/** Snow, light. */
	LightSnow(71),

	/** Snow, moderate. */
	ModerateSnow(72),

	/** Snow, heavy. */
	HeavySnow(73),

	/** Hail, light. */
	LightHail(74),

	/** Hail, moderate. */
	ModerateHail(75),

	/** Hail, heavy. */
	HeavyHail(76),

	/** Showers or intermittent precipitation. */
	Showers(80),

	/** Rain showers, light. */
	LightShowers(81),

	/** Rain showers, moderate. */
	ModerateShowers(82),

	/** Rain showers, heavy. */
	HeavyShowers(83),

	/** Rain showers, violent (>32 mm/h). */
	ViolentShowers(84),

	/** Snow showers, light. */
	LightShowShowers(85),

	/** Snow showers, moderate. */
	ModerateSnowShowers(86),

	/** Snow showers, heavy. */
	HeavySnowShowers(87),

	;

	private final int code;

	private PrecipitationType(int code) {
		this.code = code;
	}

	@Override
	public int getCode() {
		return code;
	}

	/**
	 * Get a description.
	 * 
	 * @return the description, in the default locale
	 */
	public String getDescription() {
		return getDescription(Locale.getDefault());
	}

	/**
	 * Get a description.
	 * 
	 * @param locale
	 *        the desired locale
	 * @return the description
	 */
	public String getDescription(Locale locale) {
		try {
			ResourceBundle b = ResourceBundle.getBundle(PrecipitationType.class.getName(), locale,
					PrecipitationType.class.getClassLoader());
			return b.getString(String.valueOf(code));
		} catch ( MissingResourceException e ) {
			return this.name();
		}
	}

}
