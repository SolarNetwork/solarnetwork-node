# SolarNode DNP3

## Shared library

Note that SolarNode must be able to find the OpenDNP3 `libopendnp3java` shared library at runtime.
If this is not available in a standard location, add the appropriate path to the `java.library.path`
system property. For example, pass the following argument to the JVM:

```
-Djava.library.path=/usr/local/lib
```

If you see an error like

```
java.lang.UnsatisfiedLinkError: no opendnp3java in java.library.path
```

that means the shared library was not found.
