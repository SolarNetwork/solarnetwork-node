# SolarNode Task Scheduling

SolarNode provides a task scheduling framework for plugins to be able to run periodic tasks.
The framework provides a few different services that are each designed for different scheduling
styles.

# Java Executor

For simple tasks that need to be run asynchronously on-demand but not on a repeating schedule,
SolarNode publishes a [`java.util.concurrent.Executor`][Executor] service with `(function=node)`
properties that plugins can obtain a reference to.

Here's an example of how a plugin can configure a reference to this service in OSGi Blueprint:

```xml
<reference id="executor" interface="java.util.concurrent.Executor" filter="(function=node)"/>
```

# Spring TaskExecutor

For simple tasks that need to be run asynchronously on-demand but not on a repeating schedule,
SolarNode publishes Spring [`org.springframework.core.task.TaskExecutor`][TaskExecutor] and 
[`org.springframework.core.task.AsyncListenableTaskExecutor`][AsyncListenableTaskExecutor]
services with `(function=node)` properties that plugins can obtain a reference to.

Here's an example of how a plugin can configure a reference to this service in OSGi Blueprint:

```xml
<reference id="taskExecutor" interface="org.springframework.core.task.TaskExecutor" filter="(function=node)"/>
```

# Spring TaskScheduler

For simple tasks that run on a fixed schedule, SolarNode publishes a Spring 
[`org.springframework.scheduling.TaskScheduler`][TaskScheduler] service with `(function=node)`
properties that plugins can obtain a reference to.

Here's an example of how a plugin can configure a reference to this service in OSGi Blueprint:

```xml
<reference id="taskScheduler" interface="org.springframework.scheduling.TaskScheduler" filter="(function=node)"/>
```

# Quartz Scheduler Service

For tasks that need to run on a flexible scheduling pattern, such as a cron-based schedule,
SolarNode uses the [Quartz Scheduler][quartz] scheduler with an in-memory task database.

For low-level direct access to the Quartz framework, this plugin publishes a `org.quartz.Scheduler`
service with `(function=node)` properties that plugins can obtain a reference to. Here's an example 
of how a plugin can configure a reference to this service in OSGi Blueprint:

```xml
<reference id="scheduler" interface="org.quartz.Scheduler" filter="(function=node)"/>
```

It is often more convenient, however, for plugins to publish a
`net.solarnetwork.node.job.TriggerAndJobDetail` or
`net.solarnetwork.node.job.ManagedTriggerAndJobDetail` service and let SolarNode manage the actual
scheduling with Quartz itself.

## TriggerAndJobDetail and ManagedTriggerAndJobDetail

The [`net.solarnetwork.node.job.TriggerAndJobDetail`][TriggerAndJobDetail] and
[`net.solarnetwork.node.job.ManagedTriggerAndJobDetail`][ManagedTriggerAndJobDetail] APIs can be
registered as OSGi services by other plugins, and SolarNode will schedule the tasks on behalf of
the plugin. This frees the plugin from having to interact with the Quartz `Scheduler` directly,
and allows the task and its schedule to be configured via the [SolarNode Settings API][settings] at
runtime. These APIs expose both a Quartz `Trigger` (the task schedule) and a Quartz `Job` (the task
to execute). When SolarNode observes one of these services are registered as an OSGi service at 
runtime, it will register the trigger and job with Quartz.

### `TriggerAndJobDetail`

The [`TriggerAndJobDetail`][TriggerAndJobDetail] API should be used by single-instance services 
(that is, when there is exactly one instance of the service at runtime).

