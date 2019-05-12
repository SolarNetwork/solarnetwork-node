# SolarNode DNP3 Changelog

## 2019-05-13 - v1.1.0

 * Expose more DNP3 link layer and outstation configuration as settings on the Outstation
   component. For example the local/remote addresses can now be configured.
 * Expose the DNP3 Server component UID as a setting on the Outstation component. This
   allows configuring the DNP3 server to use, if there are more than one available.
 * Fix to support analog controls. Previously analog controls could not be modified
   because the code looked for a binary control to update.
