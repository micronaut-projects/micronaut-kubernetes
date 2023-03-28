package micronaut.client

import groovy.util.logging.Slf4j
import io.micronaut.context.annotation.Property
import io.micronaut.context.env.Environment
import io.micronaut.http.MediaType
import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.Post
import io.micronaut.http.client.annotation.Client
import io.micronaut.kubernetes.test.KubernetesSpecification
import io.micronaut.kubernetes.test.TestUtils
import io.micronaut.test.extensions.spock.annotation.MicronautTest
import spock.lang.Requires
import spock.lang.Shared
import spock.util.concurrent.PollingConditions

import jakarta.inject.Inject

import io.micronaut.context.annotation.Requires as MicronautRequires

@MicronautTest(environments = [Environment.KUBERNETES])
@Property(name = "spec.name", value = "HelloControllerSpec")
@Property(name = "spec.reuseNamespace", value = "false")
@Property(name = "kubernetes.client.namespace", value = "micronaut-example-client")
@Property(name = "micronaut.http.client.read-timeout", value = "30")
@Requires({ TestUtils.kubernetesApiAvailable() })
class HelloControllerSpec extends KubernetesSpecification {

    @Inject
    @Shared
    TestClient testClient

    def setupSpec() {
        operations.portForwardService("example-client", namespace, 8082, 8888)
    }

    void "test index"() {
        expect:
        testClient.index().startsWith("Hello, example-client")
    }

    void "test all"() {
        expect:
        testClient.all().contains("example-service")
    }

    void "test enemies"() {
        expect:
        testClient.enemies().equals("noGoodRotten")
    }

    void "test config"() {
        given:
        PollingConditions conditions = new PollingConditions(timeout: 30, delay: 2)
        String configMapName = "hello-controller-spec"

        expect:
        testClient.config("foo").equals("NOTHING")
        !testClient.env().contains(configMapName)

        when:
        operations.createConfigMap(configMapName, namespace, ["foo": "bar"])

        then:
        conditions.eventually {
            testClient.config("foo").equals("bar")
            testClient.env().contains(configMapName)
        }

        when:
        operations.modifyConfigMap(configMapName, namespace, ["foo": "baz"])

        then:
        conditions.eventually {
            testClient.config("foo").equals("baz")
            testClient.env().contains(configMapName)
        }

        when:
        operations.deleteConfigMap(configMapName, namespace)

        then:
        conditions.eventually {
            !testClient.env().contains(configMapName)
        }

        cleanup:
        operations.deleteConfigMap(configMapName, namespace)
    }

    void "test reading secrets from mounted volumes"() {
        given:
        testClient.refresh()

        expect:
        testClient.config("mounted-volume-key").equals("mountedVolumeValue")
    }

    void "test reading config maps from mounted volumes"() {
        given:
        testClient.refresh()

        expect:
        testClient.env().contains("{\"name\":\"/etc/example-service/configmap/mounted.yml (Kubernetes ConfigMap)\"")
        testClient.config("mounted.foo") == "bar"
    }

    @Client("http://localhost:8888")
    @MicronautRequires(property = "spec.name", value = "HelloControllerSpec")
    static interface TestClient {

        @Get(processes = MediaType.TEXT_PLAIN)
        String index()

        @Get(uri = "/all", processes = MediaType.TEXT_PLAIN)
        String all()

        @Get(uri = "/enemies", processes = MediaType.TEXT_PLAIN)
        String enemies()

        @Get(uri = "/config/{key}", processes = MediaType.TEXT_PLAIN)
        String config(String key)

        @Post(uri = "/refreshService", processes = MediaType.TEXT_PLAIN)
        String refresh()

        @Get(uri = "/serviceEnv", processes = MediaType.TEXT_PLAIN)
        String env()

    }
}
