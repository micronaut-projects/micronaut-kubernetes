package io.micronaut.kubernetes.client.openapi

import io.micronaut.context.ApplicationContext
import io.micronaut.context.annotation.BootstrapContextCompatible
import io.micronaut.context.annotation.Requires
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.Header
import io.micronaut.kubernetes.client.openapi.api.CoreV1Api
import io.micronaut.kubernetes.client.openapi.credential.ReactiveKubernetesTokenLoader
import io.micronaut.kubernetes.client.openapi.model.V1Pod
import io.micronaut.kubernetes.client.openapi.model.V1PodList
import io.micronaut.runtime.server.EmbeddedServer
import jakarta.inject.Singleton
import org.reactivestreams.Publisher
import reactor.core.publisher.Mono
import spock.lang.AutoCleanup
import spock.lang.Shared
import spock.lang.Specification

import java.nio.file.Files
import java.nio.file.Path

class ClientCredentialLoaderSpec extends Specification {

    static final String KUBE_CONFIG = """\
apiVersion: v1
kind: Config
clusters:
  - name: test-cluster
    cluster:
      server: %s
users:
  - name: test-user
    user:
      token: test-user
contexts:
  - name: test-context
    context:
      cluster: test-cluster
      user: test-user
current-context: test-context
"""

    @AutoCleanup
    EmbeddedServer server = ApplicationContext.run(EmbeddedServer, [
            'spec.name': 'ClientCredentialLoaderSpec-Server',
            'kubernetes.client.enabled': false
    ])

    @Shared
    Path kubeConfigDir = Files.createTempDirectory("kube-temp-")

    @Shared
    Path kubeConfigFile = kubeConfigDir.resolve("config")

    def cleanupSpec() {
        if (kubeConfigFile != null) {
            Files.deleteIfExists(kubeConfigFile)
        }
        if (kubeConfigDir) {
            Files.deleteIfExists(kubeConfigDir)
        }
    }

    def 'list pods when basic authentication is used'() {
        given:
        kubeConfigFile.toFile().text = KUBE_CONFIG.formatted(server.URL)
        ApplicationContext clientContext = ApplicationContext.run([
                'spec.name': 'ClientCredentialLoaderSpec-Client',
                'kubernetes.client.kube-config-path': "file:" + kubeConfigFile.toString()
        ])

        when:
        V1PodList response = clientContext.getBean(CoreV1Api.class).listPodForAllNamespaces(
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null)

        then:
        response.getItems() != null
        response.getItems().size() == 1

        cleanup:
        clientContext.close()
    }

    @Singleton
    @Requires(property = 'spec.name', value = 'ClientCredentialLoaderSpec-Client')
    @BootstrapContextCompatible
    static class FirstCredentialLoader implements ReactiveKubernetesTokenLoader {

        @Override
        Publisher<String> getToken() {
            return Mono.just("first")
        }

        @Override
        int getOrder() {
            return -1
        }
    }

    @Singleton
    @Requires(property = 'spec.name', value = 'ClientCredentialLoaderSpec-Client')
    @BootstrapContextCompatible
    static class SecondCredentialLoader implements ReactiveKubernetesTokenLoader {

        @Override
        Publisher<String> getToken() {
            return Mono.just("second")
        }

        @Override
        int getOrder() {
            return 0
        }
    }

    @Controller
    @Requires(property = 'spec.name', value = 'ClientCredentialLoaderSpec-Server')
    static class BasicAuthController {
        @Get("/api/v1/pods")
        V1PodList auth(@Header('Authorization') String authorization) {
            return authorization == "Bearer first"
                    ? new V1PodList(Arrays.asList(new V1Pod()))
                    : new V1PodList(Collections.emptyList())
        }
    }
}
