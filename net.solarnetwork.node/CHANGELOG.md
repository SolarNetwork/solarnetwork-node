# Core SolarNode Framework Changelog

## 1.73.1 - 2020-01-23

 * Add support for a `jobScheduler.poolSize` configuration property in the `net.solarnetwork.node.core`
   configuration, so that the number of job threads is configurable. Defaults to 3.

## 1.70 - 2019-10-10

 * Add support for text area and file-based settings UI to support configuration from external
   resources.
 * Add `n.s.n.settings.SettingsChangeObserver` API and update `SimpleManagedTriggerAndJobDetail` so
   that if the wired `SettingSpecifierProvider` also implements `SettingsChangeObserver`, then 
   managed property updates applied to the job can be passed to the job instance so it can respond
   to setting changes (possibly applied to external resources) appropriately.

## 1.69 - 2019-09-15

 * Update to OSGi Compendium R5 API.

## 1.68 - 2019-05-23

 * Add `n.s.n.PlatformPackageService` for help with installing packages such as tar archives
   and native OS packages.

## 1.67 - 2019-05-15

 * Add `n.s.n.support.BaseIdentifiable` class to provide common base implementation of
   the common `n.s.n.Identifiable` and `n.s.domain.Identifiable` APIs.
 * Make published `org.springframework.core.task.AsyncListenableTaskExecutor` service also
   expose `org.springframework.core.task.AsyncTaskExecutor` and `java.util.concurrent.Executor` APIs
   so those APIs can be referenced by other bundles.
 * Make the core `java.util.concurrent.ExecutorService` based on a fixed-size pool with a size
   based on the number of CPU cores available. Give threads in this pool names like
   _SolarNode-Core-0_.
 * Define new `UpdateSetting` instruction topic to support changing node settings via the
   instruction API. [Node-164](https://data.solarnetwork.net/jira/browse/NODE-164)
 * Define new `SystemReboot` and `SystemRestart` instruction topics to support restarting
   SolarNode via the instruction API. [Node-163](https://data.solarnetwork.net/jira/browse/NODE-163)
 * Define new `SetOperatingState` instruction topic to support changing device operating states via the
   instruction API. [Node-166](https://data.solarnetwork.net/jira/browse/NODE-166)
