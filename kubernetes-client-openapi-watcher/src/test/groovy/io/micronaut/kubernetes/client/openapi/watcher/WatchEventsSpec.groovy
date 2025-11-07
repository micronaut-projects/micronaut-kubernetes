package io.micronaut.kubernetes.client.openapi.watcher

import io.micronaut.context.ApplicationContext
import io.micronaut.context.env.Environment
import io.micronaut.kubernetes.client.openapi.api.CoreV1Api
import io.micronaut.kubernetes.client.openapi.model.V1Secret
import io.micronaut.kubernetes.client.openapi.response.DeleteResponse
import io.micronaut.kubernetes.client.openapi.watcher.api.CoreV1ApiWatcher
import io.micronaut.kubernetes.openapi.test.K3sContainerSpec
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import reactor.core.Disposable
import reactor.core.publisher.Flux
import spock.util.concurrent.PollingConditions

import java.util.concurrent.ConcurrentHashMap

import static io.micronaut.kubernetes.openapi.test.KubernetesModels.getNamespaceModel
import static io.micronaut.kubernetes.openapi.test.KubernetesModels.getSecretModel
import static io.micronaut.kubernetes.openapi.test.KubernetesOperations.createNamespace
import static io.micronaut.kubernetes.openapi.test.KubernetesOperations.createSecret
import static io.micronaut.kubernetes.openapi.test.KubernetesOperations.deleteSecret
import static io.micronaut.kubernetes.openapi.test.KubernetesOperations.replaceSecret

class WatchEventsSpec extends K3sContainerSpec {

    private static final Logger LOG = LoggerFactory.getLogger(WatchEventsSpec)

    @Override
    Logger getLogger() {
        return LOG
    }

    def 'watch namespace events'() {
        given:
        ApplicationContext context = ApplicationContext.run([
                "kubernetes.client.kube-config-path"   : "file:" + kubeConfigFile.toString()
        ], Environment.KUBERNETES)
        CoreV1ApiWatcher apiWatcher = context.getBean(CoreV1ApiWatcher.class)
        CoreV1Api api = context.getBean(CoreV1Api.class)

        when:
        def namespaceName = 'watch-secrets-test'
        def secretName = 'ws-test-1'
        createNamespace(api, getNamespaceModel(namespaceName))
        createSecret(api, namespaceName, getSecretModel(secretName, ["test-key":"test-value".bytes]))

        Map<String, List<String>> events = new ConcurrentHashMap<>()

        Flux<WatchEvent<V1Secret>> flux = apiWatcher.listNamespacedSecret(namespaceName, null, null, null, null,
                null, null, null, null, null, null, true)
        Disposable disposable = flux.subscribe(event -> {events.computeIfAbsent(event.object.metadata.name, key -> []).add(event.type)})

        replaceSecret(api, namespaceName, getSecretModel(secretName, ["test-key":"new-test-value".bytes]))
        DeleteResponse<V1Secret> deleteResponse = deleteSecret(api, namespaceName, secretName)

        PollingConditions conditions = new PollingConditions(timeout: 2)

        then:
        conditions.eventually {
            events.size() > 0
            events.get(secretName)?.get(0) == 'ADDED'
            events.get(secretName)?.get(1) == 'MODIFIED'
            events.get(secretName)?.get(2) == 'DELETED'
        }
        deleteResponse.status().status == "Success"

        cleanup:
        disposable?.dispose()
    }
}
