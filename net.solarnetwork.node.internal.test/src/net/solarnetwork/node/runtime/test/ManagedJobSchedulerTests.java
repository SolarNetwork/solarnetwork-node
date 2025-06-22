/* ==================================================================
 * ManagedJobSchedulerTests.java - 15/10/2021 9:31:23 AM
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

package net.solarnetwork.node.runtime.test;

import static java.util.Collections.singletonMap;
import static net.solarnetwork.util.CollectionUtils.mapForDictionary;
import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.capture;
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.osgi.service.cm.ConfigurationEvent.CM_UPDATED;
import java.io.Serializable;
import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Delayed;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import org.easymock.Capture;
import org.easymock.EasyMock;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.cm.ConfigurationEvent;
import org.osgi.service.cm.ConfigurationListener;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.Trigger;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.scheduling.support.PeriodicTrigger;
import net.solarnetwork.node.Constants;
import net.solarnetwork.node.job.JobService;
import net.solarnetwork.node.job.SimpleManagedJob;
import net.solarnetwork.node.job.SimpleServiceProviderConfiguration;
import net.solarnetwork.node.runtime.ManagedJobScheduler;
import net.solarnetwork.node.service.support.BaseIdentifiable;
import net.solarnetwork.settings.SettingSpecifier;
import net.solarnetwork.settings.SettingSpecifierProvider;
import net.solarnetwork.settings.SettingsChangeObserver;

/**
 * Test cases for the {@link ManagedJobScheduler} class.
 *
 * @author matt
 * @version 1.1
 */
public class ManagedJobSchedulerTests {

	// API for testing observer support
	public interface ObservingIdentifiableJobService extends JobService, SettingsChangeObserver {
		// nothing
	}

	public static final class TestJobService extends BaseIdentifiable implements JobService {

		private final String settingUid;
		private Serializable otherService = new Serializable() {

			private static final long serialVersionUID = 1210566608748012336L;
		};

		private TestJobService(String settingUid) {
			super();
			this.settingUid = settingUid;
		}

		@Override
		public String getSettingUid() {
			return settingUid;
		}

		@Override
		public List<SettingSpecifier> getSettingSpecifiers() {
			return Collections.emptyList();
		}

		@Override
		public void executeJobService() throws Exception {
			// nothing
		}

		public Serializable getOtherService() {
			return otherService;
		}

	}

	private BundleContext bundleContext;
	private TaskScheduler taskScheduler;
	private ManagedJobScheduler service;

	private Object[] otherMocks;

	@Before
	public void setup() {
		bundleContext = EasyMock.createMock(BundleContext.class);
		taskScheduler = EasyMock.createMock(TaskScheduler.class);
		service = new ManagedJobScheduler(bundleContext, taskScheduler);
		service.setRandomizedCron(false); // make testing easier
	}

	@After
	public void teardown() {
		EasyMock.verify(bundleContext, taskScheduler);
		if ( otherMocks != null ) {
			EasyMock.verify(otherMocks);
		}
	}

	private void replayAll(Object... mocks) {
		EasyMock.replay(bundleContext, taskScheduler);
		if ( mocks != null ) {
			EasyMock.replay(mocks);
			this.otherMocks = mocks;
		}
	}

