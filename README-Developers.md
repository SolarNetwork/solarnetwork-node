# SolarNetwork SolarNode - Developer Info

## Testing

Tests are written as OSGi fragment bundles that target the bundle they want to test. They are not
actually tested in an OSGi runtime, however. The fragment approach allows IDEs like Eclipse to
resolve the test classpath. To run tests on the command line you typically execute `ant clean test`
in the test bundle directory:

```sh
ant clean test
```

The testing fragment bundles are always named after the bundle they target, with `.test` appended.

> :warning: **Note** the `net.solarnetwork.node.test` bundle is not a test project. It is a shared
> test-support library used by most test bundles. The `net.solarnetwork.node.internal.test` bundle
> is the fragment that targets the `net.solarnetwork.node` bundle.

### Test style

Tests are named `*Tests.java` and live in a `test` sub-package of the code under test (e.g.
`...domain.test`). They use JUnit 4, AssertJ, Mockito, and older tests EasyMock and Hamcrest. All
new tests should use BDD style AssertJ and Mockito. Except for very simple cases, comment the tests
into sections like:

```java
// GIVEN

// WHEN

// THEN
```

AssertJ method chains should be wrapped in formatter-disabled comments with minimized indentation like:

```java
// THEN
// @formatter:off
then(result)
    .as("Non-null result expected)
    .isNotNull()
    .as("Foo calculated")
    .returns(42, from(ResultClass::getFoo))
    ;
// @formatter:on
```

When both BDD AssertJ and BDD Mockito are used, then the `BDDMockito.then()` method should be
statically imported and the AssertJ `BDDAssertions.and` method should be statically imported; a
`@SuppressWarnings("static-access")` should be added to the test class to ignore warnings on the
`and.` usage.

Mockito `@Captor` argument captors should only be verified in the `// THEN` block. Tests of this
nature follow this pattern:

```java
// GIVEN
given(cache.get(TEST_NODE_ID)).willReturn(null);

// WHEN
...

// THEN
// @formatter:off
then(cache).should()
    .put(eq(TEST_NODE_ID), ownershipCaptor.capture())
    ;
and.then(ownershipCaptor.getValue())
    .as("Ownership info cached")
    .isSameAs(ownership)
    ;
// @formatter:on
```

### Other test info

- The sibling bundles they build from source in a `bundle.deps` dirset in `build.xml`.
- **Use `ant clean test`, not `ant test`, after changing main sources.** The test project caches the
  jar of the bundle under test in `build/ant/deps/` and rebuilds it only when it is missing, so
  `ant test` runs against stale code.
- **`BUILD SUCCESSFUL` does not mean the tests passed.** `<junit>` has no `haltonfailure` and prints
  nothing per test. Check `target/reports/TEST-*.xml` for `errors="0" failures="0"`, or run
  `ant test-report`, which fails on any failure and writes `target/reports/unittest/index.html`.
- All `*Test.java` and `*Tests.java` classes run; Ant has no option for a single class. After an
  `ant test`, run one class directly against the jars it assembled (rebuild with `ant clean test`
  after changing code):

  ```sh
  java -cp "build/ant/test-target/*:environment/test" org.junit.runner.JUnitCore net.solarnetwork.node.domain.test.ExpressionRootTests
  ```

- Tests are JUnit 4 (no Jupiter) with AssertJ, Mockito, Hancrest, and EasyMock. They live in a `test`
  sub-package of the code under test (e.g. `net.solarnetwork.util.test`) and are named `*Tests`.
- Coverage: `ant test -Dwith-coverage=true`, then `ant coverage-report`.
- Integration tests that need a real service are skipped unless a system property is set
  (`SystemPropertyMatchTestRule`: `mqtt5-int`, `s3-int`, `protoc-int`). They read their settings from
  `environment/local/*.properties`, which is gitignored; templates are in `environment/example/`.
  Only `environment/${env.name}` is on the test classpath and `env.name` defaults to `test`, so pass
  `-Denv.name=local`, or the tests fail with `NullPointerException: this.input is null`:

  ```sh
  ant test -Denv.name=local -Dtest.jvm.args="-Dmqtt5-int=true"
  ```

