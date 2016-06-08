# RFID Scanner Support

This folder contains a Linux C program `rfid-server` which listens for
TCP/IP connections on port **9090** and writes ASCII string messages to 
connected clients. The server will first send a status line which the
client can discard. Then the server will send RFID ID values, one per
line, as they are read from the scanner. If no RFID tags are read
for some time, the server will send a _heartbeat_ to the client in the
form of a single line with the string `ping`.

# OS Level Support

We want the `rfid-server` program to run when the OS boots up, but
it should only start if the RFID scanner device is connected. To 
do that we can use **udev** and **systemd** on Debian 8.

## Add udev rule for consistent device naming

The `rfid-server` accepts the path to the system device that represents
the RFID scanner. To make sure the system consistently maps the device
to a known value, set up a **udev** rule that generates a consistent 
symlink to the device as well as adding the `systemd` tag so our unit
configuration can refer to this device:

	SUBSYSTEM=="input", ATTRS{idVendor}=="04d9", ATTRS{idProduct}=="1503", SYMLINK+="rfid", TAG+="systemd"

This configuration will map the device matching these criteria to `/dev/rfid`.

## OS user

To read from a `/dev/input/X` device on Debian Linux, the user must
be part of the `input` group. Add a new **rfid** user that is included in
this group, that will run our `rfid-server` program:

	useradd -M -c 'RFID Server' -g input -N -s /bin/false rfid

## systemd service

Finally, add a `systemd` service unit to manage the server, which waits
for the `/dev/rfid` device to be available before launching the program.
The unit file is `/etc/systemd/system/rfid-server.service` and contains:

	# rfid-server systemd service unit 

	[Unit]
	Description=RFID server.

	[Service]
	Type=simple
	User=rfid
	Group=input
	Wants=dev-rfid.device
	After=dev-rfid.device
	ExecStart=/usr/local/bin/rfid-server /dev/rfid
	Restart=always
	RestartSec=1

	[Install]
	WantedBy=multi-user.target

Then enable this service:

	systemctl enable rfid-server
