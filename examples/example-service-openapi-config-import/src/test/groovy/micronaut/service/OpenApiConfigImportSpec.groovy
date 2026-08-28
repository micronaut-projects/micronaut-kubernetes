package micronaut.service

import io.micronaut.context.ApplicationContext
import io.micronaut.context.env.Environment
import io.micronaut.http.MediaType
import io.micronaut.http.annotation.Get
import io.micronaut.http.client.annotation.Client
import io.micronaut.kubernetes.client.openapi.api.AppsV1Api
import io.micronaut.kubernetes.client.openapi.api.CoreV1Api
import io.micronaut.kubernetes.client.openapi.api.RbacAuthorizationV1Api
import io.micronaut.kubernetes.client.openapi.api.VersionApi
import io.micronaut.kubernetes.openapi.test.KubectlPortForward
import io.micronaut.kubernetes.openapi.test.KubernetesModels
import io.micronaut.kubernetes.openapi.test.KubernetesOperations
import io.micronaut.kubernetes.openapi.test.KubernetesSpecification
import io.micronaut.kubernetes.openapi.test.TestUtils
import spock.lang.AutoCleanup
import spock.lang.Requires
import spock.lang.Shared
import spock.util.concurrent.PollingConditions

import static io.micronaut.kubernetes.openapi.test.KubernetesOperations.portForwardService

@Requires({ TestUtils.kubernetesApiAvailable() })
class OpenApiConfigImportSpec extends KubernetesSpecification {

    @Shared
    @AutoCleanup
    KubectlPortForward kubectlPortForward

    @Shared
    ApplicationContext context

    def setupSpec() {
        kubectlPortForward = portForwardService(coreV1Api, "example-service-openapi-config-import", namespace, 8083, 8887)
    }

    def cleanupSpec() {
        if (context != null) {
            context.close()
        }
    }

    @Override
    def setupPrepare() {
        namespace = "micronaut-example-openapi-config-import"
        context = createContext()
        versionApi = context.getBean(VersionApi.class)
        coreV1Api = context.getBean(CoreV1Api.class)
        rbacAuthV1Api = context.getBean(RbacAuthorizationV1Api.class)
        appsV1Api = context.getBean(AppsV1Api.class)
    }

    @Override
    def createResources() {
        createMountedConfigMap()
        createMountedSecretProp()
        createDeployment("example-service-openapi-config-import", "micronaut-kubernetes-example-service-openapi-config-import", 8083, true)
        createService("example-service-openapi-config-import", 8083)
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
        TestClient testClient = context.getBean(TestClient.class)

        expect:
        testClient.config("cm-key-5") == "NOTHING"

        when: "new config map is created"
        KubernetesOperations.createConfigMap(coreV1Api, namespace, KubernetesModels.getConfigMapModel("cm-5", ["cm-key-5": "cm-value-5"]))

        then: "values from the config map are loaded into micronaut properties"
        conditions.eventually {
            testClient.config("cm-key-5") == "cm-value-5"
        }

        when: "content of config map is changed"
        KubernetesOperations.replaceConfigMap(coreV1Api, namespace, KubernetesModels.getConfigMapModel("cm-5", ["cm-key-5": "cm-value-5555"]))

        then: "value is changed in micronaut properties"
        conditions.eventually {
            testClient.config("cm-key-5") == "cm-value-5555"
        }

        when: "config map is deleted"
        KubernetesOperations.deleteConfigMap(coreV1Api, namespace, "cm-5")

        then: "values from that config map are removed from micronaut properties"
        conditions.eventually {
            testClient.config("cm-key-5") == "NOTHING"
        }
    }

    void "test watchable when secreted imported by name"() {
        given:
        PollingConditions conditions = new PollingConditions(timeout: 30, delay: 2)
        TestClient testClient = context.getBean(TestClient.class)

        expect:
        testClient.config("sec-key-5") == "NOTHING"

        when: "new secret is created"
        KubernetesOperations.createSecret(coreV1Api, namespace, KubernetesModels.getSecretModel("sec-5", ["sec-key-5": "sec-value-5".bytes]))

        then: "values from the secret are loaded into micronaut properties"
        conditions.eventually {
            testClient.config("sec-key-5") == "sec-value-5"
        }

        when: "content of secret is changed"
        KubernetesOperations.replaceSecret(coreV1Api, namespace, KubernetesModels.getSecretModel("sec-5", ["sec-key-5": "sec-value-5555".bytes]))

        then: "value is changed in micronaut properties"
        conditions.eventually {
            testClient.config("sec-key-5") == "sec-value-5555"
        }

        when: "secret is deleted"
        KubernetesOperations.deleteSecret(coreV1Api, namespace, "sec-5")

        then: "values from that secret are removed from micronaut properties"
        conditions.eventually {
            testClient.config("sec-key-5") == "NOTHING"
        }
    }

