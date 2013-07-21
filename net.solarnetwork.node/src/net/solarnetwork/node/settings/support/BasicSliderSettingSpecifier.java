/* ==================================================================
 * BasicSliderSettingSpecifier.java - Mar 12, 2012 10:07:22 AM
 * 
 * Copyright 2007-2012 SolarNetwork.net Dev Team
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
 * $Id$
 * ==================================================================
 */

package net.solarnetwork.node.settings.support;

import net.solarnetwork.node.settings.SettingSpecifier;
import net.solarnetwork.node.settings.SliderSettingSpecifier;

/**
 * Basic implementation of {@link SliderSettingSpecifier}.
 * 
 * @author matt
 * @version $Revision$
 */
public class BasicSliderSettingSpecifier extends BaseKeyedSettingSpecifier<Double> implements
		SliderSettingSpecifier {

	private Double minimumValue = Double.valueOf(0.0);
	private Double maximumValue = Double.valueOf(1.0);
	private Double step = Double.valueOf(1);

	/**
	 * Construct with values.
	 * 
	 * @param key
	 *        the key
	 * @param defaultValue
	 *        the default value
	 * @param minValue
	 *        the minimum value
	 * @param maxValue
	 *        the maximum value
	 * @param step
	 *        the step value
	 */
	public BasicSliderSettingSpecifier(String key, Double defaultValue, Double minValue,
			Double maxValue, Double step) {
		super(key, defaultValue);
		setMaximumValue(maxValue);
		setMinimumValue(minValue);
		setStep(step);
	}

	@Override
	public Double getMinimumValue() {
		return this.minimumValue;
	}

	@Override
	public Double getMaximumValue() {
		return this.maximumValue;
	}

	@Override
	public Double getStep() {
		return step;
	}

	@Override
	public SettingSpecifier mappedWithPlaceholer(String template) {
		BasicSliderSettingSpecifier spec = new BasicSliderSettingSpecifier(String.format(template,
				getKey()), getDefaultValue(), minimumValue, maximumValue, step);
		spec.setTitle(getTitle());
		return spec;
	}

	public void setMinimumValue(Double minimumValue) {
		this.minimumValue = minimumValue;
	}

	public void setMaximumValue(Double maximumValue) {
		this.maximumValue = maximumValue;
	}

	public void setStep(Double step) {
		this.step = step;
	}

}
