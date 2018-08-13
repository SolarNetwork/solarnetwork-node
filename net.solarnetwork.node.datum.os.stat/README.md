# TODO

## Uptime

The plugin will execute a separate command to obtain the uptime of the system,
which must be reported in seconds.

An example Linux command is:

```shell
/usr/bin/cut -f1 -d' ' /proc/uptime
```

An example BSD command is:

```shell
expr $(date +"%s") - $(sysctl -n kern.boottime |sed 's/.* sec = \([0-9][0-9]*\).*/\1/')
```
