/* ==================================================================
 * EsiProgram.java - 7/08/2019 2:45:59 pm
 *
 * Copyright 2019 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.control.esi;

import static net.solarnetwork.service.OptionalService.service;
import static net.solarnetwork.service.OptionalServiceCollection.services;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.solarnetwork.esi.domain.DerProgramType;
import net.solarnetwork.node.control.esi.domain.PriceMapAccessor;
import net.solarnetwork.node.control.esi.domain.ResourceAccessor;
import net.solarnetwork.service.OptionalService.OptionalFilterableService;
import net.solarnetwork.service.OptionalServiceCollection.OptionalFilterableServiceCollection;
import net.solarnetwork.settings.SettingSpecifier;
import net.solarnetwork.settings.support.BasicMultiValueSettingSpecifier;
import net.solarnetwork.settings.support.BasicTextFieldSettingSpecifier;
import net.solarnetwork.settings.support.BasicTitleSettingSpecifier;

/**
 * General settings for ESI program integration.
 *
 * @author matt
 * @version 2.0
 */
public class EsiProgram extends BaseEsiMetadataComponent {

	/** The node property metadata key used for all ESI setting metadata. */
	public static final String ESI_PROGRAM_METADATA_KEY = "esi-program";

	private DerProgramType programType;
	private OptionalFilterableService<ResourceAccessor> resource;
	private OptionalFilterableServiceCollection<PriceMapAccessor> priceMaps;

	/**
	 * Constructor.
	 */
	public EsiProgram() {
		super(ESI_PROGRAM_METADATA_KEY);
	}

	@Override
	public String getSettingUid() {
		return "net.solarnetwork.node.control.esi.program";
	}

	@Override
	public String getDisplayName() {
		return "ESI Program Settings";
	}

	@Override
	public List<SettingSpecifier> getSettingSpecifiers() {
		List<SettingSpecifier> results = super.getSettingSpecifiers();

		// drop-down menu for program type
		BasicMultiValueSettingSpecifier programTypeSpec = new BasicMultiValueSettingSpecifier(
				"programTypeNumber", "");
		Map<String, String> programTypeTitles = new LinkedHashMap<String, String>(
				DerProgramType.values().length + 1);
		programTypeTitles.put("-1", "");
		for ( DerProgramType e : DerProgramType.values() ) {
			if ( e == DerProgramType.UNRECOGNIZED ) {
				continue;
			}
			programTypeTitles.put(String.valueOf(e.getNumber()),
					getMessageSource().getMessage("programType." + e.name(), null, Locale.getDefault()));
		}
		programTypeSpec.setValueTitles(programTypeTitles);
		results.add(programTypeSpec);

		results.add(new BasicTextFieldSettingSpecifier("resource.propertyFilters['uid']", "Main", false,
				"(objectClass=net.solarnetwork.node.control.esi.domain.ResourceAccessor)"));
		results.add(new BasicTitleSettingSpecifier("resourceStatus",
				resourceStatusMessage(Locale.getDefault()), true));

		results.add(new BasicTextFieldSettingSpecifier("priceMaps.propertyFilters['uid']", null, false,
				"(objectClass=net.solarnetwork.node.control.esi.domain.PriceMapAccessor)"));
		results.add(new BasicTextFieldSettingSpecifier("priceMaps.propertyFilters['groupUid']", "Main"));
		results.add(new BasicTitleSettingSpecifier("priceMapsStatus",
				priceMapsStatusMessage(Locale.getDefault()), true));

		return results;
	}

	@Override
	protected Map<String, Object> getEsiComponentMetadata() {
		Map<String, Object> result = new LinkedHashMap<>(4);

		result.put("programTypeNumber", getProgramTypeNumber());

		final Object uid = resource.getPropertyFilters().get("uid");
		result.put("resourceId", uid != null ? uid.toString() : "");

		final Object pmUid = priceMaps.getPropertyFilters().get("uid");
		final Object pmGuid = priceMaps.getPropertyFilters().get("groupUid");
		result.put("priceMapId", pmUid != null ? pmUid.toString() : "");
		result.put("priceMapGroupUid", pmGuid != null ? pmGuid.toString() : "");

		return result;
	}

