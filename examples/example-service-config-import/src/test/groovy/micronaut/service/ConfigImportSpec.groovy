package micronaut.service

import io.micronaut.context.ApplicationContext
import io.micronaut.context.env.Environment
import io.micronaut.http.MediaType
import io.micronaut.http.annotation.Get
import io.micronaut.http.client.annotation.Client
import io.micronaut.kubernetes.test.KubectlPortForward
import io.micronaut.kubernetes.test.KubernetesSpecification
import io.micronaut.kubernetes.test.TestUtils
import spock.lang.AutoCleanup
import spock.lang.Requires
import spock.lang.Shared
import spock.util.concurrent.PollingConditions

import static io.micronaut.kubernetes.test.KubernetesModels.getConfigMapModel
import static io.micronaut.kubernetes.test.KubernetesModels.getSecretModel
import static io.micronaut.kubernetes.test.KubernetesModels.getServicePortModel
import static io.micronaut.kubernetes.test.KubernetesModels.getServiceSpecTypeModel
import static io.micronaut.kubernetes.test.KubernetesOperations.createConfigMap
import static io.micronaut.kubernetes.test.KubernetesOperations.createDeploymentFromFile
import static io.micronaut.kubernetes.test.KubernetesOperations.createSecret
import static io.micronaut.kubernetes.test.KubernetesOperations.createService
import static io.micronaut.kubernetes.test.KubernetesOperations.deleteConfigMap
import static io.micronaut.kubernetes.test.KubernetesOperations.deleteSecret
import static io.micronaut.kubernetes.test.KubernetesOperations.portForwardService
import static io.micronaut.kubernetes.test.KubernetesOperations.replaceConfigMap
import static io.micronaut.kubernetes.test.KubernetesOperations.replaceSecret

@Requires({ TestUtils.kubernetesApiAvailable() })
class ConfigImportSpec extends KubernetesSpecification {

    @Shared
    @AutoCleanup
    KubectlPortForward kubectlPortForward

    def setupSpec() {
        kubectlPortForward = portForwardService("example-service-config-import", namespace, 8083, 8887)
    }

    @Override
    def setupFixture(String empty) {
        namespace = "micronaut-example-config-import"
        createNamespaceSafe(namespace)
        createBaseResources(namespace)
        createExampleServiceConfigImportDeployment(namespace)
    }

    def createExampleServiceConfigImportDeployment(String namespace) {
        createDeploymentFromFile(
                loadFileFromClasspath("k8s/example-service-config-import-deployment.yml"),
                "example-service-config-import",
                namespace)
        createService(
                "example-service-config-import",
                namespace,
                getServiceSpecTypeModel(
                        "LoadBalancer",
                        [getServicePortModel(8083, 8083)],
                        ["app": "example-service-config-import"]),
                ["foo": "bar"])
    }

    ApplicationContext createContext() {
        return ApplicationContext.builder().environments(Environment.KUBERNETES)
                .eventsEnabled(false)
                .eagerBeansEnabled(false)
                .deducePackage(false)
                .bootstrapEnvironment(false)
                .deduceCloudEnvironment(false)
                .configImport(false)
                .properties(Map.of("spec.name", "ConfigImportSpec"))
                .start()
    }

    void "read property from config map"() {
        given:
        ApplicationContext context = createContext()
        TestClient testClient = context.getBean(TestClient.class)

        expect:
        testClient.config("enemies.cheat.level") == "noGoodRotten"

        cleanup:
        context?.close()
    }

    void "read property from secret"() {
        given:
        ApplicationContext context = createContext()
        TestClient testClient = context.getBean(TestClient.class)

        expect:
        testClient.config("username") == "my-app"

        cleanup:
        context?.close()
    }