    void "test watchable when config maps imported by labels"() {
        given:
        PollingConditions conditions = new PollingConditions(timeout: 30, delay: 2)
        TestClient testClient = context.getBean(TestClient.class)

        expect:
        testClient.config("cm-key-1") == "NOTHING"
        testClient.config("cm-key-2") == "NOTHING"
        testClient.config("cm-key-3") == "NOTHING"
        testClient.config("cm-key-4") == "NOTHING"

        when: "new config maps are created"
        KubernetesOperations.createConfigMap(coreV1Api, namespace, KubernetesModels.getConfigMapModel("cm-1", ["cm-key-1": "cm-value-1"], ["config-set": "one"]))
        KubernetesOperations.createConfigMap(coreV1Api, namespace, KubernetesModels.getConfigMapModel("cm-2", ["cm-key-2": "cm-value-2"], ["config-set": "one"]))
        KubernetesOperations.createConfigMap(coreV1Api, namespace, KubernetesModels.getConfigMapModel("cm-3", ["cm-key-3": "cm-value-3"], ["config-set": "one"]))
        KubernetesOperations.createConfigMap(coreV1Api, namespace, KubernetesModels.getConfigMapModel("cm-4", ["cm-key-4": "cm-value-4"], ["config-set": "two"]))

        then: "values from those config maps are loaded into micronaut properties"
        conditions.eventually {
            testClient.config("cm-key-1") == "cm-value-1"
            testClient.config("cm-key-2") == "cm-value-2"
            testClient.config("cm-key-3") == "cm-value-3"
            testClient.config("cm-key-4") == "cm-value-4"
        }

        when: "config map label is removed"
        KubernetesOperations.replaceConfigMap(coreV1Api, namespace, KubernetesModels.getConfigMapModel("cm-3", ["cm-key-3": "cm-value-3"]))

        then: "values from that config map are removed from micronaut properties"
        conditions.eventually {
            testClient.config("cm-key-1") == "cm-value-1"
            testClient.config("cm-key-2") == "cm-value-2"
            testClient.config("cm-key-3") == "NOTHING"
            testClient.config("cm-key-4") == "cm-value-4"
        }

        when: "config map is deleted"
        KubernetesOperations.deleteConfigMap(coreV1Api, namespace, "cm-2")

        then: "values from that config map are removed from micronaut properties"
        conditions.eventually {
            testClient.config("cm-key-1") == "cm-value-1"
            testClient.config("cm-key-2") == "NOTHING"
            testClient.config("cm-key-3") == "NOTHING"
            testClient.config("cm-key-4") == "cm-value-4"
        }

        when: "content of watched config map is changed"
        KubernetesOperations.replaceConfigMap(coreV1Api, namespace, KubernetesModels.getConfigMapModel("cm-1", ["cm-key-1": "cm-value-1111"], ["config-set": "one"]))

        then: "values are changed in micronaut properties"
        conditions.eventually {
            testClient.config("cm-key-1") == "cm-value-1111"
            testClient.config("cm-key-2") == "NOTHING"
            testClient.config("cm-key-3") == "NOTHING"
            testClient.config("cm-key-4") == "cm-value-4"
        }

        when: "content of unwatched config map is changed"
        KubernetesOperations.replaceConfigMap(coreV1Api, namespace, KubernetesModels.getConfigMapModel("cm-4", ["cm-key-4": "cm-value-4444"], ["config-set": "two"]))

        then: "values are not changed in micronaut properties"
        conditions.eventually {
            testClient.config("cm-key-1") == "cm-value-1111"
            testClient.config("cm-key-2") == "NOTHING"
            testClient.config("cm-key-3") == "NOTHING"
            testClient.config("cm-key-4") == "cm-value-4"
        }

        when: "config maps are deleted"
        KubernetesOperations.deleteConfigMap(coreV1Api, namespace, "cm-1")
        KubernetesOperations.deleteConfigMap(coreV1Api, namespace, "cm-3")
        KubernetesOperations.deleteConfigMap(coreV1Api, namespace, "cm-4")

        then: "values from those config maps are removed from micronaut properties"
        conditions.eventually {
            testClient.config("cm-key-1") == "NOTHING"
            testClient.config("cm-key-2") == "NOTHING"
            testClient.config("cm-key-3") == "NOTHING"
            testClient.config("cm-key-4") == "NOTHING"
        }
    }