Here's an example of how a plugin can configure a `TriggerAndJobDetail` via OSGi Blueprint:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<blueprint xmlns="http://www.osgi.org/xmlns/blueprint/v1.0.0"
	xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
	xsi:schemaLocation="
		http://www.osgi.org/xmlns/blueprint/v1.0.0
		http://www.osgi.org/xmlns/blueprint/v1.0.0/blueprint.xsd>
	
	<!-- Publish a TriggerAndJobDetail for SolarNode to register with Quartz --> 
	<service interface="net.solarnetwork.node.job.TriggerAndJobDetail">
		<bean class="net.solarnetwork.node.job.SimpleTriggerAndJobDetail">
			<property name="trigger" ref="bulkDatumUploadTrigger"/>
			<property name="jobDetail" ref="bulkDatumUploadJob"/>
			<property name="messageSource">
				<bean class="org.springframework.context.support.ResourceBundleMessageSource">
					<property name="basenames" value="net.solarnetwork.node.upload.bulkjsonwebpost.JOBS"/>
				</bean>
			</property>
		</bean>
	</service>

	<!-- The Quartz trigger (schedule) --> 
	<bean id="bulkDatumUploadTrigger" class="net.solarnetwork.node.job.RandomizedCronTriggerFactoryBean">
		<property name="jobDetail" ref="bulkDatumUploadJob"/>
		<property name="name" value="Bulk Datum Uploader"/>
		<property name="description" value="Upload locally collected data to SolarNetwork"/>
		<property name="cronExpression" value="20 0/5 * * * ?"/>
		<property name="misfireInstructionName" value="MISFIRE_INSTRUCTION_DO_NOTHING"/>
		<property name="randomSecond" value="true"/>
	</bean>

	<!-- The Quartz job (task) --> 
	<bean id="bulkDatumUploadJob" class="org.springframework.scheduling.quartz.JobDetailFactoryBean">
		<property name="jobClass" value="net.solarnetwork.node.job.DatumDaoBulkUploadJob"/>
		<property name="jobDataAsMap">
			<map>
				<entry key="daos" value-ref="datumDaoCollection"/>
				<entry key="uploadService" value-ref="bulkJsonWebPostUploadService"/>
			</map>
		</property>
	</bean>
	
	<-- A service the job usees to perform the actual work -->
	<bean id="bulkJsonWebPostUploadService" 
		class="net.solarnetwork.node.upload.bulkjsonwebpost.BulkJsonWebPostUploadService"/>

