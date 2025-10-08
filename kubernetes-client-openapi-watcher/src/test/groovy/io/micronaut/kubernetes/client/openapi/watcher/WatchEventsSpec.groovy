package io.micronaut.kubernetes.client.openapi.watcher

import io.micronaut.kubernetes.client.openapi.api.CoreV1Api
import io.micronaut.kubernetes.client.openapi.model.V1Namespace
import io.micronaut.kubernetes.client.openapi.model.V1ObjectMeta
import io.micronaut.kubernetes.client.openapi.model.V1Secret
import io.micronaut.kubernetes.client.openapi.model.V1Status
import io.micronaut.kubernetes.client.openapi.watcher.api.CoreV1ApiWatcher
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
class WatchEventsSpec extends Specification implements TestPropertyProvider {

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
    CoreV1ApiWatcher apiWatcher

    @Inject
    CoreV1Api api

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

    def 'watch namespace events'() {
        when:
        def namespaceName = 'watch-secrets-test'
        def secretName = 'ws-test-1'
        createNamespace(namespaceName)
        createSecret(namespaceName, secretName)

        Map<String, List<String>> events = new ConcurrentHashMap<>()

        Flux<WatchEvent<V1Secret>> flux = apiWatcher.listNamespacedSecret(namespaceName, null, null, null, null,
                null, null, null, null, null, null, true)
        Disposable disposable = flux.subscribe(event -> {events.computeIfAbsent(event.object.metadata.name, key -> []).add(event.type)})

        replaceSecret(namespaceName, secretName)
        V1Status v1Status = deleteSecret(namespaceName, secretName)

        PollingConditions conditions = new PollingConditions(timeout: 2)

        then:
        conditions.eventually {
            events.size() > 0
            events.get(secretName)?.get(0) == 'ADDED'
            events.get(secretName)?.get(1) == 'MODIFIED'
            events.get(secretName)?.get(2) == 'DELETED'
        }
        v1Status.status == "Success"

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
        api.createNamespace(namespace, null, null, null, null)
    }

    private void createSecret(String namespaceName, String secretName) {
        V1Secret secret = new V1Secret()
        secret.kind('Secret')
        secret.apiVersion('v1')
        V1ObjectMeta objectMeta = new V1ObjectMeta()
        objectMeta.name(secretName)
        secret.metadata(objectMeta)
        secret.data(["test-key":"test-value".bytes])
        api.createNamespacedSecret(namespaceName, secret, null, null, null, null)
    }

    private void replaceSecret(String namespaceName, String secretName) {
        V1Secret secret = new V1Secret()
        secret.kind('Secret')
        secret.apiVersion('v1')
        V1ObjectMeta objectMeta = new V1ObjectMeta()
        objectMeta.name(secretName)
        secret.metadata(objectMeta)
        secret.data(["test-key":"new-test-value".bytes])
        api.replaceNamespacedSecret(secretName, namespaceName, secret, null, null, null, null)
    }

    private V1Status deleteSecret(String namespaceName, String secretName) {
        api.deleteNamespacedSecret(secretName, namespaceName, null, null, null, null, null, null, null)
    }
}
