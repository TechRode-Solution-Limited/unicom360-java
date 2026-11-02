# Unicom360 Java SDK

The official Java client for the [Unicom360](https://unicom360.com) API: SMS, email and WhatsApp
over one JSON API.

* Java 17 or later. **Gson is the only dependency**; HTTP is `java.net.http`.
* A named module (`com.unicom360`), immutable records, builders, `BigDecimal` balances.
* Targets **Unicom360 API contract v0.1.0** (`openapi.yaml` 0.1.0 in
  [unicom360-openapi](https://github.com/TechRode-Solution-Limited/unicom360-openapi)), and runs
  that contract's full conformance suite in CI.
* API reference: <https://unicom360.com/docs>.

There is no sandbox: every accepted send is real and billed.

## Contents

* [Install](#install)
* [Quick start](#quick-start)
* [Every operation](#every-operation)
* [Results](#results)
* [Errors](#errors)
* [Retries](#retries)
* [autoReferenceIds](#autoreferenceids)
* [Webhooks](#webhooks)
* [SMS and email delivery reports](#sms-and-email-delivery-reports)
* [GSM-7 helpers](#gsm-7-helpers)
* [Configuration](#configuration)
* [Logging](#logging)
* [Known differences from the published docs](#known-differences-from-the-published-docs)
* [Versioning](#versioning)

## Install

Maven:

```xml
<dependency>
  <groupId>com.unicom360</groupId>
  <artifactId>unicom360-java</artifactId>
  <version>0.1.0</version>
</dependency>
```

Gradle:

```kotlin
implementation("com.unicom360:unicom360-java:0.1.0")
```

If your application uses the module path, add `requires com.unicom360;`. Its packages are
`com.unicom360`, `.params`, `.models`, `.events`, `.errors` and `.transport`.

## Quick start

```java
import com.unicom360.Unicom360Client;
import com.unicom360.models.SmsResult;
import com.unicom360.models.SmsSendResult;
import com.unicom360.params.SmsSendParams;
import java.util.List;

Unicom360Client client = Unicom360Client.fromEnv(); // reads UNICOM360_API_KEY
// or Unicom360Client.builder().apiKey("...").build()

SmsSendResult result = client.sms().send(SmsSendParams.builder()
    .from("Unicom360")
    .to(List.of("260971234567", "260961234567"))
    .message("Your verification code is 482193. It expires in 5 minutes.")
    .build());

for (SmsResult recipient : result.results()) {
  System.out.println(recipient.mobileNumber() + " " + recipient.status() + " " + recipient.messageId());
}
```

The client is immutable and safe to share between threads: build one and keep it. Every exception
it throws is unchecked. This version is synchronous; `CompletableFuture` variants may follow, so
for now call it from your own executor when you need that.

## Every operation

| Operation | Method | Returns |
|---|---|---|
| Send an SMS | `client.sms().send(params)` | `SmsSendResult` |
| Send an email | `client.email().send(params)` | `EmailSendResult` |
| Send a WhatsApp template | `client.whatsapp().sendTemplate(params)` | `TemplateSendResult` |
| Send a WhatsApp session message | `client.whatsapp().sendSession(message)` | `SessionSendResult` |
| WhatsApp message status | `client.whatsapp().status(messageIds)` | `StatusResult` |
| WhatsApp templates | `client.whatsapp().templates()` | `TemplateList` |
| One balance | `client.balance().get()`, `get(bundleType)` | `Balance` |
| Every balance | `client.balance().all()` | `Balances` |
| Health | `client.health()` | `Health` |

Every operation has an overload with a last `RequestOptions` argument, which overrides the client's
timeout (per attempt) and retries for that call:

```java
RequestOptions fast = RequestOptions.builder().timeout(Duration.ofSeconds(5)).maxRetries(0).build();
client.balance().all(fast);
client.sms().send(params, fast);
```

### SMS

```java
SmsSendResult result = client.sms().send(SmsSendParams.builder()
    .from("Unicom360")          // a registered sender ID
    .to("260971234567")         // one number, or to(List.of(...))
    .message("Your order ZM4821 is ready.") // GSM-7, at most 1,200 characters
    .userReferenceId("order-10421")         // optional: makes the send safe to retry
    .callbackUrl("https://example.com/webhooks/unicom360/sms") // optional
    .source("crm")              // optional label for your reporting
    .build());

System.out.println(result.apiReference() + " " + result.accepted().size());
```

What you pass is what is sent: `to(String)` sends a JSON string, `to(List)` a JSON array. The SDK
does no business validation of its own (lengths, GSM characters, recipient counts); the gateway
checks those and answers with an error. Builders only insist on the required fields, with a
`NullPointerException` from `build()`.

### Email

```java
EmailSendResult result = client.email().send(EmailSendParams.builder()
    .from("noreply@yourdomain.com")
    .to(List.of("jane@example.com", "john@example.com"))
    .subject("Your order is confirmed")
    .emailBody("<p>Thanks for your order. Order ZM4821 is confirmed.</p>")
    .replyTo("support@yourdomain.com")  // optional
    .callbackUrl("https://example.com/webhooks/unicom360/email") // optional
    .build());

Optional<EmailResult> jane = result.byAddress("jane@example.com");
```

### WhatsApp template

```java
TemplateSendResult result = client.whatsapp().sendTemplate(TemplateSendParams.builder()
    .template("order_ready")
    .from("260960123456")   // optional: your WhatsApp number
    .language("en_US")      // optional
    .addRecipient(TemplateRecipient.builder()
        .to("260971234567")
        .userReferenceId("inv-42")
        .header(TemplateHeader.image("https://example.com/receipts/ZM-4821.png"))
        .body(TemplateBody.of("Ada", "ZM-4821")) // or TemplateBody.named(Map.of(...))
        .addButton(TemplateButton.url(0, "ZM-4821")) // or TemplateButton.quickReply(...)
        .build())
    .addRecipient(TemplateRecipient.builder()
        .to("260961234567")
        .body(TemplateBody.of("Joseph", "ZM-4822"))
        .build())
    .build());

System.out.println(result.acceptedCount() + " queued, " + result.rejectedCount() + " rejected");
for (TemplateResult rejected : result.rejected()) {
  System.out.println(rejected.to() + ": " + rejected.reason());
}
```

Headers: `TemplateHeader.text(value)`, `image(url)`, `video(url)`, `document(url)`,
`document(url, filename)`. An `AUTHENTICATION` template takes the code as
`TemplateRecipient.builder().to(...).otp("482193")`.

A rejected recipient is not an error. When **every** recipient is rejected the call throws
`NoValidRecipientsException`, whose `results()` say why.

### WhatsApp session message

Inside the 24-hour customer service window, build a message with the `SessionMessage` factories.
Messages are immutable, and the optional fields are setters that return a copy:

```java
client.whatsapp().sendSession(
    SessionMessage.text("260971234567", "Your order is ready.").replyTo(inboundMessageId));

client.whatsapp().sendSession(
    SessionMessage.buttons(
        "260971234567",
        "Collect today or deliver?",
        List.of(ReplyButton.of("collect", "Collect"), ReplyButton.of("deliver", "Deliver"))));
```

| Factory | Sends |
|---|---|
| `SessionMessage.text(to, text)` | text |
| `.image(to, url)`, `image(to, url, caption)` | image |
| `.video(to, url)`, `video(to, url, caption)` | video |
| `.document(to, url)`, `document(to, url, caption)`, `document(to, url, caption, filename)` | document |
| `.audio(to, url)` | audio |
| `.sticker(to, url)` | sticker |
| `.buttons(to, body, buttons)`, `buttons(to, body, buttons, header, footer)` | up to 3 `ReplyButton.of(id, title)` |
| `.list(to, body, button, sections)`, `list(to, body, button, sections, header, footer)` | `ListSection.of(title, rows)` with `ListRow.of(id, title, description)` |
| `.location(to, latitude, longitude)`, `location(to, latitude, longitude, name, address)` | a location pin |
| `.contacts(to, contacts)` | `ContactCard.builder().formattedName(..).phone(..).build()` |
| `.reaction(to, messageId, emoji)` | a reaction; `emoji = ""` removes yours |

Every message has the setters `.from(..)`, `.userReferenceId(..)` and `.callbackUrl(..)`. All but a
reaction also have `.replyTo(..)`: `reaction` returns a `ReactionMessage`, which has no such method,
and the other factories a `ContentMessage`, both of them a `SessionMessage`.

`replyTo` and a reaction's `messageId` take either a `message_id` a send returned or the `wamid.` id
of an inbound message, exactly as a `message.received` webhook delivers it. Outside the window the
call throws `SessionWindowClosedException`: send a template instead.

### WhatsApp status and templates

```java
StatusResult status = client.whatsapp().status(List.of("7c1f3a2e-5b64-4d2f-9a8e-0f1b2c3d4e5f")); // 1 to 100 ids
status.byId("7c1f3a2e-5b64-4d2f-9a8e-0f1b2c3d4e5f")
    .ifPresent(entry -> System.out.println(entry.status())); // ACCEPTED, SENT, DELIVERED, READ, FAILED, REJECTED, NOT_FOUND

TemplateList templates = client.whatsapp().templates();
Optional<TemplateInfo> orderReady = templates.byName("order_ready", "en_US");
```

### Balance and health

```java
client.balance().get().balance();                     // BigDecimal: the SMS bundle
client.balance().get(BundleType.WHATSAPP).balance();  // SMS, SMS_INTERNATIONAL, EMAIL, WHATSAPP
client.balance().all().balances();                    // Map<String, BigDecimal>
client.health().status();                             // "ok"; sends no API key
```

Balances are `BigDecimal`, exactly as the gateway sent them.

Runnable versions of all of these are in [`examples/`](examples/).

## Results

Every result is an immutable record:

* fields named as in the API, in camelCase, read with accessors (`result.apiReference()`,
  `entry.messageId()`), `null` when absent or null in the response;
* `raw()`: the JSON object (Gson's `JsonObject`) it was parsed from, unmodified;
* `extra()`: every field the gateway sent that this version does not model, so a field added
  tomorrow is readable today.

Statuses, categories and reasons are plain strings: a value this version does not know passes
through. `messageId` is always a string. Lists are never `null`, only empty.

**SMS and email results are not in request order past the tenth recipient.** The SDK keeps the
response's order; find a recipient with the helpers, which return `Optional`:

| Result | Helpers |
|---|---|
| `SmsSendResult` | `accepted()`, `invalid()`, `byNumber(number)` |
| `EmailSendResult` | `accepted()`, `invalid()`, `byAddress(address)` |
| `TemplateSendResult` | `accepted()`, `rejected()`, `byNumber(number)`; counts in `acceptedCount()`, `rejectedCount()` |
| `StatusResult` | `byId(messageId)` |
| `TemplateList` | `byName(name)`, `byName(name, language)` |

`byNumber` compares digits only (`"+260 97 123 4567"` finds `"260971234567"`); it does not expand
local formats, so pass the international form. `byAddress` ignores case and surrounding spaces.

A partial success (an `Invalid mobile number` among the results, rejected template recipients) is a
result, never an exception.

## Errors

All in `com.unicom360.errors`, all unchecked:

```
Unicom360Exception
+-- ConfigurationException          no API key, an unusable option
+-- ApiConnectionException          no complete response (sent(), getCause())
|   +-- ApiTimeoutException
+-- ApiStatusException              any other non-2xx, or a 2xx that is not an envelope
|   +-- AuthenticationException     401
|   +-- RateLimitException          429 (retryAfter())
|   +-- InvalidRequestException     400: the request's shape is wrong
|   +-- ApiServerException          5xx
+-- ApiException                    HTTP 200 with success: false: a business refusal
|   +-- InsufficientBundleException
|   +-- DuplicateReferenceException (originalMessageId())
|   +-- NoValidRecipientsException  (results())
|   +-- SessionWindowClosedException
+-- SignatureVerificationException  a webhook failed verification (reason())
```

HTTP 200 is not success: a refusal such as an unknown sender ID arrives as HTTP 200 with
`success: false` and throws `ApiException`. Every exception has `getMessage()`, `type()` and
`attempts()`; `ApiStatusException` and `ApiException` add `httpStatus()`, `code()`, `apiReference()`
(quote it to support), `rawBody()`, `data()` and `headers()`.

```java
try {
  client.sms().send(params);
} catch (InsufficientBundleException e) {
  // top up
} catch (RateLimitException e) {
  System.out.println("retry in " + e.retryAfter().toSeconds() + " s");
} catch (ApiException e) {
  if (ErrorCodes.INVALID_SENDER_ID.equals(e.code())) {
    // ...
  }
  System.out.println(e.code() + " " + e.getMessage() + " " + e.apiReference());
}
```

`code()` is normalised from the gateway's message (`ErrorCodes.normalize`), so sentences and codes
compare the same way: `Invalid From Sender ID` and `Invalid Sender` are both `INVALID_SENDER_ID`,
`duplicate user reference id` and `DUPLICATE_MSG_REF` are both `DUPLICATE_REFERENCE`, and anything
unrecognised is `UNKNOWN`. The constants are in `ErrorCodes`: `INVALID_SENDER_ID`,
`SENDER_ID_NOT_ALLOWED`, `INVALID_RECIPIENT_LIMIT`, `INSUFFICIENT_BUNDLE`, `DUPLICATE_REFERENCE`,
`INVALID_PARAMETERS`, `SENDER_REQUIRED`, `TEMPLATE_NOT_FOUND`, `TEMPLATE_NOT_APPROVED`,
`LANGUAGE_REQUIRED`, `TEMPLATE_SENDER_MISMATCH`, `NO_VALID_RECIPIENTS`, `SEND_FAILED`,
`SESSION_WINDOW_CLOSED`, `REPLY_TO_NOT_FOUND`, `REACTION_TARGET_NOT_FOUND`, `INVALID_NUMBER`,
`INVALID_USER_REFERENCE_ID`, `MEDIA_URL_INVALID`, `CLIENT_NOT_FOUND`, `MISSING_API_KEY`,
`INVALID_AUTHORIZATION`, `AUTHENTICATION_FAILED`, `CREDENTIAL_DISABLED`, `CLIENT_DISABLED`,
`CREDENTIAL_EXPIRED`, `IP_NOT_ALLOWED`, `RATE_LIMIT_EXCEEDED`, `UNKNOWN`. A code this version does
not name passes through as a string.

**A `DuplicateReferenceException` with `attempts()` greater than 1 means an earlier attempt of the
same call very probably succeeded**: the retry was refused because that attempt had already used
the reference. Treat it as sent, and look it up rather than sending again.

The exceptions are sealed, so you cannot subclass them, but their constructors are public: build
real ones in your own tests with `new ErrorDetails(...)`.

## Retries

The client retries what is safe to retry, so a retry never sends a message twice:

| Outcome | Retried |
|---|---|
| HTTP 429 | always (it is answered before any processing) |
| No connection made (DNS, refused, TLS, connect timeout) | always |
| A failure after the request was sent, an HTTP 5xx, or `SEND_FAILED` | only if the call is retry-safe |
| HTTP 400, 401, any other 4xx, any other refusal | never |

Retry-safe calls: every read (`status`, `templates`, `balance`, `health`); an SMS or WhatsApp session
send with a `userReferenceId`; a template send where every recipient has one. **Email is never
retry-safe**, even with a reference: the gateway's duplicate check for email is not atomic, so it is
retried only after a 429 or a failure to connect.

Waits: after a 429, until the next UTC minute plus up to a second of jitter (the limit is a fixed
UTC-minute window and the gateway sends no `Retry-After`); otherwise full-jitter exponential backoff
(`random() * min(8, 0.5 * 2^attempt)` seconds). `maxRetries` (default 2) bounds the retries and
`maxRetryWait` (default 60 s) any single wait; a 429 wait longer than `maxRetryWait` throws at once.
The exception thrown is the last attempt's, with `attempts()` set. If the thread is interrupted
during a wait, that exception is thrown straight away with the interrupt flag kept.

```java
Unicom360Client client = Unicom360Client.builder().maxRetries(4).build();
client.balance().get(null, RequestOptions.builder().maxRetries(0).build()); // per call
```

## autoReferenceIds

```java
Unicom360Client client = Unicom360Client.builder().autoReferenceIds(true).build();
```

Off by default. When on, every send without a `userReferenceId` gets one before the first attempt
(`u360-` followed by 32 hex characters), reused on every retry, and each template recipient without
one gets its own. That makes SMS and WhatsApp sends retry-safe. A reference you pass is never
replaced. The generated values are in the request, in the debug log line of the request, and in the
result's `raw()` where the API echoes them (template and session results). For email the reference
still lets the gateway refuse a later repeat of the same send, but it does not make email retry-safe
(see above).

## Webhooks

WhatsApp events (`message.status`, `message.received`, `webhook.test`) are signed. Verify and parse a
delivery with `Webhooks.constructEvent`, passing the **raw body bytes**, the request's headers and
your webhook secret (the 43-character string from the portal, used as it is: never base64-decode
it):

```java
import com.unicom360.Webhooks;
import com.unicom360.errors.SignatureVerificationException;
import com.unicom360.events.*;

WebhookEvent event;
try {
  event = Webhooks.constructEvent(rawBody, headers, secret); // tolerance 300 s
} catch (SignatureVerificationException e) {
  // answer 401; e.reason() says which check failed
  return;
}

if (event instanceof MessageStatusEvent status) {
  System.out.println(status.data().messageId() + " " + status.data().status()); // sent, delivered, read, failed
} else if (event instanceof MessageReceivedEvent received) {
  System.out.println(received.data().from() + " " + received.data().type() + " " + received.data().text());
} else if (event instanceof TestEvent test) {
  System.out.println(test.data().message());
} else if (event instanceof UnknownEvent unknown) { // an event this version does not know; never throws
  System.out.println(unknown.event() + " " + unknown.data());
}
```

`WebhookEvent` is a sealed interface, so on Java 21 you can use a `switch` with record patterns.
`Webhooks.verify(rawBody, headers, secret)` only verifies. `headers` is either a
`Map<String, ?>` (values may be strings or lists; names are matched without regard to case; the
first value is used) or a `HeaderLookup` such as `request::getHeader`. Overloads take a `Duration`
tolerance and an `Instant` to verify at.

**Spring Boot.** Take the body as `byte[]`, because the signature covers the exact bytes received:

```java
@PostMapping("/webhooks/unicom360/whatsapp")
public ResponseEntity<String> whatsapp(
    @RequestBody byte[] rawBody, @RequestHeader HttpHeaders headers) {
  WebhookEvent event;
  try {
    event = Webhooks.constructEvent(rawBody, headers::getFirst, secret);
  } catch (SignatureVerificationException e) {
    return ResponseEntity.status(401).body(e.reason().value());
  }
  handle(event);
  return ResponseEntity.ok().build();
}
```

**`com.sun.net.httpserver`** (the JDK's own server):

```java
server.createContext("/webhooks/unicom360/whatsapp", exchange -> {
  byte[] rawBody = exchange.getRequestBody().readAllBytes();
  int status = 200;
  try {
    handle(Webhooks.constructEvent(rawBody, exchange.getRequestHeaders(), secret));
  } catch (SignatureVerificationException e) {
    status = 401;
  }
  exchange.sendResponseHeaders(status, -1);
  exchange.close();
});
```

**Servlets:** `Webhooks.constructEvent(request.getInputStream().readAllBytes(), request::getHeader, secret)`.

Full versions: [`examples/.../spring/WebhookController.java`](examples/src/main/java/examples/spring/WebhookController.java),
[`examples/.../WebhookHttpServer.java`](examples/src/main/java/examples/WebhookHttpServer.java).

Good to know:

* Deliveries are retried on any non-2xx (after 30 s, 2 min, 10 min, 30 min, 1 h, 3 h, 6 h and 12 h).
  Answer 2xx quickly and drop repeats by `event.deliveryId()`.
* `verify` rejects a timestamp more than the tolerance (300 s by default) from your clock, in either
  direction.
* In `message.received`: a media caption arrives in `text`, not in `media`; `payload` is set only for
  location, contacts (`{"contacts": [...]}`), reaction, sticker and unsupported messages; an
  interactive or button reply has the chosen title in `text`, `payload` is `null`, and `replyTo` is
  the `wamid.` id of your interactive message (the button or row id is not passed on).

## SMS and email delivery reports

The reports posted to an SMS or email `callback_url` are **unsigned and never retried**, so anyone
can post one: treat them as hints, and confirm anything that matters another way.

```java
SmsDeliveryReport sms = Webhooks.parseSmsCallback(rawBody);       // messageId, status, mobileNumber
EmailDeliveryReport email = Webhooks.parseEmailCallback(rawBody); // messageId, status, emailAddress, message
```

Both take the body as `byte[]` or `String`, and throw `Unicom360Exception` for a body that is not a
JSON object. The email report's numeric `message_id` becomes the string the send returned.

## GSM-7 helpers

The gateway accepts only GSM-7 text (the basic set plus `^{}\[~]|€`, no form feed), at most 1,200
characters, and bills `max(1, ceil(length / 160))` units per recipient (a CR LF pair counts as one
character). The SDK never checks this itself; these opt-in helpers give the gateway's answers before
you send:

```java
Gsm.isGsm7("Hello €");              // true
Gsm.length("Hi\r\nthere");          // 8
Gsm.units("a".repeat(161));         // 2
Gsm.nonGsmCharacters("It’s");       // [NonGsmCharacter[character=’, index=2]]
Gsm.validateSms("It’s ready");      // [GsmIssue.NON_GSM]; also EMPTY and TOO_LONG
```

`GsmIssue.value()` is the string `empty`, `too_long` or `non_gsm`. `Gsm.CHARSET`, `Gsm.MAX_LENGTH`
and `Gsm.UNIT_LENGTH` are the gateway's values. Code points are what count: an emoji is one
character.

## Configuration

```java
Unicom360Client client = Unicom360Client.builder()
    .apiKey("...")                        // default: UNICOM360_API_KEY
    .baseUrl("https://api.unicom360.com") // default: UNICOM360_BASE_URL, else this
    .timeout(Duration.ofSeconds(60))      // per attempt, sending to the end of the body
    .connectTimeout(Duration.ofSeconds(10)) // to connect, TLS included
    .maxRetries(2)
    .maxRetryWait(Duration.ofSeconds(60))
    .autoReferenceIds(false)
    .appInfo(new AppInfo("shopsync", "2.3.0", "https://shopsync.example"))
    .build();
```

* A missing or blank API key throws `ConfigurationException` when the client is built. An explicit
  `apiKey("")` does not fall back to the environment. A blank environment variable counts as unset.
* `baseUrl` must be `https`; plain `http` is allowed only for `localhost`, `127.0.0.0/8` and `::1`
  (for a local mock). A path prefix is kept, trailing slashes are dropped, and a query string or
  fragment is refused.
* Redirects are never followed, so the API key is never sent to another host.
* `client.withOptions(o -> o.timeout(Duration.ofSeconds(5)))` returns a copy with other options.
* `appInfo` is appended to the User-Agent
  (`unicom360-java/0.1.0 (Java 21.0.5; linux; amd64) shopsync/2.3.0 (+https://shopsync.example)`).
* The API key never appears in `client.toString()`, in exceptions or in logs.

**Transport.** The default transport uses `java.net.http.HttpClient`. To set a proxy, an
`SSLContext` or an executor, build your own client, which must not follow redirects:

```java
HttpClient http = HttpClient.newBuilder()
    .followRedirects(HttpClient.Redirect.NEVER)
    .proxy(ProxySelector.of(new InetSocketAddress("proxy.example.com", 8080)))
    .connectTimeout(Duration.ofSeconds(10))
    .build();
Unicom360Client client = Unicom360Client.builder().httpClient(http).build();
```

`Transport` is a functional interface, so any other HTTP library, or a fake in your tests, can
replace it with `.transport(request -> ...)`. Throw `TransportException(message, sent, timeout)` when
no response arrives, saying whether the request may have reached the server.

## Logging

The SDK logs through `System.getLogger("com.unicom360")` (or the `.logger(...)` you pass), which goes
to `java.util.logging` unless you bridge it to your own framework:

* `DEBUG`: one line per attempt with the method, path, status (or the transport failure),
  `api_reference`, attempt number and duration, plus any generated `user_reference_id`.
* `INFO`: one line per scheduled retry with the attempt, the wait and the cause.

Nothing else is logged: never a body, a header, a parameter, a phone number, an address, the API key
or a webhook secret.

## Known differences from the published docs

This SDK follows the gateway's actual behaviour (as written down in `openapi.yaml`). Where the API
reference at unicom360.com/docs says otherwise:

1. **Request-shape errors can carry several messages.** A 400 from the SMS or email endpoint can list
   one message per problem; `InvalidRequestException.getMessage()` joins them with `"; "`.
2. **WhatsApp balances are whole numbers today.** The docs say they can be fractional (`37.5`); the
   gateway currently rounds them. Balances are `BigDecimal`, so fractional values will work unchanged
   when the gateway sends them.
3. **Local number formats.** Only local numbers starting `095`, `096`, `097` and `076` are normalised;
   another local form (for example `077...`) comes back `Invalid mobile number`. Send international
   numbers (`2609...`).
4. **The SMS and email `callback_url` is not validated** by the gateway (the WhatsApp one is).
5. **The WhatsApp `callback_url` limit of 300 is in bytes**, so a non-ASCII URL reaches it sooner.
6. **A session `text` that is only whitespace is refused**, like an empty one.
7. **The health `timestamp` has microseconds** (`...30.412345Z`), not milliseconds.
8. **A session `DuplicateReferenceException` can have `originalMessageId()` null** when two requests
   race for the same reference.
9. **Rejected template recipients echo the request's values**, which can be numbers; the SDK always
   gives `to` and `userReferenceId` as strings.
10. **The balance endpoints can refuse with `CLIENT_NOT_FOUND`** (HTTP 200, `status: false`, no
    `api_reference`), thrown as `ApiException`.
11. **An email `user_reference_id` does not stop two overlapping requests** from both being accepted
    (a sequential repeat is refused). That is why email is never retried after the request may have
    reached the gateway.

## Versioning

The SDK follows [Semantic Versioning](https://semver.org/) and starts at 0.1.0; 1.0.0 follows once
live use shows no differences from the spec. `Unicom360.VERSION` is its version and
`Unicom360.CONTRACT_VERSION` the API contract it targets. Changes are listed in
[CHANGELOG.md](CHANGELOG.md). Security reports: see [SECURITY.md](SECURITY.md). Contributing: see
[CONTRIBUTING.md](CONTRIBUTING.md).

## Licence

MIT, see [LICENSE](LICENSE). Copyright (c) 2026 Techrode Solutions Limited.