    void "test watchable when secrets imported by labels"() {
        given:
        PollingConditions conditions = new PollingConditions(timeout: 30, delay: 2)
        TestClient testClient = context.getBean(TestClient.class)

        expect:
        testClient.config("sec-key-1") == "NOTHING"
        testClient.config("sec-key-2") == "NOTHING"
        testClient.config("sec-key-3") == "NOTHING"
        testClient.config("sec-key-4") == "NOTHING"

        when: "new secrets are created\""
        KubernetesOperations.createSecret(coreV1Api, namespace, KubernetesModels.getSecretModel("sec-1", ["sec-key-1": "sec-value-1".bytes], ["config-set": "one"]))
        KubernetesOperations.createSecret(coreV1Api, namespace, KubernetesModels.getSecretModel("sec-2", ["sec-key-2": "sec-value-2".bytes], ["config-set": "one"]))
        KubernetesOperations.createSecret(coreV1Api, namespace, KubernetesModels.getSecretModel("sec-3", ["sec-key-3": "sec-value-3".bytes], ["config-set": "one"]))
        KubernetesOperations.createSecret(coreV1Api, namespace, KubernetesModels.getSecretModel("sec-4", ["sec-key-4": "sec-value-4".bytes], ["config-set": "two"]))

        then: "values from those secrets are loaded into micronaut properties"
        conditions.eventually {
            testClient.config("sec-key-1") == "sec-value-1"
            testClient.config("sec-key-2") == "sec-value-2"
            testClient.config("sec-key-3") == "sec-value-3"
            testClient.config("sec-key-4") == "sec-value-4"
        }

        when: "secret label is removed"
        KubernetesOperations.replaceSecret(coreV1Api, namespace, KubernetesModels.getSecretModel("sec-3", ["sec-key-3": "sec-value-3".bytes]))

        then: "values from that secret are removed from micronaut properties"
        conditions.eventually {
            testClient.config("sec-key-1") == "sec-value-1"
            testClient.config("sec-key-2") == "sec-value-2"
            testClient.config("sec-key-3") == "NOTHING"
            testClient.config("sec-key-4") == "sec-value-4"
        }

        when: "secret is deleted"
        KubernetesOperations.deleteSecret(coreV1Api, namespace, "sec-2")

        then: "values from that secret are removed from micronaut properties"
        conditions.eventually {
            testClient.config("sec-key-1") == "sec-value-1"
            testClient.config("sec-key-2") == "NOTHING"
            testClient.config("sec-key-3") == "NOTHING"
            testClient.config("sec-key-4") == "sec-value-4"
        }

        when: "content of watched secret is changed"
        KubernetesOperations.replaceSecret(coreV1Api, namespace, KubernetesModels.getSecretModel("sec-1", ["sec-key-1": "sec-value-1111".bytes], ["config-set": "one"]))

        then: "values are changed in micronaut properties"
        conditions.eventually {
            testClient.config("sec-key-1") == "sec-value-1111"
            testClient.config("sec-key-2") == "NOTHING"
            testClient.config("sec-key-3") == "NOTHING"
            testClient.config("sec-key-4") == "sec-value-4"
        }

        when: "content of unwatched config secret is changed"
        KubernetesOperations.replaceSecret(coreV1Api, namespace, KubernetesModels.getSecretModel("sec-4", ["sec-key-4": "sec-value-4444".bytes], ["config-set": "two"]))

        then: "values are not changed in micronaut properties"
        conditions.eventually {
            testClient.config("sec-key-1") == "sec-value-1111"
            testClient.config("sec-key-2") == "NOTHING"
            testClient.config("sec-key-3") == "NOTHING"
            testClient.config("sec-key-4") == "sec-value-4"
        }

        when: "secrets are deleted"
        KubernetesOperations.deleteSecret(coreV1Api, namespace, "sec-1")
        KubernetesOperations.deleteSecret(coreV1Api, namespace, "sec-3")
        KubernetesOperations.deleteSecret(coreV1Api, namespace, "sec-4")

        then: "values from those secrets are removed from micronaut properties"
        conditions.eventually {
            testClient.config("sec-key-1") == "NOTHING"
            testClient.config("sec-key-2") == "NOTHING"
            testClient.config("sec-key-3") == "NOTHING"
            testClient.config("sec-key-4") == "NOTHING"
        }
    }

    void "test refreshable when config map changed"() {
        given:
        PollingConditions conditions = new PollingConditions(timeout: 30, delay: 2)
        TestClient testClient = context.getBean(TestClient.class)

        expect:
        testClient.count() == 0

        when:
        KubernetesOperations.createConfigMap(coreV1Api, namespace, KubernetesModels.getConfigMapModel("cm-6", ["test-count": "10"]))

        then:
        conditions.eventually {
            testClient.count() == 10
        }

        when:
        KubernetesOperations.replaceConfigMap(coreV1Api, namespace, KubernetesModels.getConfigMapModel("cm-6", ["test-count": "20"]))

        then:
        conditions.eventually {
            testClient.count() == 20
        }

        when:
        KubernetesOperations.deleteConfigMap(coreV1Api, namespace, "cm-6")

        then:
        conditions.eventually {
            testClient.count() == 0
        }
    }

    void "test properties loaded from config map and secret found in mounted volumes"() {
        given:
        TestClient testClient = context.getBean(TestClient.class)

        expect:
        testClient.config("mounted.foo") == "bar"
        testClient.config("mounted-secret-key") == "mounted-secret-value"
    }

    @Client("http://localhost:8887")
    @io.micronaut.context.annotation.Requires(property = "spec.name", value = "ConfigImportSpec")
    static interface TestClient {
        @Get(uri = "/config/var/count", processes = MediaType.TEXT_PLAIN)
        Integer count()

        @Get(uri = "/config/context/{key}", processes = MediaType.TEXT_PLAIN)
        String config(String key)
    }
}
