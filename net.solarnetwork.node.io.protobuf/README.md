# SolarNode Protobuf I/O

This project contains an OSGi bundle that provides support for working with the [Protobuf][protobuf] 
data serialization framework.

## Datum Encoder

TODO

## Protoc Compiler

TODO

## Tips

Here's an example shell command to decode a hex-encoded Protobuf message with `protoc`:

```sh
echo -n 090000004036486c4011dac69fa86c5a4040189fb5eb02 \
  |xxd -r -p \
  |protoc --proto_path=$PWD --decode=sn.PowerDatum my-datum.proto
```

[protobuf]: https://developers.google.com/protocol-buffers
