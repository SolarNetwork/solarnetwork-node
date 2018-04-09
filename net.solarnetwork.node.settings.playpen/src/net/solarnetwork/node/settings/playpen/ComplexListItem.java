/* ==================================================================
 * ComplexListItem.java - 27/06/2015 7:34:40 am
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

package net.solarnetwork.node.settings.playpen;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.solarnetwork.node.settings.SettingSpecifier;
import net.solarnetwork.node.settings.support.BasicMultiValueSettingSpecifier;
import net.solarnetwork.node.settings.support.BasicTextFieldSettingSpecifier;
import net.solarnetwork.node.settings.support.BasicToggleSettingSpecifier;

/**
 * A data bean to demonstrate more complex dynamic list use.
 * 
 * @author matt
 * @version 1.1
 */
public class ComplexListItem {

	public enum Gender {
		Male,
		Female,
		Other;
	}

	private String firstName;
	private String lastName;
	private String phone;
	private Integer age;
	private Boolean enabled = Boolean.TRUE;
	private Gender gender = Gender.Other;

	public List<SettingSpecifier> settings(String prefix) {
		ComplexListItem defaults = new ComplexListItem();
		List<SettingSpecifier> results = new ArrayList<SettingSpecifier>();
		results.add(new BasicTextFieldSettingSpecifier(prefix + "firstName", defaults.firstName));
		results.add(new BasicTextFieldSettingSpecifier(prefix + "lastName", defaults.lastName));

		// drop-down menu
		BasicMultiValueSettingSpecifier menuSpec = new BasicMultiValueSettingSpecifier(
				prefix + "genderValue", defaults.gender.toString());
		Map<String, String> menuValues = new LinkedHashMap<String, String>(3);
		for ( Gender g : Gender.values() ) {
			menuValues.put(g.toString(), g.toString());
		}
		menuSpec.setValueTitles(menuValues);
		results.add(menuSpec);

		results.add(new BasicTextFieldSettingSpecifier(prefix + "phone", defaults.phone));
		results.add(new BasicTextFieldSettingSpecifier(prefix + "age",
				(defaults.age == null ? "" : defaults.age.toString())));
		results.add(new BasicToggleSettingSpecifier(prefix + "enabled", defaults.enabled));
		return results;
	}

	public String getFirstName() {
		return firstName;
	}

	public void setFirstName(String firstName) {
		this.firstName = firstName;
	}

	public String getLastName() {
		return lastName;
	}

	public void setLastName(String lastName) {
		this.lastName = lastName;
	}

	public String getPhone() {
		return phone;
	}

	public void setPhone(String phone) {
		this.phone = phone;
	}

	public Boolean getEnabled() {
		return enabled;
	}

	public void setEnabled(Boolean enabled) {
		this.enabled = enabled;
	}

	public Integer getAge() {
		return age;
	}

	public void setAge(Integer age) {
		this.age = age;
	}

	public Gender getGender() {
		return gender;
	}

	public void setGender(Gender gender) {
		this.gender = gender;
	}

	public String getGenderValue() {
		return gender.toString();
	}

	public void setGenderValue(String gender) {
		this.gender = Gender.valueOf(gender);
	}

}
