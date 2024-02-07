# Release process

```bash
mvn versions:set -DnewVersion=<NEW-VERSION>
```

Commit and tag the version. Then push and create a **GitHub release**.

Finally, publishing to Nexus and Maven Central is **automatically triggered by creating a GitHub release** using GitHub Actions.

```bash
mvn versions:set -DnewVersion=<NEW-VERSION With Minor +1 and -SNAPSHOT>
```

Commit and push.

## GitHub Actions Requirements

Publish requires following secrets:

* `OSSRH_USERNAME` the Sonatype username
* `OSSRH_TOKEN` the Sonatype token
* `OSSRH_GPG_SECRET_KEY` the gpg private key used to sign packages
* `OSSRH_GPG_SECRET_KEY_PASSWORD` the gpg private key password

These are stored in GitHub organisation's secrets.