	private static class TestScheduledFuture extends CompletableFuture<Object>
			implements ScheduledFuture<Object> {

		@Override
		public long getDelay(TimeUnit unit) {
			return 0;
		}

		@Override
		public int compareTo(Delayed o) {
			return 0;
		}

	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Test
	public void register_basic() {
		// GIVEN
		final String settingPid = "foo.bar";
		JobService job = EasyMock.createMock(JobService.class);
		expect(job.getSettingUid()).andReturn(settingPid).anyTimes();
		expect(job.getDisplayName()).andReturn(null).anyTimes();

		// register ConfigurationListener first time
		ServiceRegistration<ConfigurationListener> configurationListenerReg = EasyMock
				.createMock(ServiceRegistration.class);
		expect(bundleContext.registerService(ConfigurationListener.class, service, null))
				.andReturn(configurationListenerReg);

		// register SettingSpecifierProvider for job
		ServiceRegistration<SettingSpecifierProvider> settingProviderReg = EasyMock
				.createMock(ServiceRegistration.class);
		Capture<Dictionary<String, ?>> settingProviderRegPropsCaptor = Capture.newInstance();
		expect(bundleContext.registerService(eq(SettingSpecifierProvider.class),
				anyObject(SettingSpecifierProvider.class), capture(settingProviderRegPropsCaptor)))
						.andReturn(settingProviderReg);

		// schedule the actual job
		Capture<Runnable> taskCaptor = Capture.newInstance();
		Capture<Trigger> trigCaptor = Capture.newInstance();
		TestScheduledFuture future = new TestScheduledFuture();
		expect(taskScheduler.schedule(capture(taskCaptor), capture(trigCaptor)))
				.andReturn((ScheduledFuture) future);

		// WHEN
		replayAll(job, configurationListenerReg, settingProviderReg);
		SimpleManagedJob managedJob = new SimpleManagedJob(job);
		managedJob.setSchedule("0 * * * * ?");
		service.registerJob(managedJob, Collections.emptyMap());

		// THEN
		Map<String, ?> settingProviderRegProps = mapForDictionary(
				settingProviderRegPropsCaptor.getValue());
		assertThat("ManagedJob SettingSpecifierProvider registered with settingPid property",
				settingProviderRegProps, hasEntry(Constants.SETTING_PID, settingPid));
		assertThat("Scheduled trigger is cron", trigCaptor.getValue(),
				is(new CronTrigger(managedJob.getSchedule())));
		assertThat("Scheduled task active", future.isDone(), is(false));
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Test
	public void register_basic_delay() {
		// GIVEN
		service.setJobStartDelaySeconds(1);

		// schedule startup task
		Capture<Runnable> startupTaskCaptor = Capture.newInstance();
		Capture<Instant> startupTaskDelayCaptor = Capture.newInstance();
		TestScheduledFuture startupTaskFuture = new TestScheduledFuture();
		expect(taskScheduler.schedule(capture(startupTaskCaptor), capture(startupTaskDelayCaptor)))
				.andReturn((ScheduledFuture) startupTaskFuture);

		final String settingPid = "foo.bar";
		JobService job = EasyMock.createMock(JobService.class);
		expect(job.getSettingUid()).andReturn(settingPid).anyTimes();
		expect(job.getDisplayName()).andReturn(null).anyTimes();

		// register ConfigurationListener first time
		ServiceRegistration<ConfigurationListener> configurationListenerReg = EasyMock
				.createMock(ServiceRegistration.class);
		expect(bundleContext.registerService(ConfigurationListener.class, service, null))
				.andReturn(configurationListenerReg);

		// register SettingSpecifierProvider for job
		ServiceRegistration<SettingSpecifierProvider> settingProviderReg = EasyMock
				.createMock(ServiceRegistration.class);
		Capture<Dictionary<String, ?>> settingProviderRegPropsCaptor = Capture.newInstance();
		expect(bundleContext.registerService(eq(SettingSpecifierProvider.class),
				anyObject(SettingSpecifierProvider.class), capture(settingProviderRegPropsCaptor)))
						.andReturn(settingProviderReg);

		// WHEN
		replayAll(job, configurationListenerReg, settingProviderReg);
		service.serviceDidStartup();
		SimpleManagedJob managedJob = new SimpleManagedJob(job);
		managedJob.setSchedule("0 * * * * ?");
		service.registerJob(managedJob, Collections.emptyMap());

		// THEN
		Map<String, ?> settingProviderRegProps = mapForDictionary(
				settingProviderRegPropsCaptor.getValue());
		assertThat("ManagedJob SettingSpecifierProvider registered with settingPid property",
				settingProviderRegProps, hasEntry(Constants.SETTING_PID, settingPid));

		EasyMock.verify(taskScheduler);
		EasyMock.reset(taskScheduler);

		// schedule the actual job
		Capture<Runnable> taskCaptor = Capture.newInstance();
		Capture<Trigger> trigCaptor = Capture.newInstance();
		TestScheduledFuture future = new TestScheduledFuture();
		expect(taskScheduler.schedule(capture(taskCaptor), capture(trigCaptor)))
				.andReturn((ScheduledFuture) future);

		// WHEN
		// run the startup task to schedule the job
		EasyMock.replay(taskScheduler);
		startupTaskCaptor.getValue().run();

		assertThat("Scheduled trigger is cron", trigCaptor.getValue(),
				is(new CronTrigger(managedJob.getSchedule())));
		assertThat("Scheduled task active", future.isDone(), is(false));

	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Test
	public void register_basic_shutdown() {
		// GIVEN
		final String settingPid = "foo.bar";
		JobService job = EasyMock.createMock(JobService.class);
		expect(job.getSettingUid()).andReturn(settingPid).anyTimes();
		expect(job.getDisplayName()).andReturn(null).anyTimes();

		// register ConfigurationListener first time
		ServiceRegistration<ConfigurationListener> configurationListenerReg = EasyMock
				.createMock(ServiceRegistration.class);
		expect(bundleContext.registerService(ConfigurationListener.class, service, null))
				.andReturn(configurationListenerReg);

		// register SettingSpecifierProvider for job
		ServiceRegistration<SettingSpecifierProvider> settingProviderReg = EasyMock
				.createMock(ServiceRegistration.class);
		Capture<Dictionary<String, ?>> settingProviderRegPropsCaptor = Capture.newInstance();
		expect(bundleContext.registerService(eq(SettingSpecifierProvider.class),
				anyObject(SettingSpecifierProvider.class), capture(settingProviderRegPropsCaptor)))
						.andReturn(settingProviderReg);

		// schedule the actual job
		Capture<Runnable> taskCaptor = Capture.newInstance();
		Capture<Trigger> trigCaptor = Capture.newInstance();
		TestScheduledFuture future = new TestScheduledFuture();
		expect(taskScheduler.schedule(capture(taskCaptor), capture(trigCaptor)))
				.andReturn((ScheduledFuture) future);

		// unregsiter SettingSpecifierProvider
		settingProviderReg.unregister();

		// unregsiter ConfigurationListener
		configurationListenerReg.unregister();

		// WHEN
		replayAll(job, configurationListenerReg, settingProviderReg);
		SimpleManagedJob managedJob = new SimpleManagedJob(job);
		managedJob.setSchedule("0 * * * * ?");
		service.registerJob(managedJob, Collections.emptyMap());
		service.serviceDidShutdown();

		// THEN
		Map<String, ?> settingProviderRegProps = mapForDictionary(
				settingProviderRegPropsCaptor.getValue());
		assertThat("ManagedJob SettingSpecifierProvider registered with settingPid property",
				settingProviderRegProps, hasEntry(Constants.SETTING_PID, settingPid));
		assertThat("Scheduled trigger is cron", trigCaptor.getValue(),
				is(new CronTrigger(managedJob.getSchedule())));
		assertThat("Scheduled task cancelled after shutdown", future.isCancelled(), is(true));
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Test
	public void register_serviceProvider() {
		// GIVEN
		final String settingPid = "foo.bar";
		TestJobService job = new TestJobService(settingPid);

		// register ConfigurationListener first time
		ServiceRegistration<ConfigurationListener> configurationListenerReg = EasyMock
				.createMock(ServiceRegistration.class);
		expect(bundleContext.registerService(ConfigurationListener.class, service, null))
				.andReturn(configurationListenerReg);

		// register the service provider
		ServiceRegistration<Serializable> serviceProviderReg = EasyMock
				.createMock(ServiceRegistration.class);
		Capture<Dictionary<String, ?>> serviceProviderRegPropsCaptor = Capture.newInstance();
		expect(bundleContext.registerService(
				EasyMock.aryEq(new String[] { Serializable.class.getName() }),
				anyObject(Serializable.class), capture(serviceProviderRegPropsCaptor)))
						.andReturn((ServiceRegistration) serviceProviderReg);

		// register SettingSpecifierProvider for job
		ServiceRegistration<SettingSpecifierProvider> settingProviderReg = EasyMock
				.createMock(ServiceRegistration.class);
		Capture<Dictionary<String, ?>> settingProviderRegPropsCaptor = Capture.newInstance();
		expect(bundleContext.registerService(eq(SettingSpecifierProvider.class),
				anyObject(SettingSpecifierProvider.class), capture(settingProviderRegPropsCaptor)))
						.andReturn(settingProviderReg);

		// schedule the actual job
		Capture<Runnable> taskCaptor = Capture.newInstance();
		Capture<Trigger> trigCaptor = Capture.newInstance();
		TestScheduledFuture future = new TestScheduledFuture();
		expect(taskScheduler.schedule(capture(taskCaptor), capture(trigCaptor)))
				.andReturn((ScheduledFuture) future);

		// WHEN
		replayAll(configurationListenerReg, settingProviderReg, serviceProviderReg);
		SimpleManagedJob managedJob = new SimpleManagedJob(job);
		managedJob.setSchedule("0 * * * * ?");

		SimpleServiceProviderConfiguration serviceProviderConf = new SimpleServiceProviderConfiguration();
		serviceProviderConf.setInterfaces(new String[] { Serializable.class.getName() });
		serviceProviderConf.setProperties(singletonMap("bim", "bam"));
		managedJob.setServiceProviderConfigurations(singletonMap("otherService", serviceProviderConf));

		service.registerJob(managedJob, Collections.emptyMap());

		// THEN
		Map<String, ?> settingProviderRegProps = mapForDictionary(
				settingProviderRegPropsCaptor.getValue());
		assertThat("ManagedJob SettingSpecifierProvider registered with settingPid property",
				settingProviderRegProps, hasEntry(Constants.SETTING_PID, settingPid));
		assertThat("Scheduled trigger is cron", trigCaptor.getValue(),
				is(new CronTrigger(managedJob.getSchedule())));
		assertThat("Scheduled task active", future.isDone(), is(false));

		Map<String, ?> servicePropsRegProps = mapForDictionary(serviceProviderRegPropsCaptor.getValue());
		assertThat("Service provider props provided", servicePropsRegProps.keySet(),
				hasSize(serviceProviderConf.getProperties().size()));
		for ( Entry<String, Object> e : serviceProviderConf.getProperties().entrySet() ) {
			assertThat("Service property prop copied to service reg", servicePropsRegProps,
					hasEntry(e.getKey(), e.getValue()));
		}
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Test
	public void register_basic_execute() throws Exception {
		// GIVEN
		final String settingPid = "foo.bar";
		JobService job = EasyMock.createMock(JobService.class);
		expect(job.getSettingUid()).andReturn(settingPid).anyTimes();
		expect(job.getDisplayName()).andReturn(null).anyTimes();

		// register ConfigurationListener first time
		ServiceRegistration<ConfigurationListener> configurationListenerReg = EasyMock
				.createMock(ServiceRegistration.class);
		expect(bundleContext.registerService(ConfigurationListener.class, service, null))
				.andReturn(configurationListenerReg);

		// register SettingSpecifierProvider for job
		ServiceRegistration<SettingSpecifierProvider> settingProviderReg = EasyMock
				.createMock(ServiceRegistration.class);
		Capture<Dictionary<String, ?>> settingProviderRegPropsCaptor = Capture.newInstance();
		expect(bundleContext.registerService(eq(SettingSpecifierProvider.class),
				anyObject(SettingSpecifierProvider.class), capture(settingProviderRegPropsCaptor)))
						.andReturn(settingProviderReg);

		// schedule the actual job
		Capture<Runnable> taskCaptor = Capture.newInstance();
		Capture<Trigger> trigCaptor = Capture.newInstance();
		TestScheduledFuture future = new TestScheduledFuture();
		expect(taskScheduler.schedule(capture(taskCaptor), capture(trigCaptor)))
				.andReturn((ScheduledFuture) future);

		// execute job
		job.executeJobService();

		// WHEN
		replayAll(job, configurationListenerReg, settingProviderReg);
		SimpleManagedJob managedJob = new SimpleManagedJob(job);
		managedJob.setSchedule("0 * * * * ?");
		service.registerJob(managedJob, Collections.emptyMap());

		// execute task, should invoke executeJobService()
		taskCaptor.getValue().run();

		// THEN
		Map<String, ?> settingProviderRegProps = mapForDictionary(
				settingProviderRegPropsCaptor.getValue());
		assertThat("ManagedJob SettingSpecifierProvider registered with settingPid property",
				settingProviderRegProps, hasEntry(Constants.SETTING_PID, settingPid));
		assertThat("Scheduled trigger is cron", trigCaptor.getValue(),
				is(new CronTrigger(managedJob.getSchedule())));
		assertThat("Scheduled task active", future.isDone(), is(false));
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Test
	public void unregister_basic() {
		// GIVEN
		final String settingPid = "foo.bar";
		JobService job = EasyMock.createMock(JobService.class);
		expect(job.getSettingUid()).andReturn(settingPid).anyTimes();
		expect(job.getDisplayName()).andReturn(null).anyTimes();

		// register ConfigurationListener first time
		ServiceRegistration<ConfigurationListener> configurationListenerReg = EasyMock
				.createMock(ServiceRegistration.class);
		expect(bundleContext.registerService(ConfigurationListener.class, service, null))
				.andReturn(configurationListenerReg);

		// register SettingSpecifierProvider for job
		ServiceRegistration<SettingSpecifierProvider> settingProviderReg = EasyMock
				.createMock(ServiceRegistration.class);
		Capture<Dictionary<String, ?>> settingProviderRegPropsCaptor = Capture.newInstance();
		expect(bundleContext.registerService(eq(SettingSpecifierProvider.class),
				anyObject(SettingSpecifierProvider.class), capture(settingProviderRegPropsCaptor)))
						.andReturn(settingProviderReg);

		// schedule the actual job
		Capture<Runnable> taskCaptor = Capture.newInstance();
		Capture<Trigger> trigCaptor = Capture.newInstance();
		TestScheduledFuture future = new TestScheduledFuture();
		expect(taskScheduler.schedule(capture(taskCaptor), capture(trigCaptor)))
				.andReturn((ScheduledFuture) future);

		// unregsiter SettingSpecifierProvider
		settingProviderReg.unregister();

		// WHEN
		replayAll(job, configurationListenerReg, settingProviderReg);
		SimpleManagedJob managedJob = new SimpleManagedJob(job);
		managedJob.setSchedule("0 * * * * ?");
		service.registerJob(managedJob, Collections.emptyMap());
		service.unregisterJob(managedJob, Collections.emptyMap());

		// THEN
		Map<String, ?> settingProviderRegProps = mapForDictionary(
				settingProviderRegPropsCaptor.getValue());
		assertThat("ManagedJob SettingSpecifierProvider registered with settingPid property",
				settingProviderRegProps, hasEntry(Constants.SETTING_PID, settingPid));
		assertThat("Scheduled trigger is cron", trigCaptor.getValue(),
				is(new CronTrigger(managedJob.getSchedule())));
		assertThat("Scheduled task cancelled", future.isCancelled(), is(true));
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Test
	public void unregister_serviceProvider() {
		// GIVEN
		final String settingPid = "foo.bar";
		TestJobService job = new TestJobService(settingPid);

		// register ConfigurationListener first time
		ServiceRegistration<ConfigurationListener> configurationListenerReg = EasyMock
				.createMock(ServiceRegistration.class);
		expect(bundleContext.registerService(ConfigurationListener.class, service, null))
				.andReturn(configurationListenerReg);

		// register the service provider
		ServiceRegistration<Serializable> serviceProviderReg = EasyMock
				.createMock(ServiceRegistration.class);
		Capture<Dictionary<String, ?>> serviceProviderRegPropsCaptor = Capture.newInstance();
		expect(bundleContext.registerService(
				EasyMock.aryEq(new String[] { Serializable.class.getName() }),
				anyObject(Serializable.class), capture(serviceProviderRegPropsCaptor)))
						.andReturn((ServiceRegistration) serviceProviderReg);

		// register SettingSpecifierProvider for job
		ServiceRegistration<SettingSpecifierProvider> settingProviderReg = EasyMock
				.createMock(ServiceRegistration.class);
		Capture<Dictionary<String, ?>> settingProviderRegPropsCaptor = Capture.newInstance();
		expect(bundleContext.registerService(eq(SettingSpecifierProvider.class),
				anyObject(SettingSpecifierProvider.class), capture(settingProviderRegPropsCaptor)))
						.andReturn(settingProviderReg);

		// schedule the actual job
		Capture<Runnable> taskCaptor = Capture.newInstance();
		Capture<Trigger> trigCaptor = Capture.newInstance();
		TestScheduledFuture future = new TestScheduledFuture();
		expect(taskScheduler.schedule(capture(taskCaptor), capture(trigCaptor)))
				.andReturn((ScheduledFuture) future);

		// unregsiter SettingSpecifierProvider
		settingProviderReg.unregister();

		// unregister service provider
		serviceProviderReg.unregister();

		// WHEN
		replayAll(configurationListenerReg, settingProviderReg);
		SimpleManagedJob managedJob = new SimpleManagedJob(job);
		managedJob.setSchedule("0 * * * * ?");

		SimpleServiceProviderConfiguration serviceProviderConf = new SimpleServiceProviderConfiguration();
		serviceProviderConf.setInterfaces(new String[] { Serializable.class.getName() });
		serviceProviderConf.setProperties(singletonMap("bim", "bam"));
		managedJob.setServiceProviderConfigurations(singletonMap("otherService", serviceProviderConf));

		service.registerJob(managedJob, Collections.emptyMap());
		service.unregisterJob(managedJob, Collections.emptyMap());

		// THEN
		Map<String, ?> settingProviderRegProps = mapForDictionary(
				settingProviderRegPropsCaptor.getValue());
		assertThat("ManagedJob SettingSpecifierProvider registered with settingPid property",
				settingProviderRegProps, hasEntry(Constants.SETTING_PID, settingPid));
		assertThat("Scheduled trigger is cron", trigCaptor.getValue(),
				is(new CronTrigger(managedJob.getSchedule())));
		assertThat("Scheduled task cancelled", future.isCancelled(), is(true));
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Test
	public void observer_change() throws Exception {
		// GIVEN
		final String settingPid = "foo.bar";
		ObservingIdentifiableJobService job = EasyMock.createMock(ObservingIdentifiableJobService.class);
		expect(job.getSettingUid()).andReturn(settingPid).anyTimes();
		expect(job.getDisplayName()).andReturn(null).anyTimes();

		// register ConfigurationListener first time
		ServiceRegistration<ConfigurationListener> configurationListenerReg = EasyMock
				.createMock(ServiceRegistration.class);
		expect(bundleContext.registerService(ConfigurationListener.class, service, null))
				.andReturn(configurationListenerReg);

		// register SettingSpecifierProvider for job
		ServiceRegistration<SettingSpecifierProvider> settingProviderReg = EasyMock
				.createMock(ServiceRegistration.class);
		Capture<Dictionary<String, ?>> settingProviderRegPropsCaptor = Capture.newInstance();
		expect(bundleContext.registerService(eq(SettingSpecifierProvider.class),
				anyObject(SettingSpecifierProvider.class), capture(settingProviderRegPropsCaptor)))
						.andReturn(settingProviderReg);

		// schedule the actual job
		Capture<Runnable> taskCaptor = Capture.newInstance();
		Capture<Trigger> trigCaptor = Capture.newInstance();
		TestScheduledFuture future = new TestScheduledFuture();
		expect(taskScheduler.schedule(capture(taskCaptor), capture(trigCaptor)))
				.andReturn((ScheduledFuture) future);

		ConfigurationAdmin ca = EasyMock.createMock(ConfigurationAdmin.class);
		ServiceReference<ConfigurationAdmin> caRef = EasyMock.createMock(ServiceReference.class);
		expect(bundleContext.getService(caRef)).andReturn(ca).anyTimes();
		Configuration conf = EasyMock.createMock(Configuration.class);
		expect(ca.getConfiguration(settingPid, null)).andReturn(conf);
		Hashtable<String, Object> confProps = new Hashtable<>();
		confProps.put("schedule", "1000");
		expect(conf.getProperties()).andReturn(confProps);

		// reschedule the actual job
		Capture<Runnable> taskCaptor2 = Capture.newInstance();
		Capture<Trigger> trigCaptor2 = Capture.newInstance();
		TestScheduledFuture future2 = new TestScheduledFuture();
		expect(taskScheduler.schedule(capture(taskCaptor2), capture(trigCaptor2)))
				.andReturn((ScheduledFuture) future2);

		// WHEN
		replayAll(job, configurationListenerReg, settingProviderReg, ca, caRef, conf);
		SimpleManagedJob managedJob = new SimpleManagedJob(job);
		managedJob.setSchedule("0 * * * * ?");
		service.registerJob(managedJob, Collections.emptyMap());

		// CM update
		ConfigurationEvent evt = new ConfigurationEvent(caRef, CM_UPDATED, null, settingPid);
		service.configurationEvent(evt);

		// THEN
		Map<String, ?> settingProviderRegProps = mapForDictionary(
				settingProviderRegPropsCaptor.getValue());
		assertThat("ManagedJob SettingSpecifierProvider registered with settingPid property",
				settingProviderRegProps, hasEntry(Constants.SETTING_PID, settingPid));
		assertThat("Scheduled trigger is cron", trigCaptor.getValue(),
				is(new CronTrigger(managedJob.getSchedule())));
		assertThat("Scheduled task cancelled from reschedule", future.isCancelled(), is(true));
		PeriodicTrigger expectedTrigger = new PeriodicTrigger(Duration.ofMillis(1000));
		expectedTrigger.setFixedRate(true);
		assertThat("Rescheduled trigger is periodic from settings update", trigCaptor2.getValue(),
				is(expectedTrigger));
		assertThat("Rescheduled task active", future2.isDone(), is(false));
	}

}
