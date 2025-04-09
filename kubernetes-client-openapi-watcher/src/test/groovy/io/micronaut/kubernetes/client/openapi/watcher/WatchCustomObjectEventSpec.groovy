package io.micronaut.kubernetes.client.openapi.watcher

import io.micronaut.kubernetes.client.openapi.api.ApiextensionsV1Api
import io.micronaut.kubernetes.client.openapi.api.CoreV1Api
import io.micronaut.kubernetes.client.openapi.api.CustomObjectsApi
import io.micronaut.kubernetes.client.openapi.model.V1CustomResourceDefinition
import io.micronaut.kubernetes.client.openapi.model.V1CustomResourceDefinitionNames
import io.micronaut.kubernetes.client.openapi.model.V1CustomResourceDefinitionSpec
import io.micronaut.kubernetes.client.openapi.model.V1CustomResourceDefinitionVersion
import io.micronaut.kubernetes.client.openapi.model.V1CustomResourceValidation
import io.micronaut.kubernetes.client.openapi.model.V1JSONSchemaProps
import io.micronaut.kubernetes.client.openapi.model.V1Namespace
import io.micronaut.kubernetes.client.openapi.model.V1ObjectMeta
import io.micronaut.kubernetes.client.openapi.watcher.api.CustomObjectsApiWatcher
import io.micronaut.test.extensions.spock.annotation.MicronautTest
import io.micronaut.test.support.TestPropertyProvider
import jakarta.inject.Inject
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.testcontainers.containers.output.Slf4jLogConsumer
import org.testcontainers.k3s.K3sContainer
import org.testcontainers.utility.DockerImageName
import reactor.core.Disposable
import reactor.core.publisher.Flux
import spock.lang.AutoCleanup
import spock.lang.Shared
import spock.lang.Specification
import spock.util.concurrent.PollingConditions

import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.ConcurrentHashMap

@MicronautTest
class WatchCustomObjectEventSpec extends Specification implements TestPropertyProvider {

    private static final Logger LOG = LoggerFactory.getLogger(WatchEventsSpec)

    @Shared
    @AutoCleanup
    K3sContainer k3s = new K3sContainer(DockerImageName.parse("rancher/k3s:v1.31.5-k3s1"))
            .withLogConsumer(new Slf4jLogConsumer(LOG))

    @Shared
    Path kubeConfigDir = Files.createTempDirectory("kube-temp-")

    @Shared
    Path kubeConfigFile = kubeConfigDir.resolve("config")

    @Inject
    ApiextensionsV1Api apiextensionsV1Api

    @Inject
    CoreV1Api coreV1Api

    @Inject
    CustomObjectsApi customObjectsApi

    @Inject
    CustomObjectsApiWatcher apiWatcher

    @Override
    Map<String, String> getProperties() {
        k3s.start()
        kubeConfigFile.toFile().text = k3s.getKubeConfigYaml()
        ["kubernetes.client.kube-config-path": "file:" + kubeConfigFile.toString()]
    }

    def cleanupSpec() {
        if (kubeConfigFile != null) {
            Files.deleteIfExists(kubeConfigFile)
        }
        if (kubeConfigDir) {
            Files.deleteIfExists(kubeConfigDir)
        }
    }

    def 'watch custom resource events'() {
        when:
        def namespaceName = 'watch-custom-resource-test'
        def customObjectName = 'co-test-1'
        createNamespace(namespaceName)
        createCustomResourceDefinition()
        def createdObject = createCustomObject(namespaceName, customObjectName)

        Map<String, List<String>> events = new ConcurrentHashMap<>()

        Flux<WatchEvent<Object>> flux = apiWatcher.listNamespacedCustomObject("test.io", "v1", namespaceName, "testcustoms", null, null, null, null, null, null, null, null, null, true)
        Disposable disposable = flux.subscribe(event -> {
            events.computeIfAbsent(event.object.metadata.name, key -> []).add(event.type)
        })

        replaceCustomObject(namespaceName, customObjectName, createdObject.metadata.resourceVersion)
        deleteCustomObject(namespaceName, customObjectName)

        PollingConditions conditions = new PollingConditions(timeout: 2)

        then:
        conditions.eventually {
            events.size() > 0
            events.get(customObjectName)?.get(0) == 'ADDED'
            events.get(customObjectName)?.get(1) == 'MODIFIED'
            events.get(customObjectName)?.get(2) == 'DELETED'
        }

        cleanup:
        disposable?.dispose()
    }

    private void createNamespace(String namespaceName) {
        V1Namespace namespace = new V1Namespace()
        namespace.kind('Namespace')
        namespace.apiVersion('v1')
        V1ObjectMeta objectMeta = new V1ObjectMeta()
        objectMeta.name(namespaceName)
        namespace.metadata(objectMeta)
        coreV1Api.createNamespace(namespace, null, null, null, null)
    }

    private void createCustomResourceDefinition() {
        V1JSONSchemaProps jsonSchemaProps = new V1JSONSchemaProps()
        jsonSchemaProps.setType("object")
        jsonSchemaProps.setProperties([
                apiVersion: getProp("string"),
                "kind"    : getProp("string"),
                "metadata": getProp("object"),
                "spec"    : getProp("object", ['test-property': getProp("string")])
        ])

        V1CustomResourceValidation customResourceValidation = new V1CustomResourceValidation()
        customResourceValidation.setOpenAPIV3Schema(jsonSchemaProps)

        V1CustomResourceDefinitionVersion customResourceDefinitionVersion = new V1CustomResourceDefinitionVersion("v1", true, true)
        customResourceDefinitionVersion.setSchema(customResourceValidation)

        V1CustomResourceDefinitionSpec customResourceDefinitionSpec = new V1CustomResourceDefinitionSpec(
                "test.io",
                new V1CustomResourceDefinitionNames("TestCustom", "testcustoms"),
                "Namespaced",
                [customResourceDefinitionVersion])

        V1CustomResourceDefinition customResourceDefinition = new V1CustomResourceDefinition(customResourceDefinitionSpec)
        V1ObjectMeta metadata = new V1ObjectMeta()
        metadata.setName("testcustoms.test.io")
        customResourceDefinition.setMetadata(metadata)

        apiextensionsV1Api.createCustomResourceDefinition(customResourceDefinition, null, null, null, null)
    }

    private V1JSONSchemaProps getProp(String type, Map<String, V1JSONSchemaProps> props = [:]) {
        V1JSONSchemaProps specProp = new V1JSONSchemaProps()
        specProp.setType(type)
        specProp.setProperties(props)
        return specProp
    }

    private Object createCustomObject(String namespace, String name) {
        String body = '{"apiVersion":"test.io/v1","kind":"TestCustom","metadata":{"name":"' + name + '"}, "spec":{"test-property":"value1"}}'
        return customObjectsApi.createNamespacedCustomObject("test.io", "v1", namespace, "testcustoms", body, null, null, null, null)
    }

    private Object replaceCustomObject(String namespace, String name, String resourceVersion) {
        String body = '{"apiVersion":"test.io/v1","kind":"TestCustom","metadata":{"resourceVersion":"' + resourceVersion + '","name":"' + name + '"}, "spec":{"test-property":"value2"}}'
        return customObjectsApi.replaceNamespacedCustomObject("test.io", "v1", namespace, "testcustoms", name, body, null, null, null)
    }

    private void deleteCustomObject(String namespace, String name) {
        customObjectsApi.deleteNamespacedCustomObject("test.io", "v1", namespace, "testcustoms", name, null, null, null, null, null)
    }
}
