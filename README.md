# biscuit-java

[![Bintray Version](https://img.shields.io/bintray/v/clevercloud/maven/biscuit-java.svg)](https://bintray.com/clevercloud/maven/biscuit-java#)
[![Central Version](https://img.shields.io/maven-central/v/com.clever-cloud/biscuit-java)](https://mvnrepository.com/artifact/com.clever-cloud/biscuit-java)
[![Nexus Version](https://img.shields.io/nexus/r/com.clever-cloud/biscuit-java?server=https%3A%2F%2Foss.sonatype.org)](https://search.maven.org/artifact/com.clever-cloud/biscuit-java)

Java library for Biscuit usage.

## Publish

## Publish

You need to define this in `~/.m2/settings.xml` using your bintray APIKEY on the Clever Cloud organisation:

```xml
<server>
  <id>bintray-repo-maven-biscuit-java</id>
  <username>@@BINTRAY_USERNAME@</username>
  <password>@@YOUR_BINTRAY_API_KEY@@</password>
</server>
```

Then run

```bash
mvn deploy
```

It will prompt for GPG passphrase stored on Clever Cloud vault (search for `maven@clever-cloud.com`).

Then on bintray package homepage run Sync to Central to push to Maven Central.
