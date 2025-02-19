package io.micronaut.kubernetes.client.openapi

import io.micronaut.context.ApplicationContext
import io.micronaut.context.ProviderUtils
import io.micronaut.core.util.StringUtils
import io.micronaut.http.HttpMethod
import io.micronaut.http.HttpResponse
import io.micronaut.http.MutableHttpRequest
import io.micronaut.http.filter.ClientFilterChain
import io.micronaut.http.simple.SimpleHttpRequest
import io.micronaut.kubernetes.client.openapi.config.KubeConfig
import io.micronaut.kubernetes.client.openapi.config.KubeConfigLoader
import io.micronaut.kubernetes.client.openapi.credential.KubernetesTokenLoader
import io.micronaut.kubernetes.client.openapi.credential.ReactiveKubernetesTokenLoader
import io.micronaut.kubernetes.client.openapi.credential.TokenLoader
import org.reactivestreams.Publisher
import reactor.core.publisher.Mono
import spock.lang.Specification

class KubernetesHttpClientFilterSpec extends Specification {

    private static final def BASE_MAP = ["current-context": "test-context"]
    private static final def CONTEXT_MAP = [contexts: [[name: "test-context", context: [cluster: "test-cluster", user: "test-user"]]]]
    private static final def CLUSTER_MAP = [clusters: [[name: "test-cluster", cluster: ["server": "test-server"]]]]
    private static final def USER_MAP = [users: [[name: "test-user", user: ["username": "test-username", "password": "test-password"]]]]
    private static final def KUBE_CONFIG_MAP = BASE_MAP + CONTEXT_MAP + CLUSTER_MAP + USER_MAP

    ApplicationContext applicationContext
    KubeConfigLoader kubeConfigLoader

    def setup() {
        applicationContext = Stub(ApplicationContext)
        kubeConfigLoader = Stub(KubeConfigLoader)
    }

    def 'filter uses username and password authentication'() {
        given:
        def filter = new KubernetesHttpClientFilter(ProviderUtils.memoized(() -> kubeConfigLoader), applicationContext, null)
        def request = new SimpleHttpRequest<String>(HttpMethod.GET, "/test", "value")
        def filterChain = new CustomClientFilterChain()

        and:
        def config = new KubeConfig(KUBE_CONFIG_MAP)
        kubeConfigLoader.getKubeConfig() >> config

        when:
        Mono.from(filter.doFilter(request, filterChain)).block()

        then:
        filterChain.getAuthHeaderValue() == "Basic " + new String(Base64.getEncoder().encode("test-username:test-password".getBytes()))
    }

    def 'filter uses token from blocking loader'() {
        given:
        def filter = new KubernetesHttpClientFilter(ProviderUtils.memoized(() -> kubeConfigLoader), applicationContext, null)
        def request = new SimpleHttpRequest<String>(HttpMethod.GET, "/test", "value")
        def filterChain = new CustomClientFilterChain()

        and:
        kubeConfigLoader.getKubeConfig() >> null

        and:
        def loader1 = new BlockingLoader("test1")
        def loader2 = new ReactiveLoader("test2")
        applicationContext.getBeansOfType(TokenLoader.class) >> [loader1, loader2]

        when:
        Mono.from(filter.doFilter(request, filterChain)).block()

        then:
        filterChain.getAuthHeaderValue() == "Bearer test1"
    }

    def 'filter uses token from reactive loader'() {
        given:
        def filter = new KubernetesHttpClientFilter(ProviderUtils.memoized(() -> kubeConfigLoader), applicationContext, null)
        def request = new SimpleHttpRequest<String>(HttpMethod.GET, "/test", "value")
        def filterChain = new CustomClientFilterChain()

        and:
        kubeConfigLoader.getKubeConfig() >> null

        and:
        def loader1 = new BlockingLoader(null)
        def loader2 = new ReactiveLoader("test2")
        applicationContext.getBeansOfType(TokenLoader.class) >> [loader1, loader2]

        when:
        Mono.from(filter.doFilter(request, filterChain)).block()

        then:
        filterChain.getAuthHeaderValue() == "Bearer test2"
    }

    class CustomClientFilterChain implements ClientFilterChain {
        private String authHeaderValue

        String getAuthHeaderValue() {
            return authHeaderValue
        }

        @Override
        Publisher<? extends HttpResponse<?>> proceed(MutableHttpRequest<?> request) {
            authHeaderValue = request.getHeaders().getAuthorization().orElse(StringUtils.EMPTY_STRING)
            return Mono.empty()
        }
    }

    static class BlockingLoader implements KubernetesTokenLoader {
        private final String token

        private BlockingLoader(String token) {
            this.token = token
        }

        @Override
        String getToken() {
            return token
        }
    }

    static class ReactiveLoader implements ReactiveKubernetesTokenLoader {
        private final String token

        private ReactiveLoader(String token) {
            this.token = token
        }

        @Override
        Publisher<String> getToken() {
            return Mono.fromCallable(() -> loadToken())
        }

        private String loadToken() {
            return token
        }
    }
}
