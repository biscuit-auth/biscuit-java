# biscuit-java

[![Tests](https://github.com/biscuit-auth/biscuit-java/actions/workflows/java_ci.yml/badge.svg)](https://github.com/biscuit-auth/biscuit-java/actions/workflows/java_ci.yml)

[![Central Version](https://img.shields.io/maven-central/v/org.biscuitsec/biscuit)](https://mvnrepository.com/artifact/org.biscuitsec/biscuit)
[![Nexus Version](https://img.shields.io/nexus/r/org.biscuitsec/biscuit?server=https%3A%2F%2Fs01.oss.sonatype.org)](https://search.maven.org/artifact/org.biscuitsec/biscuit)

[Biscuit's](https://github.com/biscuit-auth/biscuit) Java library implementation.

This API implements [Biscuit 2.0](https://www.biscuitsec.org/blog/biscuit-2-0/).

## Usage

```xml
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
* The Protobuf compiler command `protoc` v3+ is required in `$PATH`.

### Build

```bash
mvn clean install
# skip tests
mvn clean install -DskipTests
```
