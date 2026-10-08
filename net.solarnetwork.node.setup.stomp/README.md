# SolarNode Setup: STOMP

This project contains a SolarNode plugin that provides a
[STOMP](https://stomp.github.io/) server that enables setup and configuration
tasks in an external application. A primary use case for this plugin is to
enable an application on a Bluetooth-enabled phone to provide an easy-to-use UI
for performing basic SolarNode setup tasks, such as confirming that SolarNode is
connected to the internet.

STOMP is a well-established and simple text-based bi-directional communication
protocol that has similarities to the structure of HTTP 1.x requests. A STOMP
message is called a *frame* and is structured as lines of text, each line ending
with an `EOL` (`\n` or ASCII `0x10`) like:

```text
COMMAND
header1:value1
header2:value2

Body^@
```

The `COMMAND` is like a HTTP verb, and is one of the values defined in the STOMP
standard. The command is followed by zero or more header key/value pairs, much
like HTTP headers. A blank line follows that, followed by zero or more
characters representing the message body, finished with a `NULL` byte,
represented by `^@` for <kbd>Ctrl-@</kbd>.

# Connecting

To connect to the SolarNode STOMP Setup server open a TCP/IP socket connection
to the host name or IP address of SolarNode, using the port configured in the
server's settings (the default is `8780`). You must then send a `CONNECT` or
`STOMP` frame.

## `CONNECT` frame

The `CONNECT` frame is sent by the client and used to initiate a new *setup
session*. It must be the first frame sent by the connected client. The following
frame headers are required:

| Header           | Description                                                                         |
|:-----------------|:------------------------------------------------------------------------------------|
| `accept-version` | Only `1.2` is allowed.                                                              |
| `host`           | The host name or IP address of SolarNode.                                           |
| `login`          | The SolarNode login to use for the session. This login will be authenticated later. |

Upon successful receipt of a `CONNECT` frame the server will send a
[`CONNECTED`](#connected-frame) frame to the client.

An example `CONNECT` frame looks like this:

```text
CONNECT
accept-version:1.2
host:solarnode
login:solar

^@
```

## `CONNECTED` frame

The `CONNECTED` frame is sent by the server and used to indicate a new *setup
session* has been successfully started. After receipt of this frame a client
must [authenticate](#authenticating). The following frame headers will be
returned:

| Header              | Description                                                |
|:--------------------|:-----------------------------------------------------------|
| `version`           | The STOMP version accepted by the server. Will be `1.2`.   |
| `server`            | The setup server name and version.                         |
| `session`           | The unique ID of the setup session.                        |
| `message`           | A request to authenticate.                                 |
| `authenticate`      | The required authentication scheme. Will be `SNS`.         |
| `auth-hash`         | The password digest algorithm to use.                      |
| `auth-hash-param-*` | Any number of password digest algorithm parameters to use. |

The only value currently supported for `auth-hash` is `bcrypt`, and the only
parameter returned will be `auth-hash-param-salt`. This salt value must be used
when [authenticating](#authenticating).

An example `CONNECTED` frame looks like this:

```text
CONNECTED
version:1.2
server:SolarNode-Setup/1.0
session:a48c33d5-307e-4387-86a6-cd3cae372f78
message:Please authenticate.
authenticate:SNS
auth-hash:bcrypt,
auth-hash-param-salt:$2a$10$upVbEZHge9Iph1NN3L6ENO

^@
```

# Authenticating

Once a client receives a `CONNECTED` frame, it must authenticate the session,
using the scheme specified in the `authenticate` header. Authentication is
performed by sending a `SEND` frame with a `destination` header value of
`/setup/authenticate` and an `authorization` header with the appropriate
credentials, the syntax of which depends on the scheme used. **Note** that the
scheme might require additional headers. The only supported scheme at this time
is `SNS`, described in the next section.

After publishing the `SEND` authentication frame, if the authentication is
successful nothing will happen and the client application can move on to
[subscribing to the setup topic](#subscribing-to-setup-topic) to start
interacting with the Setup Server. If the authentication fails, the server will
send an `ERROR` frame to the client and close the connection.

## SNS authentication scheme

The `SNS` scheme is loosely based on the
[SNWS2](https://github.com/SolarNetwork/solarnetwork/wiki/SolarNet-API-authentication-scheme-V2)
scheme used by SolarNetwork, which is itself loosely based on the [AWS Signature
Version
4](https://docs.aws.amazon.com/AmazonS3/latest/API/sig-v4-authenticating-requests.html)
scheme. At a high level, the authentication is performed using a HMAC+SHA256
digest of various parts of the STOMP frame, signed using a secret key derived
from the SolarNode user's hashed password. SolarNode does not store a plain-text
version of a user's password, so that is why the hashed password is used for the
signing key.

The required `SEND` headers for SNS authentication are:

| Header          | Description                                                                                               |
|:----------------|:----------------------------------------------------------------------------------------------------------|
| `authorization` | The SNS authorization value, e.g. `SNS Credential=me@example.com,SignedHeaders=date,Signature=168365...`. |
| `date`          | The request date, e.g. `Mon, 16 Aug 2021 02:27:39 GMT`.                                                   |

TODO: document SNS scheme

## SNS authentication example

Here is a basic example in Java that uses the
[SnsAuthorizationBuilder](https://github.com/SolarNetwork/solarnetwork-common/blob/develop/net.solarnetwork.common/src/net/solarnetwork/security/SnsAuthorizationBuilder.java)
class to generate the required `authorization` and `date` headers required by
the SNS scheme:

```java
String secret = "value-derived-from-password"; // depends on `auth-hash` CONNECTED header
SnsAuthorizationBuilder authBuilder = new SnsAuthorizationBuilder("me@example.com")
	.date(now)
	.verb("SEND")
	.path("/setup/authenticate");
String authHeader = authBuilder.build(secret);
String dateHeader = authBuilder.headerValue("date");
```

## BCrypt secret derivation

The `auth-hash:bcrypt` `CONNECTED` header indicates that the BCrypt digest
algorithm must be used to derive the SNS secret value used to sign the request,
that in turn converted into by a hex-encoded SHA-256 digest. At a high level the
algorithm in pseudo-code looks like this:

```text
secret := Hex(Sha256(BCrypt(password, salt)))
```

The `auth-hash-param-salt` header value from the `CONNECTED` frame will
determine the BCrypt salt that must be used to digest the user's plain-text
password. The salt will take the form of

```text
$2a$10$1234567890123456789012
```

Here `$2a` indicates the version of BCrypt used, `$10` indicates the number of
iterations used, and everything after the final `$` is the Base64 encoded salt
used.

### BCrypt secret example

The
[SnsAuthorizationBuilder](https://github.com/SolarNetwork/solarnetwork-common/blob/develop/net.solarnetwork.common/src/net/solarnetwork/security/SnsAuthorizationBuilder.java)
class can be used to generate the required `authorization` header value. See
[this
example](https://github.com/SolarNetwork/solarnetwork-node/blob/0d387a6ceb973c88c87e45ac7d0cd9a0bc95ba02/net.solarnetwork.node.setup.stomp.test/src/net/solarnetwork/node/setup/stomp/test/StompSetupServerHandlerTests.java#L260-L308)
for more details; here is that example distilled:

```java
String salt = "$2a$10$upVbEZHge9Iph1NN3L6ENO"; // from auth-hash-param-salt CONNECTED header
String secret = DigestUtils.sha256Hex(BCrypt.hashpw("password123", salt));
SnsAuthorizationBuilder authBuilder = new SnsAuthorizationBuilder("me@example.com")
		.date(now)
		.verb("SEND")
		.path("/setup/authenticate");
String authHeader = authBuilder.build(secret);
String dateHeader = authBuilder.headerValue("date");
```

# Subscribing to Setup topics

Once successfully authenticated, a client can subscribe to the `/setup/**` wild
card topic to receive messages from the server. Here is an example `SUBSCRIBE`
frame:

```text
SUBSCRIBE
id:0
destination:/setup/**

^@
```

The `/setup/**` subscription receives replies to `SEND` commands. It does
**not** receive [live datum](#live-datum-streaming) messages, which are only
ever delivered to the specific `/setup/datum/live` subscription that requested
them.

## Unsubscribing and disconnecting

Send an `UNSUBSCRIBE` frame with the `id` of a previous subscription to remove
it. Unknown IDs are ignored.

```text
UNSUBSCRIBE
id:0

^@
```

Send a `DISCONNECT` frame to close the connection. If a `receipt` header is
included, the server replies with a `RECEIPT` frame first.

```text
DISCONNECT
receipt:bye

^@
```

A `receipt` header on `SUBSCRIBE`, `UNSUBSCRIBE`, or `DISCONNECT` frames is
acknowledged with a `RECEIPT` frame whose `receipt-id` header matches it.

# Setup command processing

The Setup STOMP server will handle commands via `SEND` frames posted by the
client and send the result as a `MESSAGE` frame using the same `destination`
header value as used in the original `SEND` frame. Commands are processed in an
asynchronous fashion, so multiple commands can be active at once, and the order
of their replies are undefined. Clients can keep track of `SEND` and `MESSAGE`
pairs by including a unique `request-id` header value in each `SEND` frame. The
server will include that same header in the associated `MESSAGE` response frame.

For example, here is a `SEND` frame to execute the `/setup/datum/latest`
command:

```text
SEND
destination:/setup/datum/latest
request-id:1

^@
```

Here is an example `MESSAGE` response frame for that request:

```text
MESSAGE
destination:/setup/datum/latest
status:200
message-id:26188729
subscription:0
request-id:1
content-type:application/json;charset=utf-8
content-length:159

[{"created":"2021-08-19 02:30:10.005Z","sourceId":"Mock Energy Meter","i":{"voltage":234.99959,"frequency":50.499973,"watts":11214},"a":{"wattHours":6118188}}]^@
```

# Live datum streaming

A client can subscribe to a live stream of datum properties for a single source,
for example to show a live chart of a meter's power, current, and voltage while
commissioning. Subscribe to the `/setup/datum/live` destination (exactly;
patterns are not supported) with these headers:

| Header        | Required | Description                                                                                                                                                                                                     |
|:--------------|:---------|:----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `id`          | yes      | The subscription ID. Must be unique within the session.                                                                                                                                                         |
| `destination` | yes      | Must be `/setup/datum/live`.                                                                                                                                                                                    |
| `source-id`   | yes      | The source ID to stream, e.g. `/GEN/1`. Source IDs are the ones data sources produce, **before** any datum filters are applied.                                                                                 |
| `properties`  | no       | A comma-delimited list of datum property names to include, e.g. `watts,current,voltage,powerFactor`. If omitted, all instantaneous and accumulating properties are included. At most 32 properties are allowed. |
| `interval`    | no       | The minimum number of milliseconds between messages. Clamped to between `1000` (the default) and `60000`.                                                                                                       |
| `receipt`     | no       | If provided, a `RECEIPT` frame is sent once the frame is processed.                                                                                                                                             |

```text
SUBSCRIBE
id:live-1
destination:/setup/datum/live
source-id:/GEN/1
properties:watts,current,voltage,powerFactor,frequency,current_a,current_b,current_c
interval:1000

^@
```

The server then publishes `MESSAGE` frames to **that subscription only**. A
message is published when a new datum for the source is captured, provided its
timestamp is later than the last one sent and roughly `interval` milliseconds
have passed (up to 20% early is tolerated, to allow for scheduling jitter).

The body is a JSON object with a `t` property holding the datum timestamp, as
milliseconds since the epoch, plus each requested property that is present in
the datum. Missing properties are left out. The `t` name is reserved, so a datum
property named `t` is never included.

```text
MESSAGE
destination:/setup/datum/live
subscription:live-1
message-id:12
source-id:/GEN/1
status:200
content-type:application/json;charset=utf-8
content-length:83

{"t":1759900000123,"watts":-1520,"current":6.4,"voltage":239.8,"powerFactor":-0.97}^@
```

If the latest datum for the source is already known when subscribing, it is
published straight away. That datum only counts towards the 404 check below if
it is less than 30 seconds old. Send an `UNSUBSCRIBE` frame with the same `id`
to stop the stream. Closing the connection also stops it.

## Live datum status messages

Problems are reported as `MESSAGE` frames on the live subscription with a
`status` header and a `message` header describing the problem, never as an
`ERROR` frame (which would close the connection). A *terminal* status means the
subscription has ended (or was never created) and the client must subscribe
again to resume.

| Status | Terminal                          | Description                                                                                                                                                                                                                                         |
|:-------|:----------------------------------|:----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `200`  | no                                | A datum message.                                                                                                                                                                                                                                    |
| `404`  | yes                               | No datum was received for `source-id` within 15 seconds of subscribing.                                                                                                                                                                             |
| `410`  | yes                               | The subscription reached its maximum duration (15 minutes by default).                                                                                                                                                                              |
| `422`  | yes                               | The `source-id` header is missing, the `interval` header is not a number, too many properties were requested, or the `id` is already in use for a live subscription (which is then ended too).                                                      |
| `429`  | yes                               | Too many live subscriptions: at most 2 per session and 4 in total.                                                                                                                                                                                  |
| `503`  | at subscribe, yes; afterwards, no | At subscribe time, live datum is not available. While streaming, no datum has been received for 30 seconds (or three times the `interval`, if larger); this is advisory, and messages resume when datum does. Also sent when the server shuts down. |

## How live datum is collected

SolarNode data sources are usually only polled about once a minute. To get datum
more often, while at least one live subscription exists the server enables an
**operational mode**, `setup-live` by default (see the **Live Mode** setting).
The mode is enabled with an expiration date that is extended while subscriptions
remain, and is disabled 10 seconds after the last subscription ends.

The polling itself is done by an [Operational Mode Data Source
Scheduler](https://github.com/SolarNetwork/solarnetwork-node/tree/develop/net.solarnetwork.node.datum.opmode),
which must be configured to poll data sources while the mode is active,
**without** persisting the datum. The `solarnode-config-setup-live` package
provides such a configuration: mode `setup-live`, every data source, every
second, not persisted. Without a scheduler like this, live messages only arrive
at each data source's normal schedule.

# SolarNode setup command handling

Internally, each STOMP `SEND` setup command will be converted to an
`Instruction` object and offered to all
[`InstructionHandler`](https://github.com/SolarNetwork/solarnetwork-node/blob/develop/net.solarnetwork.node/src/net/solarnetwork/node/reactor/InstructionHandler.java)
services registered at runtime. The first handler to return a non-`null` result
status will cause the Setup STOMP server to convert the result into a `MESSAGE`
response and and publish that to the client.

The `Instruction` topic will be set to `SystemConfigure`. The `destination`
header from the `SEND` frame will be provided as the `service` instruction
parameter, along with all custom frame headers converted to instruction
parameters of the same name. Any `SEND` frame content will be assumed to be a
UTF-8 string and will be set as the `arg` instruction parameter. **Note** this
will be the raw string value, it will not be parsed in any way. The `SEND` frame
`content-type` header will be provided as an instruction parameter so the
handler can see what the content can be interpreted as.

A `MESSAGE` frame `status` header will be set according to the
`InstructionState` returned by the handler:

| InstructionState | Status value | Description                                                                              |
|:-----------------|:-------------|:-----------------------------------------------------------------------------------------|
| `Completed`      | `200`        | The command was executed successfully.                                                   |
| `Executing`      | `202`        | The command is executing asynchronously.                                                 |
| *null*           | `404`        | No handler accepted processing the command.                                              |
| `Declined`       | `422`        | The command was recognized but not executed because of a client problem.                 |
| *exception*      | `500`        | The handler threw an exception. The `message` header will contain the exception message. |

The instruction handler can override this default mapping by returning a
`statusCode` result parameter with an integer value. Additional the handler can
provide a `message` result parameter to pass back in the `MESSAGE` frame
returned to the client.

## Example SolarNode command handler

Here is an example `FeedbackInstructionHandler` snippet, that responds to a
`/setup/hello` command and returns a string result `Hi there!`:

```java
public boolean handlesTopic(String topic) {
  return InstructionHandler.TOPIC_SYSTEM_CONFIGURE.equals(topic);
}

public InstructionStatus processInstruction(Instruction instruction) {
  if ( instruction == null || !handlesTopic(instruction.getTopic()) ) {
    return null;
  }
  final String topic = instruction.getParameterValue(InstructionHandler.PARAM_SERVICE);
  if ( !"/setup/hello".equals(topic) ) {
    return null;
  }
  final String result = "Hi there!";
  return InstructionStatus.createStatus(instruction, InstructionState.Completed, Instant.now(),
      Collections.singletonMap(InstructionHandler.PARAM_SERVICE_RESULT, result));
}
```

To have this handler invoked, a client would post a `SEND` frame like this:

```text
destination:/setup/hello
request-id:2

^@
```

The Setup STOMP server would post a `MESSAGE` back to the client like this:

```text
destination:/setup/hello
status:200
message-id:1234567
subscription:0
request-id:2
content-type:application/json;charset=utf-8
content-length:11

"Hi there!"^@
```

> :warning: **Note** how the response is a JSON string, enclosed in
> double-quotes. All messages returned from the server will be encoded into
> JSON.

