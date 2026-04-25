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

    void "test watchable when config map imported by name"() {
        given:
        PollingConditions conditions = new PollingConditions(timeout: 30, delay: 2)
        ApplicationContext context = createContext()
        TestClient testClient = context.getBean(TestClient.class)

        expect:
        testClient.config("cm-key-5") == "NOTHING"

        when: "new config map is created"
        createConfigMap(namespace, getConfigMapModel("cm-5", ["cm-key-5": "cm-value-5"]))

        then: "values from the config map are loaded into micronaut properties"
        conditions.eventually {
            testClient.config("cm-key-5") == "cm-value-5"
        }

        when: "config map is deleted"
        deleteConfigMap("cm-5", namespace)

        then: "values from that config map are removed from micronaut properties"
        conditions.eventually {
            testClient.config("cm-key-5") == "NOTHING"
        }

        cleanup:
        context?.close()
    }

    void "test watchable when secreted imported by name"() {
        given:
        PollingConditions conditions = new PollingConditions(timeout: 30, delay: 2)
        ApplicationContext context = createContext()
        TestClient testClient = context.getBean(TestClient.class)

        expect:
        testClient.config("sec-key-5") == "NOTHING"

        when: "new secret is created"
        createSecret(namespace, getSecretModel("sec-5", ["sec-key-5": "sec-value-5".bytes]))

        then: "values from the secret are loaded into micronaut properties"
        conditions.eventually {
            testClient.config("sec-key-5") == "sec-value-5"
        }

        when: "secret is deleted"
        deleteSecret("sec-5", namespace)

        then: "values from that secret are removed from micronaut properties"
        conditions.eventually {
            testClient.config("sec-key-5") == "NOTHING"
        }

        cleanup:
        context?.close()
    }

    void "test watchable when config maps imported by labels"() {
        given:
        PollingConditions conditions = new PollingConditions(timeout: 30, delay: 2)
        ApplicationContext context = createContext()
        TestClient testClient = context.getBean(TestClient.class)

        expect:
        testClient.config("cm-key-1") == "NOTHING"
        testClient.config("cm-key-2") == "NOTHING"
        testClient.config("cm-key-3") == "NOTHING"
        testClient.config("cm-key-4") == "NOTHING"

        when: "new config maps are created"
        createConfigMap(namespace, getConfigMapModel("cm-1", ["cm-key-1": "cm-value-1"], ["watchable": "true"]))
        createConfigMap(namespace, getConfigMapModel("cm-2", ["cm-key-2": "cm-value-2"], ["watchable": "true"]))
        createConfigMap(namespace, getConfigMapModel("cm-3", ["cm-key-3": "cm-value-3"], ["watchable": "true"]))
        createConfigMap(namespace, getConfigMapModel("cm-4", ["cm-key-4": "cm-value-4"], ["watchable": "false"]))

        then: "values from those config maps are loaded into micronaut properties"
        conditions.eventually {
            testClient.config("cm-key-1") == "cm-value-1"
            testClient.config("cm-key-2") == "cm-value-2"
            testClient.config("cm-key-3") == "cm-value-3"
            testClient.config("cm-key-4") == "cm-value-4"
        }

        when: "config map label is removed"
        replaceConfigMap(namespace, getConfigMapModel("cm-3", ["cm-key-3": "cm-value-3"]))

        then: "values from that config map are removed from micronaut properties"
        conditions.eventually {
            testClient.config("cm-key-1") == "cm-value-1"
            testClient.config("cm-key-2") == "cm-value-2"
            testClient.config("cm-key-3") == "NOTHING"
            testClient.config("cm-key-4") == "cm-value-4"
        }

        when: "config map is deleted"
        deleteConfigMap("cm-2", namespace)

        then: "values from that config map are removed from micronaut properties"
        conditions.eventually {
            testClient.config("cm-key-1") == "cm-value-1"
            testClient.config("cm-key-2") == "NOTHING"
            testClient.config("cm-key-3") == "NOTHING"
            testClient.config("cm-key-4") == "cm-value-4"
        }

        when: "content of watched config map is changed"
        replaceConfigMap(namespace, getConfigMapModel("cm-1", ["cm-key-1": "cm-value-1111"], ["watchable": "true"]))

        then: "values are changed in micronaut properties"
        conditions.eventually {
            testClient.config("cm-key-1") == "cm-value-1111"
            testClient.config("cm-key-2") == "NOTHING"
            testClient.config("cm-key-3") == "NOTHING"
            testClient.config("cm-key-4") == "cm-value-4"
        }

        when: "content of unwatched config map is changed"
        replaceConfigMap(namespace, getConfigMapModel("cm-4", ["cm-key-4": "cm-value-4444"], ["watchable": "false"]))

        then: "values are not changed in micronaut properties"
        conditions.eventually {
            testClient.config("cm-key-1") == "cm-value-1111"
            testClient.config("cm-key-2") == "NOTHING"
            testClient.config("cm-key-3") == "NOTHING"
            testClient.config("cm-key-4") == "cm-value-4"
        }

        when: "config maps are deleted"
        deleteConfigMap("cm-1", namespace)
        deleteConfigMap("cm-3", namespace)
        deleteConfigMap("cm-4", namespace)

        then: "values from those config maps are removed from micronaut properties"
        conditions.eventually {
            testClient.config("cm-key-1") == "NOTHING"
            testClient.config("cm-key-2") == "NOTHING"
            testClient.config("cm-key-3") == "NOTHING"
            testClient.config("cm-key-4") == "NOTHING"
        }

        cleanup:
        context?.close()
    }

    void "test watchable when secrets imported by labels"() {
        given:
        PollingConditions conditions = new PollingConditions(timeout: 30, delay: 2)
        ApplicationContext context = createContext()
        TestClient testClient = context.getBean(TestClient.class)

        expect:
        testClient.config("sec-key-1") == "NOTHING"
        testClient.config("sec-key-2") == "NOTHING"
        testClient.config("sec-key-3") == "NOTHING"
        testClient.config("sec-key-4") == "NOTHING"

        when: "new secrets are created\""
        createSecret(namespace, getSecretModel("sec-1", ["sec-key-1": "sec-value-1".bytes], ["watchable": "true"]))
        createSecret(namespace, getSecretModel("sec-2", ["sec-key-2": "sec-value-2".bytes], ["watchable": "true"]))
        createSecret(namespace, getSecretModel("sec-3", ["sec-key-3": "sec-value-3".bytes], ["watchable": "true"]))
        createSecret(namespace, getSecretModel("sec-4", ["sec-key-4": "sec-value-4".bytes], ["watchable": "false"]))

        then: "values from those secrets are loaded into micronaut properties"
        conditions.eventually {
            testClient.config("sec-key-1") == "sec-value-1"
            testClient.config("sec-key-2") == "sec-value-2"
            testClient.config("sec-key-3") == "sec-value-3"
            testClient.config("sec-key-4") == "sec-value-4"
        }

        when: "secret label is removed"
        replaceSecret(namespace, getSecretModel("sec-3", ["sec-key-3": "sec-value-3".bytes]))

        then: "values from that secret are removed from micronaut properties"
        conditions.eventually {
            testClient.config("sec-key-1") == "sec-value-1"
            testClient.config("sec-key-2") == "sec-value-2"
            testClient.config("sec-key-3") == "NOTHING"
            testClient.config("sec-key-4") == "sec-value-4"
        }

        when: "secret is deleted"
        deleteSecret("sec-2", namespace)

        then: "values from that secret are removed from micronaut properties"
        conditions.eventually {
            testClient.config("sec-key-1") == "sec-value-1"
            testClient.config("sec-key-2") == "NOTHING"
            testClient.config("sec-key-3") == "NOTHING"
            testClient.config("sec-key-4") == "sec-value-4"
        }

        when: "content of watched secret is changed"
        replaceSecret(namespace, getSecretModel("sec-1", ["sec-key-1": "sec-value-1111".bytes], ["watchable": "true"]))

        then: "values are changed in micronaut properties"
        conditions.eventually {
            testClient.config("sec-key-1") == "sec-value-1111"
            testClient.config("sec-key-2") == "NOTHING"
            testClient.config("sec-key-3") == "NOTHING"
            testClient.config("sec-key-4") == "sec-value-4"
        }

        when: "content of unwatched config secret is changed"
        replaceSecret(namespace, getSecretModel("sec-4", ["sec-key-4": "sec-value-4444".bytes], ["watchable": "false"]))

        then: "values are not changed in micronaut properties"
        conditions.eventually {
            testClient.config("sec-key-1") == "sec-value-1111"
            testClient.config("sec-key-2") == "NOTHING"
            testClient.config("sec-key-3") == "NOTHING"
            testClient.config("sec-key-4") == "sec-value-4"
        }

        when: "secrets are deleted"
        deleteSecret("sec-1", namespace)
        deleteSecret("sec-3", namespace)
        deleteSecret("sec-4", namespace)

        then: "values from those secrets are removed from micronaut properties"
        conditions.eventually {
            testClient.config("sec-key-1") == "NOTHING"
            testClient.config("sec-key-2") == "NOTHING"
            testClient.config("sec-key-3") == "NOTHING"
            testClient.config("sec-key-4") == "NOTHING"
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
