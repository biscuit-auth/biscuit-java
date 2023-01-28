# biscuit-java

[![Tests](https://github.com/biscuit-auth/biscuit-java/actions/workflows/java_ci.yml/badge.svg)](https://github.com/biscuit-auth/biscuit-java/actions/workflows/java_ci.yml)

[![Central Version](https://img.shields.io/maven-central/v/org.biscuitsec/biscuit)](https://mvnrepository.com/artifact/org.biscuitsec/biscuit)
[![Nexus Version](https://img.shields.io/nexus/r/org.biscuitsec/biscuit?server=https%3A%2F%2Fs01.oss.sonatype.org)](https://search.maven.org/artifact/org.biscuitsec/biscuit)

[Biscuit's](https://github.com/biscuit-auth/biscuit) Java library implementation.

This API implements [Biscuit 2.0](https://www.biscuitsec.org/blog/biscuit-2-0/).

## Usage

```java
<!-- https://mvnrepository.com/artifact/org.biscuitsec/biscuit -->
<dependency>
    <groupId>org.biscuitsec</groupId>
    <artifactId>biscuit</artifactId>
    <version>@VERSION@</version>
</dependency>
```

## Development

### Requirements

* JDK v11
* the Protobuf compiler command `protoc` v3+ is required in `$PATH`.

### Build

```bash
mvn clean install
# skip tests
mvn clean install -DskipTests
```

## Publish

### Release process

```bash
mvn versions:set -DnewVersion=<NEW-VERSION>
```

Commit and tag the version. Then push and create a **GitHub release**.

Finally, publishing to Nexus and Maven Central is **automatically triggered by creating a GitHub release** using GitHub Actions.

```bash
mvn versions:set -DnewVersion=<NEW-VERSION With Minor +1 and -SNAPSHOT>
```

Commit and push.

### GitHub Actions Requirements

Publish requires following secrets:

* `OSSRH_USERNAME` the Sonatype username
* `OSSRH_TOKEN` the Sonatype token
* `OSSRH_GPG_SECRET_KEY` the gpg private key used to sign packages
* `OSSRH_GPG_SECRET_KEY_PASSWORD` the gpg private key password

These are stored in GitHub organisation's secrets.
