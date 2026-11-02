# Contributing

Thanks for helping. Issues and pull requests are welcome at
<https://github.com/TechRode-Solution-Limited/unicom360-java>. Report security problems privately
instead: see [SECURITY.md](SECURITY.md).

Two rules apply to everything in this repository (code, comments, docs, commit messages): never
write the em dash character, and add no AI attribution lines to commits or pull requests.

## Set up

A JDK, 17 or later. Nothing else: `./mvnw` downloads Maven 3.9.9. The code compiles with
`--release 17` whatever JDK runs the build, so a newer JDK still produces Java 17 class files.

## Checks

CI runs all of these; run them before you push:

```sh
./mvnw spotless:check    # google-java-format; needs JDK 21 or later (spotless:apply fixes)
./mvnw verify            # enforcer, compile, every test, javadoc (doclint), jars
./mvnw install -DskipTests && ./mvnw -f examples/pom.xml verify   # the examples still compile
grep -rn --exclude-dir=.git --exclude-dir=target $'\xe2\x80\x94' . && echo "em dash found"
```

`./mvnw verify` fails on any javac lint warning and any javadoc warning. The formatter is not bound
to the lifecycle, because google-java-format needs JDK 21 while the build itself runs on JDK 17.

### Other JDKs

CI tests Temurin 17, 21 and 25. To run one locally without installing it, use its Docker image
(the source is copied, so the container never writes to your checkout; the named volume keeps the
Maven downloads between runs):

```sh
docker run --rm -v "$PWD":/src:ro -v unicom360-java-m2:/root/.m2 eclipse-temurin:17-jdk sh -c '
  cp -r /src /work && cd /work && rm -rf target && ./mvnw -B -ntp verify'
```

### The contract test

`ContractTest` sends every call in `requests.json` to a
[Prism](https://stoplight.io/open-source/prism) mock of `openapi.yaml`, through the default
transport, and fails on anything but a 2xx without an `sl-violations` header. It carries the JUnit
tag `contract`, which the default run excludes:

```sh
docker run --rm -p 4015:4010 -v /path/to/unicom360-openapi:/spec:ro stoplight/prism:5 \
  mock --multiprocess=false --errors -h 0.0.0.0 /spec/openapi.yaml
UNICOM360_CONTRACT_BASE_URL=http://127.0.0.1:4015 ./mvnw test -Dgroups=contract
```

(`--multiprocess=false` avoids a crash of the Prism 5 image at start-up.)

## The conformance fixtures

`src/test/resources/conformance/` is a vendored copy of `conformance/` from
[unicom360-openapi](https://github.com/TechRode-Solution-Limited/unicom360-openapi), pinned by the
tag and sha256 in its `VERSION`. Never edit it by hand: the suite recomputes the sha256 and fails on
any change. To move to a new contract version:

```sh
cd ../unicom360-openapi && git checkout vX.Y.Z
scripts/sync-conformance ../unicom360-java/src/test/resources/conformance
scripts/sync-conformance --check ../unicom360-java/src/test/resources/conformance
```

Then update `Unicom360.CONTRACT_VERSION`, make the suite pass, update the README's "targets" line
and Known differences, and commit the fixtures with the code. The fixtures never ship in the jar:
CI checks its entries.

Every vector of every fixture file runs through the public API; a vector the harness does not
understand fails, it is never skipped. When the contract adds a rule, extend
`src/test/java/com/unicom360/conformance/Harness.java` rather than filtering vectors.

## Releasing

Releases go to [Maven Central](https://central.sonatype.com) through the Sonatype Central Portal.

### One-time setup

1. Create an account on the [Central Portal](https://central.sonatype.com) and add the namespace
   `com.unicom360`. **The namespace must be verified first: the Portal gives you a code to publish
   as a DNS TXT record on `unicom360.com`.** Nothing can be published until it is verified.
2. Generate a Portal user token (Account, then "Generate User Token").
3. Create a GPG key for signing and publish its public part, for example:

   ```sh
   gpg --quick-generate-key "Techrode Solutions Limited <info@techrode.co>" rsa4096 sign 2y
   gpg --keyserver keyserver.ubuntu.com --send-keys <KEY_ID>
   gpg --armor --export-secret-keys <KEY_ID>   # the value of MAVEN_GPG_PRIVATE_KEY
   ```

4. Add four secrets to the GitHub repository:

   | Secret | Value |
   |---|---|
   | `CENTRAL_TOKEN_USERNAME` | the Portal token's user name |
   | `CENTRAL_TOKEN_PASSWORD` | the Portal token's password |
   | `MAVEN_GPG_PRIVATE_KEY` | the armored private key |
   | `MAVEN_GPG_PASSPHRASE` | its passphrase |

### Each release

1. Update `<version>` in `pom.xml` and `examples/pom.xml`, and `Unicom360.VERSION`.
2. In `CHANGELOG.md`, give the version's section today's date (`## [X.Y.Z] - YYYY-MM-DD`) and add the
   link at the bottom. `release.yml` refuses an undated heading.
3. Commit, then tag and push the tag:

   ```sh
   git tag vX.Y.Z
   git push origin vX.Y.Z
   ```

4. `release.yml` checks that the tag matches the POM and that the CHANGELOG has a dated section, runs
   every check on Temurin 17, 21 and 25, runs `./mvnw -P release deploy` (which signs the artifacts
   and uploads them to the Portal), and creates the GitHub Release with the CHANGELOG section as its
   notes.
5. The profile uses `autoPublish=false` and `waitUntil=validated`: the upload stops at the Portal,
   validated but not published. **Open <https://central.sonatype.com/publishing>, inspect the
   deployment, and press Publish.** A published version can never be changed or removed.

### Publishing without GitHub Actions

When Actions is unavailable, publish from a clean checkout of the tag with Docker. Put the Portal
token in `~/.m2/settings.xml` under `<server><id>central</id>...</server>` and have the GPG key in
`~/.gnupg`:

```sh
git checkout vX.Y.Z
docker run --rm -it -v "$PWD":/src -v ~/.m2:/root/.m2 -v ~/.gnupg:/root/.gnupg -w /src maven:3.9-eclipse-temurin-21 mvn -P release deploy
```

Then press Publish on the Portal, and create the GitHub Release by hand from the CHANGELOG section.
