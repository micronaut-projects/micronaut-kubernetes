package io.micronaut.kubernetes.client.openapi

import io.micronaut.context.ApplicationContext
import io.micronaut.context.annotation.Replaces
import io.micronaut.context.annotation.Requires
import io.micronaut.context.annotation.Value
import io.micronaut.core.io.ResourceResolver
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.Header
import io.micronaut.http.client.HttpClient
import io.micronaut.http.client.netty.DefaultHttpClient
import io.micronaut.http.uri.UriBuilder
import io.micronaut.kubernetes.client.openapi.api.CoreV1Api
import io.micronaut.kubernetes.client.openapi.config.AbstractKubeConfigLoader
import io.micronaut.kubernetes.client.openapi.config.DefaultKubeConfigLoader
import io.micronaut.kubernetes.client.openapi.config.KubeConfig
import io.micronaut.kubernetes.client.openapi.model.V1Pod
import io.micronaut.kubernetes.client.openapi.model.V1PodList
import io.micronaut.runtime.server.EmbeddedServer
import jakarta.inject.Singleton
import org.yaml.snakeyaml.LoaderOptions
import org.yaml.snakeyaml.Yaml
import org.yaml.snakeyaml.constructor.SafeConstructor
import spock.lang.AutoCleanup
import spock.lang.Specification

class ClientConfigLoaderSpec extends Specification {

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
      token: test-token
contexts:
  - name: test-context
    context:
      cluster: test-cluster
      user: test-user
current-context: test-context
"""

    @AutoCleanup
    EmbeddedServer kubernetesServer = ApplicationContext.run(EmbeddedServer, [
            'spec.name': 'ClientConfigLoaderKubernetesServer',
            'kubernetes.client.enabled': false
    ])

    @AutoCleanup
    EmbeddedServer kubeConfigProviderServer = ApplicationContext.run(EmbeddedServer, [
            'spec.name': 'ClientConfigLoaderKubeConfigProviderServer',
            'kubernetes.client.enabled': false,
            'kubernetes.server.url': kubernetesServer.URL
    ])

    def 'list pods when token authentication is used'() {
        given:
        ApplicationContext clientContext = ApplicationContext.run([
                'spec.name': 'ClientConfigLoaderClientContext',
                'kube.config.provider.url': kubeConfigProviderServer.URL
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
    @Requires(property = 'spec.name', value = 'ClientConfigLoaderClientContext')
    @Replaces(DefaultKubeConfigLoader.class)
    static class CustomKubeConfigLoader extends AbstractKubeConfigLoader {

        HttpClient httpClient

        protected CustomKubeConfigLoader(ResourceResolver resourceResolver, @Value("\${kube.config.provider.url}") kubeConfigProviderUrl) {
            super(resourceResolver)
            httpClient = DefaultHttpClient.builder().uri(URI.create(kubeConfigProviderUrl)).build()
        }

        @Override
        protected KubeConfig loadKubeConfig() {
            String uri = UriBuilder.of("/kube-config").toString()
            String result = httpClient.toBlocking().retrieve(uri)
            Yaml yaml = new Yaml(new SafeConstructor(new LoaderOptions()))
            Map<String, Object> configMap = yaml.load(result)
            return new KubeConfig(configMap)
        }
    }

    @Controller
    @Requires(property = 'spec.name', value = 'ClientConfigLoaderKubeConfigProviderServer')
    static class KubeConfigProviderController {

        @Value("\${kubernetes.server.url}") String kubernetesServerUrl

        @Get("/kube-config")
        String getKubeConfig() {
            return KUBE_CONFIG.formatted(kubernetesServerUrl)
        }
    }

    @Controller
    @Requires(property = 'spec.name', value = 'ClientConfigLoaderKubernetesServer')
    static class KubernetesServerController {
        @Get("/api/v1/pods")
        V1PodList auth(@Header('Authorization') String authorization) {
            return authorization == "Bearer test-token"
                    ? new V1PodList(Arrays.asList(new V1Pod()))
                    : new V1PodList(Collections.emptyList())

        }
    }
}
