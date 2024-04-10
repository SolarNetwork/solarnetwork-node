/* ==================================================================
 * VirtualMeterExpressionRootImpl.java - 9/05/2021 7:50:18 AM
 *
 * Copyright 2021 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.datum.filter.virt;

import java.math.BigDecimal;
import java.util.Map;
import net.solarnetwork.domain.datum.Datum;
import net.solarnetwork.domain.datum.DatumSamples;
import net.solarnetwork.node.domain.ExpressionRoot;
import net.solarnetwork.node.service.DatumService;
import net.solarnetwork.node.service.MetadataService;
import net.solarnetwork.service.ExpressionService;

/**
 * An object to use as the "root" for virtual meter {@link ExpressionService}
 * evaluation.
 *
 * @author matt
 * @version 2.1
 * @since 1.6
 */
public class VirtualMeterExpressionRootImpl extends ExpressionRoot
		implements VirtualMeterExpressionRoot {

	private final VirtualMeterConfig config;
	private final long prevDate;
	private final long currDate;
	private final BigDecimal prevInput;
	private final BigDecimal currInput;
	private final BigDecimal prevReading;

	/**
	 * Constructor.
	 *
	 * @param datum
	 *        the datum currently being populated
	 * @param samples
	 *        the samples
	 * @param parameters
	 *        the parameters
	 * @param datumService
	 *        the optional datum service
	 * @param metadataService
	 *        the metadata service
	 * @param config
	 *        the virtual meter configuration
	 * @param prevDate
	 *        the previous reading date, as an epoch
	 * @param currDate
	 *        the current reading date, as an epoch
	 * @param prevInput
	 *        the previous input value
	 * @param currInput
	 *        the current input value
	 * @param prevReading
	 *        the previous reading
	 */
	public VirtualMeterExpressionRootImpl(Datum datum, DatumSamples samples, Map<String, ?> parameters,
			DatumService datumService, MetadataService metadataService, VirtualMeterConfig config,
			long prevDate, long currDate, BigDecimal prevInput, BigDecimal currInput,
			BigDecimal prevReading) {
		super(datum, samples, parameters, datumService, null, metadataService);
		this.config = config;
		this.prevDate = prevDate;
		this.currDate = currDate;
		this.prevInput = prevInput;
		this.currInput = currInput;
		this.prevReading = prevReading;
	}

	@Override
	public VirtualMeterConfig getConfig() {
		return config;
	}

	@Override
	public long getPrevDate() {
		return prevDate;
	}

	@Override
	public long getCurrDate() {
		return currDate;
	}

	@Override
	public BigDecimal getPrevInput() {
		return prevInput;
	}

	@Override
	public BigDecimal getCurrInput() {
		return currInput;
	}

	@Override
	public BigDecimal getPrevReading() {
		return prevReading;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("VirtualMeterExpressionRootImpl{prevDate=");
		builder.append(prevDate);
		builder.append(", currDate=");
		builder.append(currDate);
		builder.append(", timeUnits=");
		builder.append(getTimeUnits());
		builder.append(", ");
		if ( prevInput != null ) {
			builder.append("prevInput=");
			builder.append(prevInput);
			builder.append(", ");
		}
		if ( currInput != null ) {
			builder.append("currInput=");
			builder.append(currInput);
			builder.append(", ");
		}
		builder.append("inputDiff=");
		builder.append(getInputDiff());
		builder.append(", ");
		if ( prevReading != null ) {
			builder.append("prevReading=");
			builder.append(prevReading);
			builder.append(", ");
		}
		builder.append(super.toString());
		builder.append("}");
		return builder.toString();
	}

}
