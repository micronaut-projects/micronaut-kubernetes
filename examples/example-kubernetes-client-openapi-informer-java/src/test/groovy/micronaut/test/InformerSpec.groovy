package micronaut.test

import io.micronaut.context.annotation.Property
import io.micronaut.http.MediaType
import io.micronaut.http.annotation.Get
import io.micronaut.http.client.annotation.Client
import io.micronaut.kubernetes.client.openapi.model.V1ConfigMap
import io.micronaut.kubernetes.client.openapi.model.V1Secret
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

@MicronautTest
@Requires({ TestUtils.kubernetesApiAvailable() })
@Property(name = "spec.type", value = "example-test")
@Property(name = "spec.name", value = "InformerSpec")
@Property(name = "kubernetes.client.namespace", value = "example-openapi-informer")
class InformerSpec extends KubernetesSpecification {

    @Inject
    @Shared
    TestClient testClient

    @Shared
    @AutoCleanup
    KubectlPortForward kubectlPortForward

    def setupSpec() {
        kubectlPortForward = portForwardService(coreV1Api, "example-informer", namespace, 8084, 8889)
    }

    @Override
    def createResources() {
        createTestConfigMap()
        createTestSecret()
        createDeployment("example-informer", "micronaut-kubernetes-example-informer-openapi", 8084, false)
        createService("example-informer", 8084)
    }

    void "test config map added and updated"() {
        given:
        PollingConditions conditions = new PollingConditions(timeout: 30, delay: 1)
        String configMapName = "test-configmap-2"

        expect:
        !testClient.configMap(configMapName)

        when:
        createConfigMap(coreV1Api, namespace, getConfigMapModel(configMapName, ["foo": "bar"]))

        then:
        conditions.eventually {
            testClient.configMap(configMapName).data["foo"] == "bar"
        }

        when:
        replaceConfigMap(coreV1Api, namespace, getConfigMapModel(configMapName, ["foo": "baz"]))

        then:
        conditions.eventually {
            testClient.configMap(configMapName).data["foo"] == "baz"
        }

        when:
        deleteConfigMap(coreV1Api, namespace, configMapName)

        then:
        conditions.eventually {
            !testClient.configMap(configMapName)
        }
    }

    void "test secret added and updated"() {
        given:
        PollingConditions conditions = new PollingConditions(timeout: 30, delay: 1)
        String secretName = "test-secret-2"

        expect:
        !testClient.secret(secretName)

        when:
        createSecret(coreV1Api, namespace, getSecretModel(secretName, ["secretFoo": "secretBar".bytes]))

        then:
        conditions.eventually {
            testClient.secret(secretName).data["secretFoo"] == "secretBar".bytes
        }

        when:
        replaceSecret(coreV1Api, namespace, getSecretModel(secretName, ["secretFoo": "secretBaz".bytes]))

        then:
        conditions.eventually {
            testClient.secret(secretName).data["secretFoo"] == "secretBaz".bytes
        }

        when:
        deleteSecret(coreV1Api, namespace, secretName)

        then:
        conditions.eventually {
            !testClient.secret(secretName)
        }
    }

    @Client("http://localhost:8889")
    @io.micronaut.context.annotation.Requires(property = "spec.name", value = "InformerSpec")
    static interface TestClient {

        @Get(uri = "/config-maps/{name}", processes = MediaType.APPLICATION_JSON)
        V1ConfigMap configMap(String name)

        @Get(uri = "/secrets/{name}", processes = MediaType.APPLICATION_JSON)
        V1Secret secret(String name)
    }
}
