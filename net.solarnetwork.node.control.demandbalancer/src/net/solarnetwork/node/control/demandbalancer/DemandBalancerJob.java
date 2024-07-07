/* ==================================================================
 * DemandBalancerJob.java - Mar 23, 2014 3:43:41 PM
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

import java.util.ArrayList;
import java.util.List;
import org.springframework.context.MessageSource;
import net.solarnetwork.node.job.JobService;
import net.solarnetwork.node.service.support.BaseIdentifiable;
import net.solarnetwork.settings.KeyedSettingSpecifier;
import net.solarnetwork.settings.SettingSpecifier;
import net.solarnetwork.util.ObjectUtils;

/**
 * Job to execute the {@link DemandBalancer} on a schedule.
 *
 * <p>
 * Note that {@link #getSettingSpecifiers()} will map all
 * {@link KeyedSettingSpecifier} instances to the {@code demandBalancer.}
 * prefix. The corresponding {@link DemandBalancer#getMessageSource()} should be
 * configured as a {@link net.solarnetwork.support.PrefixedMessageSource} using
 * the same prefix in order for the mapping to work correctly.
 * </p>
 *
 * @author matt
 * @version 2.0
 */
public class DemandBalancerJob extends BaseIdentifiable implements JobService {

	private final DemandBalancer demandBalancer;

	/**
	 * Constructor.
	 *
	 * @param demandBalancer
	 *        the balancer
	 */
	public DemandBalancerJob(DemandBalancer demandBalancer) {
		super();
		this.demandBalancer = ObjectUtils.requireNonNullArgument(demandBalancer, "demandBalancer");
	}

	@Override
	public void executeJobService() throws Exception {
		demandBalancer.evaluateBalance();
	}

	@Override
	public String getSettingUid() {
		return demandBalancer.getSettingUid();
	}

	@Override
	public String getDisplayName() {
		return demandBalancer.getDisplayName();
	}

	@Override
	public MessageSource getMessageSource() {
		return demandBalancer.getMessageSource();
	}

	@Override
	public List<SettingSpecifier> getSettingSpecifiers() {
		List<SettingSpecifier> result = new ArrayList<>();
		for ( SettingSpecifier spec : demandBalancer.getSettingSpecifiers() ) {
			if ( spec instanceof KeyedSettingSpecifier<?> ) {
				KeyedSettingSpecifier<?> keyedSpec = (KeyedSettingSpecifier<?>) spec;
				result.add(keyedSpec.mappedTo("demandBalancer."));
			} else {
				result.add(spec);
			}
		}
		return result;
	}

	/**
	 * Get the demand balancer.
	 *
	 * @return the service
	 */
	public DemandBalancer getDemandBalancer() {
		return demandBalancer;
	}

}
