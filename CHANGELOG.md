# Changelog

All notable changes to this project are documented here. The format follows
[Keep a Changelog](https://keepachangelog.com/en/1.1.0/) and versions follow
[Semantic Versioning](https://semver.org/).

## [0.1.0] - Unreleased

First release, targeting the Unicom360 API contract v0.1.0.

### Added

- `Unicom360Client` (synchronous, Java 17 or later, Gson as the only dependency, a JPMS module
  `com.unicom360`) with every operation of the public API: `client.sms().send`,
  `client.email().send`, `client.whatsapp().sendTemplate`, `sendSession`, `status`, `templates`,
  `client.balance().get`, `all` and `client.health()`.
- Immutable result records that keep unknown fields (`extra()`) and the parsed JSON (`raw()`), with
  lookup helpers (`byNumber`, `byAddress`, `byId`, `byName`, `accepted`, `invalid`, `rejected`).
  Balances are `BigDecimal`.
- Builders for every request (`SmsSendParams`, `EmailSendParams`, `TemplateSendParams`,
  `TemplateRecipient`, `TemplateHeader`, `TemplateBody`, `TemplateButton`) and session messages
  (`SessionMessage`, `ContactCard`, `ReplyButton`, `ListSection`, `ListRow`).
- The unchecked exception hierarchy rooted at `Unicom360Exception`, with error codes normalised from
  the gateway's messages (`ErrorCodes`).
- Safe automatic retries (429, failures before sending, and 5xx, failures after sending and
  `SEND_FAILED` on retry-safe calls only), with UTC-minute waits after a 429 and full-jitter backoff
  otherwise; the `autoReferenceIds` option; per-call overrides with `RequestOptions`.
- Webhook verification and parsing (`Webhooks.verify`, `constructEvent`) and the unsigned SMS and
  email delivery reports (`parseSmsCallback`, `parseEmailCallback`). A `null`, empty or
  whitespace-only secret throws `ConfigurationException` before any other check, because anyone can
  forge a signature with an empty key.
- Opt-in GSM-7 helpers (`Gsm`).
- A pluggable `Transport` with a default on `java.net.http.HttpClient` that never follows redirects
  and bounds each attempt from sending to the end of the body.
- Logging through `System.getLogger("com.unicom360")`, with the API key and webhook secrets redacted
  everywhere.
- The contract's conformance suite (v0.1.0), a Prism contract test, examples for every operation and
  webhook receivers for Spring Boot and `com.sun.net.httpserver`.

[0.1.0]: https://github.com/TechRode-Solution-Limited/unicom360-java/releases/tag/v0.1.0
