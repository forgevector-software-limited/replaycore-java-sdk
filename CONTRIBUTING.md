# Contributing

Thank you for your interest in the ReplayCore Java SDK. This project is maintained
by ForgeVector Software Limited; contributions that improve correctness, clarity,
and developer experience are welcome.

## Ground rules

- **Java 8 compatibility is non-negotiable.** The Minecraft plugin ecosystem still
  ships servers on Java 8. All production code must compile and run on Java 8;
  do not use APIs introduced in Java 9+ in `src/main`. The build pins this with a
  Java 8 toolchain target.
- **Zero runtime dependencies.** The SDK must remain dependency-free at runtime so
  it can be shaded into a plugin without classpath conflicts. Only test-scoped
  dependencies (JUnit) are permitted.
- **No live network in tests.** Unit tests must use the in-memory `HttpTransport`
  and never call the real API.
- **Wrap only the real, key-authed surface.** Do not add methods for endpoints a
  customer API key cannot call. If you add a method for a newly-shipped endpoint,
  cite it and cover it with tests against a mocked response.

## Development

Build, test, and document:

```bash
./gradlew build      # compile + test + assemble jars
./gradlew test       # tests only
./gradlew javadoc    # generate API docs
```

A Maven build is also available:

```bash
mvn verify
```

The build treats compiler warnings as errors and requires Javadoc to generate
cleanly, so a green build means the public surface is documented and lint-clean.

## Coding standards

- Public types and methods carry complete Javadoc: a description sentence plus
  `@param`, `@return`, and `@throws` as applicable.
- Model types are immutable; mutable input is assembled through validating
  builders.
- Validate caller input early and throw `IllegalArgumentException` for programmer
  errors; reserve the `ReplayCoreException` hierarchy for remote failures.
- Keep comments purposeful — explain *why*, not *what the code obviously does*.
- Every source file begins with the standard copyright header (see existing
  files).

## Tests

- Add or update tests for any behavioural change.
- Cover both the happy path and the error mapping (status → exception type).
- Run the full suite before opening a change.

## Submitting changes

1. Create a topic branch.
2. Make the change with tests and Javadoc.
3. Ensure `./gradlew build` is green.
4. Open a pull request describing the change and, for any new endpoint coverage,
   the endpoint it wraps.

## Reporting issues

Include the SDK version, your Java version, a minimal reproduction, and the full
exception (type, status code, and `code`) — never include your API key.
