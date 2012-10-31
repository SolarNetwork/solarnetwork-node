/* ==================================================================
 * BasicToggleSettingSpecifier.java - Mar 12, 2012 10:09:37 AM
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
import net.solarnetwork.node.settings.ToggleSettingSpecifier;

/**
 * Basic implementation of {@link ToggleSettingSpecifier}.
 * 
 * @author matt
 * @version $Revision$
 */
public class BasicToggleSettingSpecifier extends BaseKeyedSettingSpecifier<Object> implements
		ToggleSettingSpecifier {

	private Object trueValue = Boolean.TRUE;
	private Object falseValue = Boolean.FALSE;

	/**
	 * Constructor.
	 * 
	 * @param key
	 *            the key
	 * @param defaultValue
	 *            the default value
	 */
	public BasicToggleSettingSpecifier(String key, Object defaultValue) {
		super(key, defaultValue);
	}

	/**
	 * Constructor.
	 * 
	 * @param key
	 *            the key
	 * @param defaultValue
	 *            the default value
	 * @param trans
	 *            the transient flag value
	 */
	public BasicToggleSettingSpecifier(String key, Object defaultValue, boolean trans) {
		super(key, defaultValue, trans);
	}
	
	@Override
	public Object getTrueValue() {
		return this.trueValue;
	}

	@Override
	public Object getFalseValue() {
		return this.falseValue;
	}

	@Override
	public SettingSpecifier mappedTo(String prefix) {
		BasicToggleSettingSpecifier spec = new BasicToggleSettingSpecifier(prefix + getKey(),
				getDefaultValue());
		spec.setTitle(getTitle());
		spec.setTrueValue(trueValue);
		spec.setFalseValue(falseValue);
		return spec;
	}

	public void setTrueValue(Object trueValue) {
		this.trueValue = trueValue;
	}

	public void setFalseValue(Object falseValue) {
		this.falseValue = falseValue;
	}

}
