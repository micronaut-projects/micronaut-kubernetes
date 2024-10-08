package io.micronaut.kubernetes.client.openapi

import io.micronaut.context.ApplicationContext
import io.micronaut.context.annotation.Requires
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.Header
import io.micronaut.kubernetes.client.openapi.api.CoreV1Api
import io.micronaut.kubernetes.client.openapi.model.V1Pod
import io.micronaut.kubernetes.client.openapi.model.V1PodList
import io.micronaut.runtime.server.EmbeddedServer
import spock.lang.AutoCleanup
import spock.lang.Shared
import spock.lang.Specification

import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path

class KubernetesBasicAuthSpec extends Specification {

    static final String KUBE_CONFIG = """
apiVersion: v1
kind: Config
clusters:
  - name: test-cluster
    cluster:
      server: %s
users:
  - name: test-user
    user:
      username: test-username
      password: test-password
contexts:
  - name: test-context
    context:
      cluster: test-cluster
      user: test-user
current-context: test-context
"""

    @AutoCleanup
    EmbeddedServer server = ApplicationContext.run(EmbeddedServer, [
            'spec.name': 'BasicAuthServer',
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

    @Controller
    @Requires(property = 'spec.name', value = 'BasicAuthServer')
    static class BasicAuthController {
        @Get("/api/v1/pods")
        V1PodList auth(@Header('Authorization') String authorization) {
            String basicTokenEncoded = authorization.substring("Basic ".length())
            byte[] basicTokenDecoded = Base64.getDecoder().decode(basicTokenEncoded)
            String basicToken = new String(basicTokenDecoded, StandardCharsets.UTF_8)
            String[] userAndPass= basicToken.split(":")
            return userAndPass.length == 2 && userAndPass[0] == 'test-username' && userAndPass[1] == 'test-password'
                    ? new V1PodList(Arrays.asList(new V1Pod()))
                    : new V1PodList(Collections.emptyList())
        }
    }
}
