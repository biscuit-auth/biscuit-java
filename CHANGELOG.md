# Biscuit Java CHANGELOG

## v2.1.0

### introduce new symbols and symbols' offset

In order to reduce the token serialized size. The news symbols have been added:

```txt
"read",
"write",
"resource",
"operation",
"right",
"time",
"role",
"owner",
"tenant",
"namespace",
"user",
"team",
"service",
"admin",
"email",
"group",
"member",
"ip_address",
"client",
"client_ip",
"domain",
"path",
"version",
"cluster",
"node",
"hostname",
"nonce",
"query"
```

These are the defaultSymbols provided in each Biscuit implementation. An offset has been created for the custom symbols starting at index 1024. This enable Biscuit spec to add default symbols without breaking existing tokens using `v2.1.0+`.

### introduce `UnverifiedBiscuit`

It provides tooling on token like attenuation without creating a `Biscuit` as Biscuit creation checks the token signatures which requires public key.

### introduce `AuthorizedWorld`

`Authorizer.authorize()` returns the `AuthorizedWorld` which has a `queryAll` method to fetch facts matching a rule and query the `AuthorizedWorld`.

### common usage

Considering a Base64 `RFC4648_URLSAFE` token,

* parse it as `UnverifiedBiscuit unverifiedBiscuit`;
* verify its signatures using and create `Biscuit` using `Biscuit biscuit = unverifiedBiscuit.verify(publicKey)`;
* create `Authorizer` from `biscuit` using `Authorizer authorizer = biscuit.authorizer()`;
* add context to authorize the token (facts, rules, checks);
* authorize using `Tuple2<Long, AuthorizedWorld> authorizedResult = authorizer.authorize()`.