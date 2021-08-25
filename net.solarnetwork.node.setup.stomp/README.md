# SolarNode Setup: STOMP

This project contains a SolarNode plugin that provides a [STOMP][stomp] server that enables setup
and configuration tasks in an external application. A primary use case for this plugin is to enable
an application on a Bluetooth-enabled phone to provide an easy-to-use UI for performing basic
SolarNode setup tasks, such as confirming that SolarNode is connected to the internet.

STOMP is a well-established and simple text-based bi-directional communication protocol that has similarities
to the structure of HTTP 1.x requests. A STOMP message is called a _frame_ and is structured as lines
of text, each line ending with an `EOL` (`\n` or ASCII `0x10`) like:

```
COMMAND
header1:value1
header2:value2

Body^@
```

The `COMMAND` is like a HTTP verb, and is one of the values defined in the STOMP standard. The
command is followed by zero or more header key/value pairs, much like HTTP headers. A blank line
follows that, followed by zero or more characters  representing the message body, finished with a
`NULL` byte, represented by `^@` for <kbd>Ctrl-@</kbd>.


# Connecting

To connect to the SolarNode STOMP Setup server open a TCP/IP socket connection to the host name or
IP address of SolarNode, using the port configured in the server's settings (the default is `8780`).
You must then send a `CONNECT` or `STOMP` frame.

## `CONNECT` frame

The `CONNECT` frame is sent by the client and used to initiate a new _setup session_. It must be the
first frame sent by the connected client. The following frame headers are required:

| Header | Description |
|:-------|:------------|
| `accept-version` | Only `1.2` is allowed. |
| `host` | The host name or IP address of SolarNode. |
| `login` | The SolarNode login to use for the session. This login will be authenticated later. |

