package micronaut.openapi.client

import io.micronaut.context.annotation.Property
import io.micronaut.context.env.Environment
import io.micronaut.http.MediaType
import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.Post
import io.micronaut.http.client.annotation.Client
import io.micronaut.kubernetes.openapi.test.KubectlPortForward
import io.micronaut.kubernetes.openapi.test.KubernetesSpecification
import io.micronaut.kubernetes.openapi.test.TestUtils
import io.micronaut.test.extensions.spock.annotation.MicronautTest
import jakarta.inject.Inject
import spock.lang.AutoCleanup
import spock.lang.Requires
import spock.lang.Shared
import spock.util.concurrent.PollingConditions

import static io.micronaut.kubernetes.openapi.test.KubernetesModels.getConfigMapModel
import static io.micronaut.kubernetes.openapi.test.KubernetesModels.getSecretModel
import static io.micronaut.kubernetes.openapi.test.KubernetesOperations.createConfigMap
import static io.micronaut.kubernetes.openapi.test.KubernetesOperations.createSecret
import static io.micronaut.kubernetes.openapi.test.KubernetesOperations.deleteConfigMap
import static io.micronaut.kubernetes.openapi.test.KubernetesOperations.deleteSecret
import static io.micronaut.kubernetes.openapi.test.KubernetesOperations.portForwardService
import static io.micronaut.kubernetes.openapi.test.KubernetesOperations.replaceConfigMap
import static io.micronaut.kubernetes.openapi.test.KubernetesOperations.replaceSecret

@MicronautTest(environments = [Environment.KUBERNETES])
@Requires({ TestUtils.kubernetesApiAvailable() })
@Property(name = "spec.type", value = "example-test")
@Property(name = "spec.name", value = "HelloControllerSpec")
@Property(name = "kubernetes.client.namespace", value = "example-openapi-discovery")
class HelloControllerSpec extends KubernetesSpecification {

    @Inject
    @Shared
    TestClient testClient

    @Shared
    @AutoCleanup
    KubectlPortForward kubectlPortForward

    def setupSpec() {
        kubectlPortForward = portForwardService(coreV1Api, "example-client", namespace, 8082, 8888)
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
        testClient.enemies() == "noGoodRotten"
    }

    void "test config added and updated"() {
        given:
        PollingConditions conditions = new PollingConditions(timeout: 30, initialDelay: 2, delay: 2)
        String configMapName = "hello-controller-spec"

        expect:
        testClient.config("foo") == "NOTHING"
        !testClient.env().contains(configMapName)

        when:
        createConfigMap(coreV1Api, namespace, getConfigMapModel(configMapName, ["foo": "bar"]))

        then:
        conditions.eventually {
            testClient.config("foo") == "bar"
            testClient.env().contains(configMapName)
        }

        when:
        replaceConfigMap(coreV1Api, namespace, getConfigMapModel(configMapName, ["foo": "baz"]))

        then:
        conditions.eventually {
            testClient.config("foo") == "baz"
            testClient.env().contains(configMapName)
        }

        when:
        deleteConfigMap(coreV1Api, namespace, configMapName)

        then:
        conditions.eventually {
            !testClient.env().contains(configMapName)
        }
    }

    void "test secret added and updated"() {
        given:
        PollingConditions conditions = new PollingConditions(timeout: 30, initialDelay: 2, delay: 2)
        String secretName = "hello-controller-spec-secret"

        expect:
        testClient.config("secretFoo") == "NOTHING"
        !testClient.env().contains(secretName)

        when:
        createSecret(coreV1Api, namespace, getSecretModel(secretName, ["secretFoo": "secretBar".bytes]))

        then:
        conditions.eventually {
            testClient.config("secretFoo") == "secretBar"
            testClient.env().contains(secretName)
        }

        when:
        replaceSecret(coreV1Api, namespace, getSecretModel(secretName, ["secretFoo": "secretBaz".bytes]))

        then:
        conditions.eventually {
            testClient.config("secretFoo") == "secretBaz"
            testClient.env().contains(secretName)
        }

        when:
        deleteSecret(coreV1Api, namespace, secretName)

        then:
        conditions.eventually {
            !testClient.env().contains(secretName)
        }
    }

    void "test reading secrets from mounted volumes"() {
        given:
        def value = testClient.config("mounted-volume-key")

        expect:
        value == "mountedVolumeValue"
    }

    void "test reading config maps from mounted volumes"() {
        given:
        def value = testClient.config("mounted.foo")
        def env = testClient.env()

        expect:
        env.contains("{\"name\":\"/etc/example-service/configmap/mounted.yml (Kubernetes V1ConfigMap)\"")
        value == "bar"
    }

    @Client("http://localhost:8888")
    @io.micronaut.context.annotation.Requires(property = "spec.name", value = "HelloControllerSpec")
    static interface TestClient {

        @Get(processes = MediaType.TEXT_PLAIN)
        String index()

        @Get(uri = "/all", processes = MediaType.APPLICATION_JSON)
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