    void "read properties from watchable config maps"() {
        given:
        PollingConditions conditions = new PollingConditions(timeout: 30, delay: 2)
        ApplicationContext context = createContext()
        TestClient testClient = context.getBean(TestClient.class)

        expect:
        testClient.config("cm-key-1") == "NOTHING"
        testClient.config("cm-key-2") == "NOTHING"
        testClient.config("cm-key-3") == "NOTHING"

        when:
        createConfigMap(namespace, getConfigMapModel("cm-1", ["cm-key-1": "cm-value-1"], ["cm-label-key": "cm-label-value"]))
        createConfigMap(namespace, getConfigMapModel("cm-2", ["cm-key-2": "cm-value-2"], ["cm-label-key": "cm-label-value"]))
        createConfigMap(namespace, getConfigMapModel("cm-3", ["cm-key-3": "cm-value-3"], ["cm-label-key": "cm-label-value"]))

        then:
        conditions.eventually {
            testClient.config("cm-key-1") == "cm-value-1"
            testClient.config("cm-key-2") == "cm-value-2"
            testClient.config("cm-key-3") == "cm-value-3"
        }

        when:
        replaceConfigMap(namespace, getConfigMapModel("cm-3", ["cm-key-3": "cm-value-3"]))

        then:
        conditions.eventually {
            testClient.config("cm-key-1") == "cm-value-1"
            testClient.config("cm-key-2") == "cm-value-2"
            testClient.config("cm-key-3") == "NOTHING"
        }

        when:
        deleteConfigMap("cm-2", namespace)

        then:
        conditions.eventually {
            testClient.config("cm-key-1") == "cm-value-1"
            testClient.config("cm-key-2") == "NOTHING"
            testClient.config("cm-key-3") == "NOTHING"
        }

        when:
        replaceConfigMap(namespace, getConfigMapModel("cm-1", ["cm-key-1": "cm-value-1111"], ["cm-label-key": "cm-label-value"]))

        then:
        conditions.eventually {
            testClient.config("cm-key-1") == "cm-value-1111"
            testClient.config("cm-key-2") == "NOTHING"
            testClient.config("cm-key-3") == "NOTHING"
        }

        when:
        deleteConfigMap("cm-1", namespace)
        deleteConfigMap("cm-3", namespace)

        then:
        conditions.eventually {
            testClient.config("cm-key-1") == "NOTHING"
            testClient.config("cm-key-2") == "NOTHING"
            testClient.config("cm-key-3") == "NOTHING"
        }

        cleanup:
        context?.close()
    }

    void "read properties from watchable secrets"() {
        given:
        PollingConditions conditions = new PollingConditions(timeout: 30, delay: 2)
        ApplicationContext context = createContext()
        TestClient testClient = context.getBean(TestClient.class)

        expect:
        testClient.config("sec-key-1") == "NOTHING"
        testClient.config("sec-key-2") == "NOTHING"
        testClient.config("sec-key-3") == "NOTHING"

        when:
        createSecret(namespace, getSecretModel("sec-1", ["sec-key-1": "sec-value-1".bytes], ["sec-label-key": "sec-label-value"]))
        createSecret(namespace, getSecretModel("sec-2", ["sec-key-2": "sec-value-2".bytes], ["sec-label-key": "sec-label-value"]))
        createSecret(namespace, getSecretModel("sec-3", ["sec-key-3": "sec-value-3".bytes], ["sec-label-key": "sec-label-value"]))

        then:
        conditions.eventually {
            testClient.config("sec-key-1") == "sec-value-1"
            testClient.config("sec-key-2") == "sec-value-2"
            testClient.config("sec-key-3") == "sec-value-3"
        }

        when:
        replaceSecret(namespace, getSecretModel("sec-3", ["sec-key-3": "sec-value-3".bytes]))

        then:
        conditions.eventually {
            testClient.config("sec-key-1") == "sec-value-1"
            testClient.config("sec-key-2") == "sec-value-2"
            testClient.config("sec-key-3") == "NOTHING"
        }

        when:
        deleteSecret("sec-2", namespace)

        then:
        conditions.eventually {
            testClient.config("sec-key-1") == "sec-value-1"
            testClient.config("sec-key-2") == "NOTHING"
            testClient.config("sec-key-3") == "NOTHING"
        }

        when:
        replaceSecret(namespace, getSecretModel("sec-1", ["sec-key-1": "sec-value-1111".bytes], ["sec-label-key": "sec-label-value"]))

        then:
        conditions.eventually {
            testClient.config("sec-key-1") == "sec-value-1111"
            testClient.config("sec-key-2") == "NOTHING"
            testClient.config("sec-key-3") == "NOTHING"
        }

        when:
        deleteSecret("sec-1", namespace)
        deleteSecret("sec-3", namespace)

        then:
        conditions.eventually {
            testClient.config("sec-key-1") == "NOTHING"
            testClient.config("sec-key-2") == "NOTHING"
            testClient.config("sec-key-3") == "NOTHING"
        }

        cleanup:
        context?.close()
    }

    @Client("http://localhost:8887")
    @io.micronaut.context.annotation.Requires(property = "spec.name", value = "ConfigImportSpec")
    static interface TestClient {
        @Get(uri = "/config/var/lives", processes = MediaType.TEXT_PLAIN)
        String lives()

        @Get(uri = "/config/context/{key}", processes = MediaType.TEXT_PLAIN)
        String config(String key)
    }
}
