package io.micronaut.kubernetes.client.openapi.reactor

import io.micronaut.kubernetes.client.openapi.model.V1ConfigMap
import io.micronaut.kubernetes.client.openapi.model.V1Namespace
import io.micronaut.kubernetes.client.openapi.model.V1ObjectMeta
import io.micronaut.kubernetes.client.openapi.reactor.api.CoreV1ApiReactor
import io.micronaut.kubernetes.client.openapi.response.DeleteResponse
import io.micronaut.test.extensions.spock.annotation.MicronautTest
import io.micronaut.test.support.TestPropertyProvider
import jakarta.inject.Inject
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.testcontainers.containers.output.Slf4jLogConsumer
import org.testcontainers.k3s.K3sContainer
import org.testcontainers.utility.DockerImageName
import spock.lang.AutoCleanup
import spock.lang.Shared
import spock.lang.Specification

import java.nio.file.Files
import java.nio.file.Path

@MicronautTest
class DeleteObjectSpec extends Specification implements TestPropertyProvider {

    private static final Logger logger = LoggerFactory.getLogger(DeleteObjectSpec)

    @Shared
    @AutoCleanup
    K3sContainer k3s = new K3sContainer(DockerImageName.parse("rancher/k3s:v1.31.5-k3s1"))
            .withLogConsumer(new Slf4jLogConsumer(logger))

    @Shared
    Path kubeConfigDir = Files.createTempDirectory("kube-temp-")

    @Shared
    Path kubeConfigFile = kubeConfigDir.resolve("config")

    @Inject
    CoreV1ApiReactor api

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

    def 'delete kubernetes objects'() {
        given:
        V1Namespace namespace = new V1Namespace()
        namespace.kind('Namespace')
        namespace.apiVersion('v1')
        V1ObjectMeta namespaceMetadata = new V1ObjectMeta()
        namespaceMetadata.name('micronaut-test-namespace')
        namespace.metadata(namespaceMetadata)
        api.createNamespace(namespace, null, null, null, null).block()

        V1ConfigMap configMap = new V1ConfigMap()
        configMap.kind('ConfigMap')
        configMap.apiVersion('v1')
        configMap.data(['test.properties': 'testKey=testValue'])
        V1ObjectMeta configMapMetadata = new V1ObjectMeta()
        configMapMetadata.name('micronaut-test-config-map')
        configMap.metadata(configMapMetadata)
        api.createNamespacedConfigMap('micronaut-test-namespace', configMap, null, null, null, null).block()

        when:
        DeleteResponse<V1ConfigMap> configMapResponse = api.deleteNamespacedConfigMap('micronaut-test-config-map', 'micronaut-test-namespace', null, null, null, null, null, null, null).block()
        DeleteResponse<V1Namespace> namespaceResponse = api.deleteNamespace('micronaut-test-namespace', null, null, null, null, null, null, null).block()

        then:
        configMapResponse.object() == null
        configMapResponse.status().apiVersion == 'v1'
        configMapResponse.status().details.kind == 'configmaps'
        configMapResponse.status().details.name == 'micronaut-test-config-map'

        namespaceResponse.object().apiVersion == 'v1'
        namespaceResponse.object().metadata.name == 'micronaut-test-namespace'
        namespaceResponse.status() == null
    }
}
