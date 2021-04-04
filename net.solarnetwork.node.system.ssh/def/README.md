# SolarNode Remote SSH service

This plugin allows SolarNode to establish a SSH connection to a remote server
along with a reverse port forwarding configuration so that remote server can
then SSH back to the SolarNode. This is designed to support remote
administration of the node without the need to have the node exposed to incoming
connections from the public internet.


## OS Level Support

The service relies on a helper program to start, stop, and list the SSH
connections. The helper program is expected to support the following command
line arguments, followed by an action verb:

 * `-u` the SSH username to use
 * `-h` the host name (or IP address) of the server to connect to
 * `-p` the SSH port to use
 * `-r` the port number to establish reverse forwarding back to the
   node's local port 22

After the arguments, the program must accept the following action verbs:

 * `showkey` to print on STDUOUT the public SSH key of the node.
 * `list` to print on STDOUT one line for each active SSH connection,
   as a comma-delimited list in the form `user,host,port,reverse_port(,error)`.
   This action requires no command line arguments. The `error` value
   represents any number of additional error status values that can 
   be conveyed to the calling program.
 * `status` to print on STDOUT either `active` for an active, successful
   connection, or anything else for an error condition.
 * `start` to establish the SSH connection based on the command line
   arguments.
 * `stop` to disconnect an active SSH connection based on the command
   line arguments.

### `systemd` based helper script

The `solarssh.sh` script in this folder meets the helper program requirements.
It relies on a `systemd` template service unit named `solarssh@.service` to be
available that reads the SSH connection settings from an environment file named
after the unit instance name. The `solarssh@.service` file in this folder can be
copied to `/lib/systemd/system/solarssh@.service` to make it available, for
example. The `solarssh.sh` script will dynamically generate an appropriate
environment file with the connection settings passed to it, and generate a
unique service unit instance name based on the connection settings. It will then
use `systemctl` to start, stop, and list the SSH services.

Additionally the `solarssh-cleaner.timer` and associated `solarssh-cleaner.service`
units should be installed, which will close any SSH sessions left active but
no longer working.


### `sudo` support

The `solarssh.sh` script also relies on `sudo` when calling the `systemctl
start` and `systemctl stop`. For that to work, the SolarNode user (usually
`solar`) must be allowed to call those without needing a password. This can be
configured via a `sudoers` configuration file like `/etc/sudoers.d/solarnode`
with content like:

		solar ALL = (root) NOPASSWD: /bin/systemctl start solarssh@*, \
			/bin/systemctl stop solarssh@*


## SSH Level Support

For the SSH connections to be established successfully, they must work without
the need to enter any password, and without requiring confirmation about trusting
the remote server. This can be achieved by copying the remote server's public SSH
key to `~solar/.ssh/known_hosts` (to trust the remote server) and copying the node's
public key (e.g. `~solar/.ssh/id_ecdsa.pub`) to the `~/ssh/authorized_keys` file
on the remote server (for the remote server to trust the node). Alternatively,
SSH can be configured to use signed keys via a trusted SSH certificate authority,
so the `known_hosts` and `authorized_keys` need not be updated.

SSH administration is outside the scope of this guide.


## SolarNetwork instruction support

Once all the OS level support is in place, this service works by responding to
`StartRemoteSsh` and `StopRemoteSsh` instructions from SolarNet to start and
stop SSH connections, respectively. See the [SolarUser API Guide][solaruser-api]
for more information on how instructions work. Both instructions expect the
following parameters:

 * `user` the SSH username to use
 * `host` the host name (or IP address) of the server to connect to
 * `port` the SSH port to use
 * `rport` the port number to establish reverse forwarding back to the
   node's local port 22

These correspond to the helper program arguments discussed earlier in this guide.

  [solaruser-api]: https://github.com/SolarNetwork/solarnetwork/wiki/SolarUser-API#queue-instruction
