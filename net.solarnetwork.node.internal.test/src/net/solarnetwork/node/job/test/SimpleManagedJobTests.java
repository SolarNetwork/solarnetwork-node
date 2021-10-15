/* ==================================================================
 * SimpleManagedJobTests.java - 15/10/2021 6:08:13 PM
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

package net.solarnetwork.node.job.test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import org.junit.Test;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.PropertyAccessorFactory;
import net.solarnetwork.node.domain.datum.NodeDatum;
import net.solarnetwork.node.job.DatumDataSourcePollManagedJob;
import net.solarnetwork.node.job.SimpleManagedJob;
import net.solarnetwork.node.service.DatumDataSource;
import net.solarnetwork.node.service.support.BaseIdentifiable;

/**
 * Test cases for the {@link SimpleManagedJob} class.
 * 
 * @author matt
 * @version 1.0
 */
public class SimpleManagedJobTests {

	private static final class TestDatumDataSource extends BaseIdentifiable implements DatumDataSource {

		private String someProperty;

		@Override
		public Class<? extends NodeDatum> getDatumType() {
			return NodeDatum.class;
		}

		@Override
		public NodeDatum readCurrentDatum() {
			return null;
		}

		public String getSomeProperty() {
			return someProperty;
		}

		@SuppressWarnings("unused")
		public void setSomeProperty(String someProperty) {
			this.someProperty = someProperty;
		}

	}

	@Test
	public void legacyPropertyPopulation() {
		// GIVEN
		TestDatumDataSource ds = new TestDatumDataSource();
		DatumDataSourcePollManagedJob job = new DatumDataSourcePollManagedJob();
		job.setDatumDataSource(ds);
		SimpleManagedJob managedJob = new SimpleManagedJob(job);

		// WHEN
		final String schedule = "1 * * * * ?";
		final String uid = "Test UID";
		final String someProperty = "Foobar";
		BeanWrapper bean = PropertyAccessorFactory.forBeanPropertyAccess(managedJob);
		bean.setPropertyValue("triggerCronExpression", schedule);
		bean.setPropertyValue("jobDetail.jobDataMap['datumDataSource'].uid", uid);
		bean.setPropertyValue("jobDetail.jobDataMap['datumDataSource'].someProperty", someProperty);

		// THEN
		assertThat("Set the schedule based on legacy property name", managedJob.getSchedule(),
				is(schedule));
		assertThat("Set the DataSource UID based on legacy Quartz job property path", ds.getUid(),
				is(uid));
		assertThat("Set the DataSource someProperty based on legacy Quartz job property path",
				ds.getSomeProperty(), is(someProperty));
	}

}
