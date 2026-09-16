# Python Docs Disabled Test Inventory

This file tracks the Python documentation examples of this repository (`test-suite-python`
and the `examples/example-kubernetes-client-openapi*-python` projects) that are present but disabled,
reduced, or intentionally commented out because the direct port of the Java example does not compile or
does not behave like the Java example yet. It is the bug-fixing task list for the Python compiler
(`micronaut-inject-python` / `micronaut-context-python`); every row references a `TODO(python)` comment
in the sources.

The Python examples are compiled by every build and their tests run with
`./gradlew pythonCheck -Ppython-ci` (the "Python CI" GitHub workflow).

## Migration Rules

- Do not define local copies of Micronaut annotation helpers or custom annotation shims in docs
  snippets. Standard Micronaut and library annotations are imported from their Java package.
- Java classes are imported from their package (`from java.util import Optional`,
  `from reactor.core.publisher import Mono`), never aliased with `java.type(...)`. Classes of the official
  Kubernetes Java SDK live in the `io.kubernetes` package, which cannot be imported at runtime (`io` is the
  standard library module): they are imported inside a `try:` block with an `except ImportError` fallback to
  the generated `kubernetes...` shim package (`from io.kubernetes.client.openapi.models import V1ConfigMap` /
  `from kubernetes.client.openapi.models import V1ConfigMap`, marked `TODO(python)`). The imported names are
  used in type hints, generic base classes, annotation members and as runtime type arguments.
- Logging uses Python's `logging` module (`LOG = logging.getLogger(__name__)`), not slf4j.
- A Python test class is a `@MicronautTest` with injected beans; Python beans are looked up with the
  imported Python class, `context.getBean(PodController).asPolyglotValue()`.
- The main and test Python sources of an `examples/*-python` project are compiled together into the test
  output (`compilePython` is disabled by the `kubernetes-python-examples` convention), because two GraalPy
  virtual file systems on the same classpath shadow each other's generated shims.
- Python has no `package-info`, so the beans of the example projects that must only be loaded inside a
  cluster declare `@Requires(env=Environment.KUBERNETES)` themselves (outside the snippet tags).
- The examples talk to a Kubernetes API, which is not available to the unit tests: the tests exercise the
  filters, reconcilers and controllers with duck-typed stand-ins for the informer cache and the API.

## Active `@Disabled` Tests

| Test | Reason |
| --- | --- |
| `micronaut.docs.openapi.informer.example2.CustomTypeNameMapperTest` | A Python `TypeNameMapper` bean is collected by `TypeNameResolver` while the `ApiReactorExecMethodProcessor` runs during context start, before the GraalPy context bean is initialized (`GraalPy context has not been initialized`). The `CustomTypeNameMapper` bean is therefore gated by `spec.name`. |
| `micronaut.docs.openapi.informer.example1.CustomObjectTest.test_custom_object_is_serialized` | The introspection of a Python class is built from its attributes, so `CustomObject`, which implements `KubernetesObject` through explicit getters, is serialized as `{}` by `@Serdeable`. |

## Reduced Ports

| Target | Difference |
| --- | --- |
| `micronaut.docs.operator.OnAddFilter`, `micronaut.operator.OnAddFilter` (`examples/example-kubernetes-client-openapi-operator-python`) | The base class is the raw `Predicate` instead of `Predicate[V1ConfigMap]`: the stub generated for the parameterized interface copies the static `Predicate.not(Predicate<? super T>)` method with the invalid signature `Predicate<? super ? super Object>`. `BiPredicate` has no static methods and is parameterized as in Java. |
| `micronaut.docs.openapi.informer.example1.CustomObject`, `CustomObjectList` | Written as plain classes with explicit `getApiVersion()` / `getKind()` / `getMetadata()` / `getItems()` methods instead of dataclasses: dataclass attributes generate getters that clash with the bridged interface getters (`getApiVersion() is already defined`), and the wildcard return type of `KubernetesListObject.getItems()` (`List<? extends KubernetesObject>`) is not supported by the stub generator (`Unrecognized type def: Wildcard`). See the disabled serialization test above. |

## `java.type` usages

None: every Java class is imported, the `io.kubernetes` classes through the `try`/`except ImportError` form
described above (13 files, see the `TODO(python)` markers on the fallback imports).