	@Override
	protected String getConfigurationErrorStatusMessage(Locale locale) {
		String result = super.getConfigurationErrorStatusMessage(locale);
		if ( result != null ) {
			return result;
		}

		final DerProgramType pt = getProgramType();
		if ( pt == null || pt == DerProgramType.UNRECOGNIZED ) {
			return getMessageSource().getMessage("status.error.noProgramType", null, locale);
		}

		final ResourceAccessor rsrc = service(resource);
		if ( rsrc == null ) {
			return getMessageSource().getMessage("status.error.noResource", null, locale);
		}

		final Iterable<PriceMapAccessor> pms = services(priceMaps);
		if ( pms == null || !pms.iterator().hasNext() ) {
			return getMessageSource().getMessage("status.error.noPriceMap", null, locale);
		}

		return null;
	}

	private String resourceStatusMessage(Locale locale) {
		ResourceAccessor r = service(resource);
		return (r != null ? r.getStatusMessage(locale)
				: getMessageSource().getMessage("status.error.noResource", null, locale));
	}

	private String priceMapsStatusMessage(Locale locale) {
		StringBuilder buf = new StringBuilder();
		Iterable<PriceMapAccessor> pms = services(priceMaps);
		if ( pms != null ) {
			for ( PriceMapAccessor p : pms ) {
				if ( buf.length() > 0 ) {
					buf.append(" ");
				}
				String uid = p.getUid();
				String guid = p.getGroupUid();
				if ( uid != null && !uid.isEmpty() ) {
					buf.append(uid).append(": ");
				} else if ( guid != null && !guid.isEmpty() ) {
					buf.append(getMessageSource().getMessage("priceMaps.groupUid.title",
							new Object[] { guid }, locale)).append(": ");
				}
				buf.append(p.getStatusMessage(locale));
			}
		}
		return (buf.length() > 0 ? buf.toString()
				: getMessageSource().getMessage("status.error.noPriceMap", null, locale));
	}

	/**
	 * Get the DER program to participate in.
	 *
	 * @return the DER program
	 */
	public DerProgramType getProgramType() {
		return programType;
	}

	/**
	 * Set the DER program to participate in.
	 *
	 * @param programType
	 *        the DER program
	 */
	public void setProgramType(DerProgramType programType) {
		this.programType = programType;
	}

	/**
	 * Get the program type as a number.
	 *
	 * @return the program type number, as in {@link DerProgramType#getNumber()}
	 */
	public int getProgramTypeNumber() {
		DerProgramType p = getProgramType();
		return (p != null ? p : DerProgramType.UNRECOGNIZED).getNumber();
	}

	/**
	 * Set the program type as a number.
	 *
	 * @param programType
	 *        the program type to set, as in {@link DerProgramType#getNumber()}
	 */
	public void setProgramTypeNumber(int programType) {
		setProgramType(DerProgramType.forNumber(programType));
	}

	/**
	 * Get the resource to manage.
	 *
	 * @return the resource
	 */
	public OptionalFilterableService<ResourceAccessor> getResource() {
		return resource;
	}

	/**
	 * Set the resource to manage.
	 *
	 * @param resource
	 *        the resource
	 */
	public void setResource(OptionalFilterableService<ResourceAccessor> resource) {
		this.resource = resource;
	}

	/**
	 * Get the price maps to support.
	 *
	 * @return the price maps
	 */
	public OptionalFilterableServiceCollection<PriceMapAccessor> getPriceMaps() {
		return priceMaps;
	}

	/**
	 * Set the price maps to support.
	 *
	 * @param priceMaps
	 *        the price maps
	 */
	public void setPriceMaps(OptionalFilterableServiceCollection<PriceMapAccessor> priceMaps) {
		this.priceMaps = priceMaps;
	}

}