</blueprint>
```

### `ManagedTriggerAndJobDetail`

The [`ManagedTriggerAndJobDetail`][ManagedTriggerAndJobDetail] should be used by OSGi 
[ManagedServiceFactory][ManagedServiceFactory] services (that is, services that can have any 
number of themselves created at runtime).

Here's an example of how a mock energy meter plugin configures a `ManagedTriggerAndJobDetail` via 
OSGi Blueprint and the Eclipse Gemini Blueprint `<managed-service-factory>` extension:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<blueprint xmlns="http://www.osgi.org/xmlns/blueprint/v1.0.0"
	xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
	xmlns:ctx="http://www.springframework.org/schema/context"
	xmlns:osgix="http://www.eclipse.org/gemini/blueprint/schema/blueprint-compendium"
	xmlns:beans="http://www.springframework.org/schema/beans"
	xsi:schemaLocation="
		http://www.osgi.org/xmlns/blueprint/v1.0.0
		http://www.osgi.org/xmlns/blueprint/v1.0.0/blueprint.xsd
		http://www.eclipse.org/gemini/blueprint/schema/blueprint-compendium
		http://www.eclipse.org/gemini/blueprint/schema/blueprint-compendium/gemini-blueprint-compendium.xsd
		http://www.springframework.org/schema/beans
		http://www.springframework.org/schema/beans/spring-beans-4.2.xsd
		http://www.springframework.org/schema/context
		http://www.springframework.org/schema/context/spring-context-4.2.xsd">

	<!-- publish a "component factory" so our EnergyMeter appears in the settings GUI -->
	<service interface="net.solarnetwork.node.settings.SettingSpecifierProviderFactory">
		<bean class="net.solarnetwork.node.settings.support.BasicSettingSpecifierProviderFactory">
			<property name="displayName" value="EnergyMeter Mock"/>
			<property name="factoryUid" value="net.solarnetwork.node.datum.energymeter.mock"/>
			<property name="messageSource" ref="messageSource"/>
		</bean>
	</service>

	<!-- publish a "service factory" that each Mock Energy Meter Inverter component instance can be
	     configured in the GUI, along with a periodic job to collect datum from it -->
	<osgix:managed-service-factory factory-pid="net.solarnetwork.node.datum.energymeter.mock" autowire-on-update="true">
		<osgix:interfaces>
			<beans:value>net.solarnetwork.node.job.ManagedTriggerAndJobDetail</beans:value>
			<beans:value>net.solarnetwork.node.settings.SettingSpecifierProvider</beans:value>
		</osgix:interfaces>
		<osgix:service-properties>
			<beans:entry key="settingPid" value="net.solarnetwork.node.datum.energymeter.mock"/>
		</osgix:service-properties>
		<bean class="net.solarnetwork.node.job.SimpleManagedTriggerAndJobDetail">

			<!-- the trigger defines when the periodic job runs; in this case
			     we define a cron style trigger, that by default runs once/minute -->
			<property name="trigger">
				<bean class="net.solarnetwork.node.job.RandomizedCronTriggerFactoryBean">
					<property name="name" value="mockPowerDatumLoggerTrigger"/>
					<property name="cronExpression" value="5 * * * * ?"/>
					<property name="misfireInstructionName" value="MISFIRE_INSTRUCTION_DO_NOTHING"/>
					<property name="randomSecond" value="true"/>
				</bean>
			</property>

			<!-- the jobDetail defines what job should be executed periodically by the
			     trigger; here we define a  net.solarnetwork.node.job.DatumDataSourceManagedLoggerJob
			     job which will invoke the readCurrentDatum() method on a DatumDataSource, which
			     is what MockEnergyMeterDatumDataSource is! -->
			<property name="jobDetail">
				<bean class="org.springframework.scheduling.quartz.JobDetailFactoryBean">
					<property name="name" value="mockPowerDatumLoggerJob"/>
					<property name="jobClass" value="net.solarnetwork.node.job.DatumDataSourceManagedLoggerJob"/>
					<property name="jobDataAsMap">
						<map>
							<entry key="datumDao" value-ref="generalNodeDatumDao"/>
							<entry key="datumDataSource">
								<bean class="net.solarnetwork.node.datum.energymeter.mock.MockEnergyMeterDatumSource">
									<property name="eventAdmin" ref="eventAdmin"/>
									<property name="messageSource" ref="jobMessageSource"/>
								</bean>
							</entry>
						</map>
					</property>
				</bean>
			</property>
		</bean>
	</osgix:managed-service-factory>

	<!-- support localized strings for the settings in the GUI -->
	<bean id="messageSource" class="org.springframework.context.support.ResourceBundleMessageSource">
		<property name="basenames">
			<array>
				<value>net.solarnetwork.node.datum.energymeter.mock.MockEnergyMeterDatumSource</value>
				<value>net.solarnetwork.node.support.DatumDataSourceSupport</value>
				<value>net.solarnetwork.node.support.BaseIdentifiable</value>
			</array>
		</property>
	</bean>

	<!-- support localized strings for the periodic job settings in the GUI -->
	<bean id="jobMessageSource" class="net.solarnetwork.node.util.PrefixedMessageSource">
		<property name="prefix" value="datumDataSource."/>
		<property name="delegate" ref="messageSource"/>
	</bean>

</blueprint>
```



[AsyncListenableTaskExecutor]: https://docs.spring.io/spring-framework/docs/4.3.x/javadoc-api/org/springframework/core/task/AsyncListenableTaskExecutor.html
[Executor]: https://docs.oracle.com/javase/8/docs/api/java/util/concurrent/Executor.html
[quartz]: http://www.quartz-scheduler.org/documentation/quartz-2.2.2/
[ManagedTriggerAndJobDetail]: ../src/net/solarnetwork/node/job/ManagedTriggerAndJobDetail.java
[ManagedServiceFactory]: https://docs.osgi.org/javadoc/r4v42/org/osgi/service/cm/ManagedServiceFactory.html
[settings]: ../Settings.md
[TaskExecutor]: https://docs.spring.io/spring-framework/docs/4.3.x/javadoc-api/org/springframework/core/task/TaskExecutor.html
[TaskScheduler]: https://docs.spring.io/spring-framework/docs/4.3.x/javadoc-api/org/springframework/scheduling/TaskScheduler.html
[TriggerAndJobDetail]: ../src/net/solarnetwork/node/job/TriggerAndJobDetail.java