Upon successful receipt of a `CONNECT` frame the server will send a [`CONNECTED`](#connected-frame)
frame to the client.

An example `CONNECT` frame looks like this:

```
CONNECT
accept-version:1.2
host:solarnode
login:solar

^@
```

## `CONNECTED` frame

The `CONNECTED` frame is sent by the server and used to indicate a new _setup session_ has been 
successfully started. After receipt of this frame a client must [authenticate](#authenticating).
The following frame headers will be returned:

| Header | Description |
|:-------|:------------|
| `version` | The STOMP version accepted by the server. Will be `1.2`. |
| `server` | The setup server name and version. |
| `session` | The unique ID of the setup session. |
| `message` | A request to authenticate. |
| `authenticate` | The required authentication scheme. Will be `SNS`. |
| `auth-hash` | The password digest algorithm to use. |
| `auth-hash-param-*` | Any number of password digest algorithm parameters to use. |

The only value currently supported for `auth-hash` is `bcrypt`, and the only parameter returned
will be `auth-hash-param-salt`. This salt value must be used when [authenticating](#authenticating).

An example `CONNECTED` frame looks like this:

```
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

Once a client receives a `CONNECTED` frame, it must authenticate the session, using the scheme
specified in the `authenticate` header. Authentication is performed by sending a `SEND` frame with a
`destination` header value of `/setup/authentication` and an `authorization` header with the
appropriate credentials, the syntax of which depends on the scheme used. **Note** that the scheme
might require additional headers. The only supported scheme at this time is `SNS`, described in the
next section.

After publishing the `SEND` authentication frame, if the authentication is successful nothing will
happen and the client application can move on to 
[subscribing to the setup topic](#subscribing-to-setup-topic) to start interacting with the Setup
Server. If the authentication fails, the server will send an `ERROR` frame to the client and close
the connection.

## SNS authentication scheme

The `SNS` scheme is loosely based on the [SNWS2][snws2] scheme used by SolarNetwork, which is itself
loosely based on the [AWS Signature Version 4][s3-sigv4] scheme. At a high level, the authentication
is performed using a HMAC+SHA256 digest of various parts of the STOMP frame, signed using a secret
key derived from the SolarNode user's hashed password. SolarNode does not store a plain-text version
of a user's password, so that is why the hashed password is used for the signing key.

The required `SEND` headers for SNS authentication are:

| Header | Description |
|:-------|:------------|
| `authorization` | The SNS authorization value, e.g. `SNS Credential=me@example.com,SignedHeaders=date,Signature=168365...`. |
| `date` | The request date, e.g. `Mon, 16 Aug 2021 02:27:39 GMT`. |

TODO: document SNS scheme

## SNS authentication example

Here is a basic example in Java that uses the
[SnsAuthorizationBuilder][SnsAuthorizationBuilder.java] class to generate the required
`authorization` and `date` headers required by the SNS scheme:

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

The `auth-hash:bcrypt` `CONNECTED` header indicates that the BCrypt digest algorithm must be used to
derive the SNS secret value used to sign the request, that in turn converted into by a hex-encoded
SHA-256 digest. At a high level the algorithm in pseudo-code looks like this:

```
secret := Hex(Sha256(BCrypt(password, salt)))
```

The `auth-hash-param-salt` header value from the `CONNECTED` frame will determine the BCrypt salt
that must be used to digest the user's plain-text password. The salt will take the form of

```
$2a$10$1234567890123456789012
```

Here `$2a` indicates the version of BCrypt used, `$10` indicates the number of iterations used, and
everything after the final `$` is the Base64 encoded salt used.

### BCrypt secret example

The [SnsAuthorizationBuilder][SnsAuthorizationBuilder.java] class can be used to generate the
required `authorization` header value. See [this example][sns-auth-builder-example-java] for 
more details; here is that example distilled:

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

Once successfully authenticated, a client can subscribe to the `/setup/**` wild card topic to
receive messages from the server. Here is an example `SUBSCRIBE` frame:

```
SUBSCRIBE
id:0
destination:/setup/**

^@
```

# Setup command processing

The Setup STOMP server will handle commands via `SEND` frames posted by the client and send the
result as a `MESSAGE` frame using the same `destination` header value as used in the original
`SEND` frame. Commands are processed in an asynchronous fashion, so multiple commands can be 
active at once, and the order of their replies are undefined. Clients can keep track of `SEND` and
`MESSAGE` pairs by including a unique `request-id` header value in each `SEND` frame. The server
will include that same header in the associated `MESSAGE` response frame.

For example, here is a `SEND` frame to execute the `/setup/datum/latest` command:

```
SEND
destination:/setup/datum/latest
request-id:1

^@
```

Here is an example `MESSAGE` response frame for that request:

```
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

# SolarNode setup command handling

Internally, each STOMP `SEND` setup command will be converted to an `Instruction` object and offered
to all [`FeedbackInstructionHandler`][FeedbackInstructionHandler.java] services registered at
runtime. The first handler to return a non-`null` result status will cause the Setup STOMP server to
convert the result into a `MESSAGE` response and and publish that to the client.

The `Instruction` topic will be set to `SystemConfigure`. The `destination` header from the `SEND`
frame will be provided as the `service` instruction parameter, along with all custom frame headers
converted to instruction parameters of the same name. Any `SEND` frame content will be assumed to be
a UTF-8 string and will be set as the `arg` instruction parameter. **Note** this will be the raw
string value, it will not be parsed in any way. The `SEND` frame `content-type` header will be
provided as an instruction parameter so the handler can see what the content can be interpreted as.

 A `MESSAGE` frame `status` header will be set according to the `InstructionState` returned by the
 handler:
 
 | InstructionState | Status value | Description |
 |:-----------------|:-------------|:------------|
 | `Completed`      | `200`        | The command was executed successfully. |
 | `Executing`      | `202`        | The command is executing asynchronously. |
 | _null_           | `404`        | No handler accepted processing the command. |
 | `Declined`       | `422`        | The command was recognized but not executed because of a client problem. |
 | _exception_      | `500`        | The handler threw an exception. The `message` header will contain the exception  message. |
 
 The instruction handler can override this default mapping by returning a `statusCode` result
 parameter with an integer value. Additional the handler can provide a `message` result parameter
 to pass back in the `MESSAGE` frame returned to the client.
 
## Example SolarNode command handler

Here is an example `FeedbackInstructionHandler` snippet, that responds to a `/setup/hello` command
and returns a string result `Hi there!`:

```java
public boolean handlesTopic(String topic) {
  return InstructionHandler.TOPIC_SYSTEM_CONFIGURE.equals(topic);
}

public InstructionStatus processInstructionWithFeedback(Instruction instruction) {
  if ( instruction == null || !handlesTopic(instruction.getTopic()) ) {
    return null;
  }
  final String topic = instruction.getParameterValue(InstructionHandler.PARAM_SERVICE);
  if ( !"/setup/hello".equals(topic) ) {
    return null;
  }
  final String result = "Hi there!";
  return InstructionStatus.createStatus(instruction, InstructionState.Completed, new Date(),
      Collections.singletonMap(InstructionHandler.PARAM_SERVICE_RESULT, result));
}
```

To have this handler invoked, a client would post a `SEND` frame like this:

```
destination:/setup/hello
request-id:2

^@
```

The Setup STOMP server would post a `MESSAGE` back to the client like this:

```
destination:/setup/hello
status:200
message-id:1234567
subscription:0
request-id:2
content-type:application/json;charset=utf-8
content-length:11

"Hi there!"^@

```

> :warning: **Note** how the response is a JSON string, enclosed in double-quotes. All messages 
> returned from the server will be encoded into JSON.

[FeedbackInstructionHandler.java]: https://github.com/SolarNetwork/solarnetwork-node/blob/develop/net.solarnetwork.node/src/net/solarnetwork/node/reactor/FeedbackInstructionHandler.java
[s3-sigv4]: https://docs.aws.amazon.com/AmazonS3/latest/API/sig-v4-authenticating-requests.html
[SnsAuthorizationBuilder.java]: https://github.com/SolarNetwork/solarnetwork-common/blob/develop/net.solarnetwork.common/src/net/solarnetwork/security/SnsAuthorizationBuilder.java
[sns-auth-builder-example-java]: https://github.com/SolarNetwork/solarnetwork-node/blob/0d387a6ceb973c88c87e45ac7d0cd9a0bc95ba02/net.solarnetwork.node.setup.stomp.test/src/net/solarnetwork/node/setup/stomp/test/StompSetupServerHandlerTests.java#L260-L308
[snws2]: https://github.com/SolarNetwork/solarnetwork/wiki/SolarNet-API-authentication-scheme-V2
[stomp]: https://stomp.github.io/
