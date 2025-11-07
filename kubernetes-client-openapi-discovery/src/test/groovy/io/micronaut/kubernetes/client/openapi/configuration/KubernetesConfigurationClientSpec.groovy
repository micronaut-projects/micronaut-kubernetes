package io.micronaut.kubernetes.client.openapi.configuration

import io.micronaut.context.ApplicationContext
import io.micronaut.context.env.Environment
import io.micronaut.context.exceptions.ConfigurationException
import io.micronaut.core.io.scan.ClassPathResourceLoader
import io.micronaut.kubernetes.client.openapi.api.CoreV1Api
import io.micronaut.kubernetes.client.openapi.model.V1ConfigMap
import io.micronaut.kubernetes.client.openapi.model.V1Pod
import io.micronaut.kubernetes.client.openapi.model.V1PodSpec
import io.micronaut.kubernetes.client.openapi.model.V1Secret
import io.micronaut.kubernetes.openapi.test.K3sContainerSpec
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import spock.util.concurrent.PollingConditions

import java.nio.file.Path

import static io.micronaut.kubernetes.openapi.test.KubernetesModels.getConfigMapModel
import static io.micronaut.kubernetes.openapi.test.KubernetesModels.getContainerModel
import static io.micronaut.kubernetes.openapi.test.KubernetesModels.getNamespaceModel
import static io.micronaut.kubernetes.openapi.test.KubernetesModels.getPodModel
import static io.micronaut.kubernetes.openapi.test.KubernetesModels.getPodSpecModel
import static io.micronaut.kubernetes.openapi.test.KubernetesModels.getSecretModel
import static io.micronaut.kubernetes.openapi.test.KubernetesOperations.createConfigMap
import static io.micronaut.kubernetes.openapi.test.KubernetesOperations.createNamespace
import static io.micronaut.kubernetes.openapi.test.KubernetesOperations.createPod
import static io.micronaut.kubernetes.openapi.test.KubernetesOperations.createSecret
import static io.micronaut.kubernetes.openapi.test.KubernetesOperations.deleteConfigMap
import static io.micronaut.kubernetes.openapi.test.KubernetesOperations.deleteSecret
import static io.micronaut.kubernetes.openapi.test.KubernetesOperations.replaceConfigMap
import static io.micronaut.kubernetes.openapi.test.KubernetesOperations.replaceSecret

class KubernetesConfigurationClientSpec extends K3sContainerSpec {

    private static final Logger LOG = LoggerFactory.getLogger(KubernetesConfigurationClientSpec.class)

    private static final NAMESPACE_NAME_1 = "micronaut-service-configuration-1"
    private static final NAMESPACE_NAME_2 = "micronaut-service-configuration-2"

    private static final GAME_CONFIG_JSON_PS_NAME = createConfigMapPropSourceName("game-config-json")
    private static final GAME_CONFIG_PROPERTIES_PS_NAME = createConfigMapPropSourceName("game-config-properties")
    private static final GAME_CONFIG_YML_PS_NAME = createConfigMapPropSourceName("game-config-yml")
    private static final LITERAL_CONFIG_PS_NAME = createConfigMapPropSourceName("literal-config")
    private static final CONFIG_MAP_LIST_PS_NAME = "Kubernetes V1ConfigMapList"
    private static final CONFIG_MAP_LIST_PS_KEY = "v1configmaplist.resource-version"

    private static final TEST_SECRET_1_PS_NAME = createSecretPropSourceName("test-secret-1")
    private static final TEST_SECRET_2_PS_NAME = createSecretPropSourceName("test-secret-2")
    private static final TEST_SECRET_3_PS_NAME = createSecretPropSourceName("test-secret-3")
    private static final TEST_SECRET_4_PS_NAME = createSecretPropSourceName("test-secret-4")
    private static final SECRET_LIST_PS_NAME = "Kubernetes V1SecretList"
    private static final SECRET_LIST_PS_KEY = "v1secretlist.resource-version"

    // pod name should be equal to value that is passed as env variable to tests in build.gradle file
    private static final POD_NAME = "test-pod"

    void setup() {
        KubernetesConfigurationClient.propertySourceCache.clear()
    }

    @Override
    Logger getLogger() {
        return LOG
    }

    @Override
    def setupKubernetes(ApplicationContext context) {
        CoreV1Api api = context.getBean(CoreV1Api.class)
        createNamespace(api, getNamespaceModel(NAMESPACE_NAME_1))

        createConfigMap(api, NAMESPACE_NAME_1, getJsonConfigMap())
        createConfigMap(api, NAMESPACE_NAME_1, getPropertiesConfigMap())
        createConfigMap(api, NAMESPACE_NAME_1, getYmlConfigMap())
        createConfigMap(api, NAMESPACE_NAME_1, getLiteralConfigMap())

        V1Secret secret1 = getSecretModel("test-secret-1", ["username": "user".bytes, "password": "pass".bytes])
        createSecret(api, NAMESPACE_NAME_1, secret1)
        V1Secret secret2 = getSecretModel("test-secret-2", ["secretKey2": "secretValue2".bytes], ["podLabelKey1": "podLabelValue1"])
        createSecret(api, NAMESPACE_NAME_1, secret2)
        V1Secret secret3 = getSecretModel("test-secret-3", ["secretKey3": "secretValue3".bytes], ["podLabelKey2": "podLabelValue2"])
        createSecret(api, NAMESPACE_NAME_1, secret3)

        V1PodSpec podSpec1 = getPodSpecModel([getContainerModel("test-cont-1")])
        V1Pod pod1 = getPodModel(POD_NAME, podSpec1, ["podLabelKey1": "podLabelValue1", "podLabelKey2": "podLabelValue2"])
        createPod(api, NAMESPACE_NAME_1, pod1)

        createNamespace(api, getNamespaceModel(NAMESPACE_NAME_2))

        createConfigMap(api, NAMESPACE_NAME_2, getLiteralConfigMap())

        V1Secret secret4 = getSecretModel("test-secret-4", ["secretKey4": "secretValue4".bytes])
        createSecret(api, NAMESPACE_NAME_2, secret4)

        V1PodSpec podSpec2 = getPodSpecModel([getContainerModel("test-cont-2")])
        V1Pod pod2 = getPodModel(POD_NAME, podSpec2, ["podLabelKey20": "podLabelValue20"])
        createPod(api, NAMESPACE_NAME_2, pod2)
    }

    void "read json, properties, yml and literal config maps"() {
        given:
        ApplicationContext context = ApplicationContext.run([
                "micronaut.config-client.enabled"      : true,
                "kubernetes.client.kube-config-path"   : "file:" + kubeConfigFile.toString(),
                "kubernetes.client.namespace"          : NAMESPACE_NAME_1,
                "kubernetes.client.config-maps.enabled": true,
                "kubernetes.client.config-maps.use-api": true,
                "kubernetes.client.config-maps.watch"  : false
        ], Environment.KUBERNETES)

        when:
        def propertySources = KubernetesConfigurationClient.propertySourceCache

        then:
        propertySources.size() == 5

        propertySources.get(GAME_CONFIG_JSON_PS_NAME) != null
        propertySources.get(GAME_CONFIG_JSON_PS_NAME).size() == 5
        propertySources.get(GAME_CONFIG_JSON_PS_NAME).get("enemies") == "monsters"
        propertySources.get(GAME_CONFIG_JSON_PS_NAME).get("lives") == 1
        propertySources.get(GAME_CONFIG_JSON_PS_NAME).get("secret.code.passphrase") == "mon"
        propertySources.get(GAME_CONFIG_JSON_PS_NAME).get("secret.code.allowed") == true
        propertySources.get(GAME_CONFIG_JSON_PS_NAME).contains(createResVersionConfigMapPropName("game-config-json"))

        propertySources.get(GAME_CONFIG_PROPERTIES_PS_NAME) != null
        propertySources.get(GAME_CONFIG_PROPERTIES_PS_NAME).size() == 5
        propertySources.get(GAME_CONFIG_PROPERTIES_PS_NAME).get("enemies") == "zombies"
        propertySources.get(GAME_CONFIG_PROPERTIES_PS_NAME).get("lives") == "2"
        propertySources.get(GAME_CONFIG_PROPERTIES_PS_NAME).get("secret.code.passphrase") == "zom"
        propertySources.get(GAME_CONFIG_PROPERTIES_PS_NAME).get("secret.code.allowed") == "false"
        propertySources.get(GAME_CONFIG_PROPERTIES_PS_NAME).contains(createResVersionConfigMapPropName("game-config-properties"))

        propertySources.get(GAME_CONFIG_YML_PS_NAME) != null
        propertySources.get(GAME_CONFIG_YML_PS_NAME).size() == 5
        propertySources.get(GAME_CONFIG_YML_PS_NAME).get("enemies") == "aliens"
        propertySources.get(GAME_CONFIG_YML_PS_NAME).get("lives") == 3
        propertySources.get(GAME_CONFIG_YML_PS_NAME).get("secret.code.passphrase") == "ali"
        propertySources.get(GAME_CONFIG_YML_PS_NAME).get("secret.code.allowed") == true
        propertySources.get(GAME_CONFIG_YML_PS_NAME).contains(createResVersionConfigMapPropName("game-config-yml"))

        propertySources.get(LITERAL_CONFIG_PS_NAME) != null
        propertySources.get(LITERAL_CONFIG_PS_NAME).size() == 3
        propertySources.get(LITERAL_CONFIG_PS_NAME).get("special.how") == "very"
        propertySources.get(LITERAL_CONFIG_PS_NAME).get("special.type") == "charm"
        propertySources.get(LITERAL_CONFIG_PS_NAME).contains(createResVersionConfigMapPropName("literal-config"))

        propertySources.get(CONFIG_MAP_LIST_PS_NAME) != null
        propertySources.get(CONFIG_MAP_LIST_PS_NAME).size() == 1
        propertySources.get(CONFIG_MAP_LIST_PS_NAME).contains(CONFIG_MAP_LIST_PS_KEY)

        cleanup:
        context.close()
    }

    void "read config maps and watch enabled"() {
        given:
        ApplicationContext context = ApplicationContext.run([
                "micronaut.config-client.enabled"      : true,
                "kubernetes.client.kube-config-path"   : "file:" + kubeConfigFile.toString(),
                "kubernetes.client.namespace"          : NAMESPACE_NAME_2,
                "kubernetes.client.config-maps.enabled": true,
                "kubernetes.client.config-maps.use-api": true,
                "kubernetes.client.config-maps.watch"  : true
        ], Environment.KUBERNETES)

        def watcher = context.getBean(KubernetesConfigMapWatcher.class)
        watcher.onApplicationEvent(null)

        def api = context.getBean(CoreV1Api.class)
        def conditions = new PollingConditions(timeout: 2)

        when:
        def propertySources = KubernetesConfigurationClient.propertySourceCache

        then:
        propertySources.size() == 2
        propertySources.get(LITERAL_CONFIG_PS_NAME) != null
        propertySources.get(LITERAL_CONFIG_PS_NAME).size() == 3
        propertySources.get(LITERAL_CONFIG_PS_NAME).get("special.how") == "very"
        propertySources.get(LITERAL_CONFIG_PS_NAME).get("special.type") == "charm"
        propertySources.get(LITERAL_CONFIG_PS_NAME).contains(createResVersionConfigMapPropName("literal-config"))
        propertySources.get(CONFIG_MAP_LIST_PS_NAME) != null
        propertySources.get(CONFIG_MAP_LIST_PS_NAME).size() == 1
        propertySources.get(CONFIG_MAP_LIST_PS_NAME).contains(CONFIG_MAP_LIST_PS_KEY)

        when: "new config map is created"
        def newConfigMapPropSourceName = createConfigMapPropSourceName("config-map-1")
        def newConfigMap = getConfigMapModel("config-map-1", ["test-key-1": "test-value-1", "test-key-2": "test-value-2"])
        createConfigMap(api, NAMESPACE_NAME_2, newConfigMap)

        then: "new property source is created"
        conditions.eventually {
            with(KubernetesConfigurationClient.propertySourceCache) {
                it.size() == 3
                it.get(newConfigMapPropSourceName) != null
                it.get(newConfigMapPropSourceName).size() == 3
                it.get(newConfigMapPropSourceName).get("test-key-1") == "test-value-1"
                it.get(newConfigMapPropSourceName).get("test-key-2") == "test-value-2"
                it.get(newConfigMapPropSourceName).contains(createResVersionConfigMapPropName("config-map-1"))
            }
        }

        when: "existing config map is replaced"
        def updatedConfigMap = getConfigMapModel("config-map-1", ["test-key-1": "test-value-10", "test-key-3": "test-value-3"])
        replaceConfigMap(api, NAMESPACE_NAME_2, updatedConfigMap)

        then: "existing property source is updated"
        conditions.eventually {
            with(KubernetesConfigurationClient.propertySourceCache) {
                it.size() == 3
                it.get(newConfigMapPropSourceName) != null
                it.get(newConfigMapPropSourceName).size() == 3
                it.get(newConfigMapPropSourceName).get("test-key-1") == "test-value-10"
                it.get(newConfigMapPropSourceName).get("test-key-3") == "test-value-3"
                it.get(newConfigMapPropSourceName).contains(createResVersionConfigMapPropName("config-map-1"))
            }
        }

        when: "existing config map is deleted"
        deleteConfigMap(api, NAMESPACE_NAME_2, "config-map-1")

        then: "existing property source is deleted"
        conditions.eventually {
            with(KubernetesConfigurationClient.propertySourceCache) {
                it.size() == 2
                it.get(newConfigMapPropSourceName) == null
            }
        }

        cleanup:
        context.close()
    }

    void "test includes filter for config maps"() {
        given:
        ApplicationContext context = ApplicationContext.run([
                "micronaut.config-client.enabled"       : true,
                "kubernetes.client.kube-config-path"    : "file:" + kubeConfigFile.toString(),
                "kubernetes.client.namespace"           : NAMESPACE_NAME_1,
                "kubernetes.client.config-maps.enabled" : true,
                "kubernetes.client.config-maps.use-api" : true,
                "kubernetes.client.config-maps.watch"   : false,
                "kubernetes.client.config-maps.includes": ["game-config-json", "game-config-yml"]
        ], Environment.KUBERNETES)

        when:
        def propertySources = KubernetesConfigurationClient.propertySourceCache

        then:
        propertySources.size() == 3
        propertySources.get(GAME_CONFIG_JSON_PS_NAME) != null
        propertySources.get(GAME_CONFIG_JSON_PS_NAME).get("enemies") == "monsters"
        propertySources.get(GAME_CONFIG_YML_PS_NAME) != null
        propertySources.get(GAME_CONFIG_YML_PS_NAME).get("enemies") == "aliens"
        propertySources.get(CONFIG_MAP_LIST_PS_NAME) != null
        propertySources.get(CONFIG_MAP_LIST_PS_NAME).contains(CONFIG_MAP_LIST_PS_KEY)

        cleanup:
        context.close()
    }

    void "test excludes filter for config maps"() {
        given:
        ApplicationContext context = ApplicationContext.run([
                "micronaut.config-client.enabled"       : true,
                "kubernetes.client.kube-config-path"    : "file:" + kubeConfigFile.toString(),
                "kubernetes.client.namespace"           : NAMESPACE_NAME_1,
                "kubernetes.client.config-maps.enabled" : true,
                "kubernetes.client.config-maps.use-api" : true,
                "kubernetes.client.config-maps.watch"   : false,
                "kubernetes.client.config-maps.excludes": ["game-config-json", "game-config-yml"]
        ], Environment.KUBERNETES)

        when:
        def propertySources = KubernetesConfigurationClient.propertySourceCache

        then:
        propertySources.size() == 3
        propertySources.get(GAME_CONFIG_PROPERTIES_PS_NAME) != null
        propertySources.get(GAME_CONFIG_PROPERTIES_PS_NAME).get("enemies") == "zombies"
        propertySources.get(LITERAL_CONFIG_PS_NAME) != null
        propertySources.get(LITERAL_CONFIG_PS_NAME).get("special.how") == "very"
        propertySources.get(CONFIG_MAP_LIST_PS_NAME) != null
        propertySources.get(CONFIG_MAP_LIST_PS_NAME).contains(CONFIG_MAP_LIST_PS_KEY)

        cleanup:
        context.close()
    }

    void "test multiple filters for config maps"() {
        given:
        ApplicationContext context = ApplicationContext.run([
                "micronaut.config-client.enabled"       : true,
                "kubernetes.client.kube-config-path"    : "file:" + kubeConfigFile.toString(),
                "kubernetes.client.namespace"           : NAMESPACE_NAME_1,
                "kubernetes.client.config-maps.enabled" : true,
                "kubernetes.client.config-maps.use-api" : true,
                "kubernetes.client.config-maps.watch"   : false,
                "kubernetes.client.config-maps.includes": ["game-config-json", "game-config-yml"],
                "kubernetes.client.config-maps.excludes": ["game-config-yml"]
        ], Environment.KUBERNETES)

        when:
        def propertySources = KubernetesConfigurationClient.propertySourceCache

        then:
        propertySources.size() == 2
        propertySources.get(GAME_CONFIG_JSON_PS_NAME) != null
        propertySources.get(GAME_CONFIG_JSON_PS_NAME).get("enemies") == "monsters"
        propertySources.get(CONFIG_MAP_LIST_PS_NAME) != null
        propertySources.get(CONFIG_MAP_LIST_PS_NAME).contains(CONFIG_MAP_LIST_PS_KEY)

        cleanup:
        context.close()
    }

    void "test label filter for config maps"() {
        given:
        ApplicationContext context = ApplicationContext.run([
                "micronaut.config-client.enabled"      : true,
                "kubernetes.client.kube-config-path"   : "file:" + kubeConfigFile.toString(),
                "kubernetes.client.namespace"          : NAMESPACE_NAME_1,
                "kubernetes.client.config-maps.enabled": true,
                "kubernetes.client.config-maps.use-api": true,
                "kubernetes.client.config-maps.watch"  : false,
                "kubernetes.client.config-maps.labels" : ["podLabelKey2": "podLabelValue2"]
        ], Environment.KUBERNETES)

        when:
        def propertySources = KubernetesConfigurationClient.propertySourceCache

        then:
        propertySources.size() == 2
        propertySources.get(GAME_CONFIG_PROPERTIES_PS_NAME) != null
        propertySources.get(GAME_CONFIG_PROPERTIES_PS_NAME).get("enemies") == "zombies"
        propertySources.get(CONFIG_MAP_LIST_PS_NAME) != null
        propertySources.get(CONFIG_MAP_LIST_PS_NAME).contains(CONFIG_MAP_LIST_PS_KEY)

        cleanup:
        context.close()
    }

    void "test pod label key filter for config maps"() {
        given:
        ApplicationContext context = ApplicationContext.run([
                "micronaut.config-client.enabled"         : true,
                "kubernetes.client.kube-config-path"      : "file:" + kubeConfigFile.toString(),
                "kubernetes.client.namespace"             : NAMESPACE_NAME_1,
                "kubernetes.client.config-maps.enabled"   : true,
                "kubernetes.client.config-maps.use-api"   : true,
                "kubernetes.client.config-maps.watch"     : false,
                "kubernetes.client.config-maps.pod-labels": ["podLabelKey1"]
        ], Environment.KUBERNETES)

        when:
        def propertySources = KubernetesConfigurationClient.propertySourceCache

        then:
        System.getenv("KUBERNETES_SERVICE_HOST") == "localhost"
        System.getenv("HOSTNAME") == POD_NAME
        propertySources.size() == 2
        propertySources.get(GAME_CONFIG_JSON_PS_NAME) != null
        propertySources.get(GAME_CONFIG_JSON_PS_NAME).get("enemies") == "monsters"
        propertySources.get(CONFIG_MAP_LIST_PS_NAME) != null
        propertySources.get(CONFIG_MAP_LIST_PS_NAME).contains(CONFIG_MAP_LIST_PS_KEY)

        cleanup:
        context.close()
    }

    void "test pod label key filter failure when exceptionOnPodLabelsMissing is enabled for config maps"() {
        given:
        def properties = [
                "micronaut.config-client.enabled"                              : true,
                "kubernetes.client.kube-config-path"                           : "file:" + kubeConfigFile.toString(),
                "kubernetes.client.namespace"                                  : NAMESPACE_NAME_1,
                "kubernetes.client.config-maps.enabled"                        : true,
                "kubernetes.client.config-maps.use-api"                        : true,
                "kubernetes.client.config-maps.watch"                          : false,
                "kubernetes.client.config-maps.pod-labels"                     : ["podLabelKey1", "podLabelKey100"],
                "kubernetes.client.config-maps.exception-on-pod-labels-missing": true
        ]

        when:
        ApplicationContext.run(properties, Environment.KUBERNETES)

        then:
        System.getenv("KUBERNETES_SERVICE_HOST") == "localhost"
        System.getenv("HOSTNAME") == POD_NAME
        def e = thrown(ConfigurationException)
        e.message.startsWith("Pod metadata does not contain label")
    }

    void "test reading of config maps from mounted volumes"() {
        given:
        def loader = ClassPathResourceLoader.defaultLoader(this.class.getClassLoader())
        def mountedJsonUrl = loader.getResource("classpath:kubernetes/config-maps/mounted.json")
                .orElseThrow(() -> new FileNotFoundException("File 'kubernetes/config-maps/mounted.json' not found on classpath"))
        Path parentPath = Path.of(mountedJsonUrl.toURI()).parent
        ApplicationContext context = ApplicationContext.run([
                "micronaut.config-client.enabled"      : true,
                "kubernetes.client.kube-config-path"   : "file:" + kubeConfigFile.toString(),
                "kubernetes.client.namespace"          : NAMESPACE_NAME_1,
                "kubernetes.client.config-maps.enabled": true,
                "kubernetes.client.config-maps.use-api": false,
                "kubernetes.client.config-maps.watch"  : false,
                "kubernetes.client.config-maps.paths"  : [parentPath.toString()]
        ], Environment.KUBERNETES)

        when:
        def propertySources = KubernetesConfigurationClient.propertySourceCache
        def jsonPropertySourceKeyOpt = propertySources.keySet().stream().filter(key -> key.contains("mounted.json")).findFirst()
        def ymlPropertySourceKeyOpt = propertySources.keySet().stream().filter(key -> key.contains("mounted.yml")).findFirst()

        then:
        propertySources.size() == 2
        jsonPropertySourceKeyOpt.isPresent()
        propertySources.get(jsonPropertySourceKeyOpt.get()).size() == 1
        propertySources.get(jsonPropertySourceKeyOpt.get()).get("foo2") == "bar2"
        ymlPropertySourceKeyOpt.isPresent()
        propertySources.get(ymlPropertySourceKeyOpt.get()).size() == 1
        propertySources.get(ymlPropertySourceKeyOpt.get()).get("mounted.foo1") == "bar1"

        cleanup:
        context.close()
    }

    void "read secrets"() {
        given:
        ApplicationContext context = ApplicationContext.run([
                "micronaut.config-client.enabled"      : true,
                "kubernetes.client.kube-config-path"   : "file:" + kubeConfigFile.toString(),
                "kubernetes.client.namespace"          : NAMESPACE_NAME_1,
                "kubernetes.client.config-maps.enabled": false,
                "kubernetes.client.secrets.enabled"    : true,
                "kubernetes.client.secrets.use-api"    : true,
                "kubernetes.client.secrets.watch"      : false
        ], Environment.KUBERNETES)

        when:
        def propertySources = KubernetesConfigurationClient.propertySourceCache

        then:
        propertySources.size() == 4

        propertySources.get(TEST_SECRET_1_PS_NAME) != null
        propertySources.get(TEST_SECRET_1_PS_NAME).size() == 3
        propertySources.get(TEST_SECRET_1_PS_NAME).get("username") == "user"
        propertySources.get(TEST_SECRET_1_PS_NAME).get("password") == "pass"
        propertySources.get(TEST_SECRET_1_PS_NAME).contains(createResVersionSecretPropName("test-secret-1"))

        propertySources.get(TEST_SECRET_2_PS_NAME) != null
        propertySources.get(TEST_SECRET_2_PS_NAME).size() == 2
        propertySources.get(TEST_SECRET_2_PS_NAME).get("secretKey2") == "secretValue2"
        propertySources.get(TEST_SECRET_2_PS_NAME).contains(createResVersionSecretPropName("test-secret-2"))

        propertySources.get(TEST_SECRET_3_PS_NAME) != null
        propertySources.get(TEST_SECRET_3_PS_NAME).size() == 2
        propertySources.get(TEST_SECRET_3_PS_NAME).get("secretKey3") == "secretValue3"
        propertySources.get(TEST_SECRET_3_PS_NAME).contains(createResVersionSecretPropName("test-secret-3"))

        propertySources.get(SECRET_LIST_PS_NAME) != null
        propertySources.get(SECRET_LIST_PS_NAME).size() == 1
        propertySources.get(SECRET_LIST_PS_NAME).contains(SECRET_LIST_PS_KEY)

        cleanup:
        context.close()
    }

    void "read secrets and watch enabled"() {
        given:
        ApplicationContext context = ApplicationContext.run([
                "micronaut.config-client.enabled"      : true,
                "kubernetes.client.kube-config-path"   : "file:" + kubeConfigFile.toString(),
                "kubernetes.client.namespace"          : NAMESPACE_NAME_2,
                "kubernetes.client.config-maps.enabled": false,
                "kubernetes.client.secrets.enabled"    : true,
                "kubernetes.client.secrets.use-api"    : true,
                "kubernetes.client.secrets.watch"      : true
        ], Environment.KUBERNETES)

        def watcher = context.getBean(KubernetesSecretWatcher.class)
        watcher.onApplicationEvent(null)

        def api = context.getBean(CoreV1Api.class)
        def conditions = new PollingConditions(timeout: 2)

        when:
        def propertySources = KubernetesConfigurationClient.propertySourceCache

        then:
        propertySources.size() == 2
        propertySources.get(TEST_SECRET_4_PS_NAME) != null
        propertySources.get(TEST_SECRET_4_PS_NAME).size() == 2
        propertySources.get(TEST_SECRET_4_PS_NAME).get("secretKey4") == "secretValue4"
        propertySources.get(TEST_SECRET_4_PS_NAME).contains(createResVersionSecretPropName("test-secret-4"))
        propertySources.get(SECRET_LIST_PS_NAME) != null
        propertySources.get(SECRET_LIST_PS_NAME).size() == 1
        propertySources.get(SECRET_LIST_PS_NAME).contains(SECRET_LIST_PS_KEY)

        when: "new secret is created"
        def newSecretPropSourceName = createSecretPropSourceName("test-secret-5")
        V1Secret newSecret = getSecretModel("test-secret-5", ["secretKey5": "secretValue5".bytes])
        createSecret(api, NAMESPACE_NAME_2, newSecret)

        then: "new property source is created"
        conditions.eventually {
            with(KubernetesConfigurationClient.propertySourceCache) {
                it.size() == 3
                it.get(newSecretPropSourceName) != null
                it.get(newSecretPropSourceName).size() == 2
                it.get(newSecretPropSourceName).get("secretKey5") == "secretValue5"
                it.get(newSecretPropSourceName).contains(createResVersionSecretPropName("test-secret-5"))
            }
        }

        when: "existing secret is replaced"
        def updatedSecret = getSecretModel("test-secret-5", ["secretKey5": "secretValue500".bytes])
        replaceSecret(api, NAMESPACE_NAME_2, updatedSecret)

        then: "existing property source is updated"
        conditions.eventually {
            with(KubernetesConfigurationClient.propertySourceCache) {
                it.size() == 3
                it.get(newSecretPropSourceName) != null
                it.get(newSecretPropSourceName).size() == 2
                it.get(newSecretPropSourceName).get("secretKey5") == "secretValue500"
                it.get(newSecretPropSourceName).contains(createResVersionSecretPropName("test-secret-5"))
            }
        }

        when: "existing secret is deleted"
        deleteSecret(api, NAMESPACE_NAME_2, "test-secret-5")

        then: "existing property source is deleted"
        conditions.eventually {
            with(KubernetesConfigurationClient.propertySourceCache) {
                it.size() == 2
                it.get(newSecretPropSourceName) == null
            }
        }

        cleanup:
        context.close()
    }

    void "test includes filter for secrets"() {
        given:
        ApplicationContext context = ApplicationContext.run([
                "micronaut.config-client.enabled"      : true,
                "kubernetes.client.kube-config-path"   : "file:" + kubeConfigFile.toString(),
                "kubernetes.client.namespace"          : NAMESPACE_NAME_1,
                "kubernetes.client.config-maps.enabled": false,
                "kubernetes.client.secrets.enabled"    : true,
                "kubernetes.client.secrets.use-api"    : true,
                "kubernetes.client.secrets.watch"      : false,
                "kubernetes.client.secrets.includes"   : ["test-secret-2"]
        ], Environment.KUBERNETES)

        when:
        def propertySources = KubernetesConfigurationClient.propertySourceCache

        then:
        propertySources.size() == 2
        propertySources.get(TEST_SECRET_2_PS_NAME) != null
        propertySources.get(TEST_SECRET_2_PS_NAME).get("secretKey2") == "secretValue2"
        propertySources.get(SECRET_LIST_PS_NAME) != null
        propertySources.get(SECRET_LIST_PS_NAME).contains(SECRET_LIST_PS_KEY)

        cleanup:
        context.close()
    }

    void "test excludes filter for secrets"() {
        given:
        ApplicationContext context = ApplicationContext.run([
                "micronaut.config-client.enabled"      : true,
                "kubernetes.client.kube-config-path"   : "file:" + kubeConfigFile.toString(),
                "kubernetes.client.namespace"          : NAMESPACE_NAME_1,
                "kubernetes.client.config-maps.enabled": false,
                "kubernetes.client.secrets.enabled"    : true,
                "kubernetes.client.secrets.use-api"    : true,
                "kubernetes.client.secrets.watch"      : false,
                "kubernetes.client.secrets.excludes"   : ["test-secret-1", "test-secret-2"]
        ], Environment.KUBERNETES)

        when:
        def propertySources = KubernetesConfigurationClient.propertySourceCache

        then:
        propertySources.size() == 2
        propertySources.get(TEST_SECRET_3_PS_NAME) != null
        propertySources.get(TEST_SECRET_3_PS_NAME).get("secretKey3") == "secretValue3"
        propertySources.get(SECRET_LIST_PS_NAME) != null
        propertySources.get(SECRET_LIST_PS_NAME).contains(SECRET_LIST_PS_KEY)

        cleanup:
        context.close()
    }

    void "test multiple filters for secrets"() {
        given:
        ApplicationContext context = ApplicationContext.run([
                "micronaut.config-client.enabled"      : true,
                "kubernetes.client.kube-config-path"   : "file:" + kubeConfigFile.toString(),
                "kubernetes.client.namespace"          : NAMESPACE_NAME_1,
                "kubernetes.client.config-maps.enabled": false,
                "kubernetes.client.secrets.enabled"    : true,
                "kubernetes.client.secrets.use-api"    : true,
                "kubernetes.client.secrets.watch"      : false,
                "kubernetes.client.secrets.includes"   : ["test-secret-1", "test-secret-2"],
                "kubernetes.client.secrets.excludes"   : ["test-secret-1"]
        ], Environment.KUBERNETES)

        when:
        def propertySources = KubernetesConfigurationClient.propertySourceCache

        then:
        propertySources.size() == 2
        propertySources.get(TEST_SECRET_2_PS_NAME) != null
        propertySources.get(TEST_SECRET_2_PS_NAME).get("secretKey2") == "secretValue2"
        propertySources.get(SECRET_LIST_PS_NAME) != null
        propertySources.get(SECRET_LIST_PS_NAME).contains(SECRET_LIST_PS_KEY)

        cleanup:
        context.close()
    }

    void "test label filter for secrets"() {
        given:
        ApplicationContext context = ApplicationContext.run([
                "micronaut.config-client.enabled"      : true,
                "kubernetes.client.kube-config-path"   : "file:" + kubeConfigFile.toString(),
                "kubernetes.client.namespace"          : NAMESPACE_NAME_1,
                "kubernetes.client.config-maps.enabled": false,
                "kubernetes.client.secrets.enabled"    : true,
                "kubernetes.client.secrets.use-api"    : true,
                "kubernetes.client.secrets.watch"      : false,
                "kubernetes.client.secrets.labels"     : ["podLabelKey2": "podLabelValue2"]
        ], Environment.KUBERNETES)

        when:
        def propertySources = KubernetesConfigurationClient.propertySourceCache

        then:
        propertySources.size() == 2
        propertySources.get(TEST_SECRET_3_PS_NAME) != null
        propertySources.get(TEST_SECRET_3_PS_NAME).get("secretKey3") == "secretValue3"
        propertySources.get(SECRET_LIST_PS_NAME) != null
        propertySources.get(SECRET_LIST_PS_NAME).contains(SECRET_LIST_PS_KEY)

        cleanup:
        context.close()
    }

    void "test pod label key filter for secrets"() {
        given:
        ApplicationContext context = ApplicationContext.run([
                "micronaut.config-client.enabled"      : true,
                "kubernetes.client.kube-config-path"   : "file:" + kubeConfigFile.toString(),
                "kubernetes.client.namespace"          : NAMESPACE_NAME_1,
                "kubernetes.client.config-maps.enabled": false,
                "kubernetes.client.secrets.enabled"    : true,
                "kubernetes.client.secrets.use-api"    : true,
                "kubernetes.client.secrets.watch"      : false,
                "kubernetes.client.secrets.pod-labels" : ["podLabelKey1"]
        ], Environment.KUBERNETES)

        when:
        def propertySources = KubernetesConfigurationClient.propertySourceCache

        then:
        System.getenv("KUBERNETES_SERVICE_HOST") == "localhost"
        System.getenv("HOSTNAME") == POD_NAME
        propertySources.size() == 2
        propertySources.get(TEST_SECRET_2_PS_NAME) != null
        propertySources.get(TEST_SECRET_2_PS_NAME).get("secretKey2") == "secretValue2"
        propertySources.get(SECRET_LIST_PS_NAME) != null
        propertySources.get(SECRET_LIST_PS_NAME).contains(SECRET_LIST_PS_KEY)

        cleanup:
        context.close()
    }

    void "test pod label key filter failure when exceptionOnPodLabelsMissing is enabled for secrets"() {
        given:
        def properties = [
                "micronaut.config-client.enabled"                          : true,
                "kubernetes.client.kube-config-path"                       : "file:" + kubeConfigFile.toString(),
                "kubernetes.client.namespace"                              : NAMESPACE_NAME_1,
                "kubernetes.client.config-maps.enabled"                    : false,
                "kubernetes.client.secrets.enabled"                        : true,
                "kubernetes.client.secrets.use-api"                        : true,
                "kubernetes.client.secrets.watch"                          : false,
                "kubernetes.client.secrets.pod-labels"                     : ["podLabelKey1", "podLabelKey100"],
                "kubernetes.client.secrets.exception-on-pod-labels-missing": true
        ]

        when:
        ApplicationContext.run(properties, Environment.KUBERNETES)

        then:
        System.getenv("KUBERNETES_SERVICE_HOST") == "localhost"
        System.getenv("HOSTNAME") == POD_NAME
        def e = thrown(ConfigurationException)
        e.message.startsWith("Pod metadata does not contain label")
    }

    void "test reading of secrets from mounted volumes"() {
        given:
        def loader = ClassPathResourceLoader.defaultLoader(this.class.getClassLoader())
        def secretFileUrl = loader.getResource("classpath:kubernetes/secrets/foo")
                .orElseThrow(() -> new FileNotFoundException("File 'kubernetes/secrets/foo' not found on classpath"))
        Path parentPath = Path.of(secretFileUrl.toURI()).parent
        ApplicationContext context = ApplicationContext.run([
                "micronaut.config-client.enabled"      : true,
                "kubernetes.client.kube-config-path"   : "file:" + kubeConfigFile.toString(),
                "kubernetes.client.namespace"          : NAMESPACE_NAME_1,
                "kubernetes.client.config-maps.enabled": false,
                "kubernetes.client.secrets.enabled"    : true,
                "kubernetes.client.secrets.use-api"    : false,
                "kubernetes.client.secrets.watch"      : false,
                "kubernetes.client.secrets.paths"      : [parentPath.toString()]
        ], Environment.KUBERNETES)

        when:
        def propertySources = KubernetesConfigurationClient.propertySourceCache
        def filePropertySourceKeyOpt = propertySources.keySet().stream().findFirst()

        then:
        propertySources.size() == 1
        filePropertySourceKeyOpt.isPresent()
        propertySources.get(filePropertySourceKeyOpt.get()).size() == 1
        propertySources.get(filePropertySourceKeyOpt.get()).get("foo") == "bar"

        cleanup:
        context.close()
    }

    V1ConfigMap getJsonConfigMap() {
        def jsonContent = '''\
            {
              "enemies": "monsters",
              "lives": 1,
              "secret": {
                "code": {
                  "passphrase": "mon",
                  "allowed": true
                }
              }
            }\
        '''.stripIndent()
        return getConfigMapModel("game-config-json", ["game.json": jsonContent], ["podLabelKey1": "podLabelValue1"])
    }

    V1ConfigMap getPropertiesConfigMap() {
        def propertiesContent = '''\
            enemies=zombies
            lives=2
            secret.code.passphrase=zom
            secret.code.allowed=false\
        '''.stripIndent()
        return getConfigMapModel("game-config-properties", ["game.properties": propertiesContent], ["podLabelKey2": "podLabelValue2"])
    }

    V1ConfigMap getYmlConfigMap() {
        def ymlContent = '''\
            ---
            enemies: aliens
            lives: 3
            ---
            secret:
              code:
                passphrase: ali
                allowed: true\
        '''.stripIndent()
        return getConfigMapModel("game-config-yml", ["game.yml": ymlContent])
    }

    V1ConfigMap getLiteralConfigMap() {
        return getConfigMapModel("literal-config", ["special.how": "very", "special.type": "charm"])
    }

    static String createConfigMapPropSourceName(String objectName) {
        return objectName + " (Kubernetes V1ConfigMap)"
    }

    static String createSecretPropSourceName(String objectName) {
        return objectName + " (Kubernetes V1Secret)"
    }

    String createResVersionConfigMapPropName(String objectName) {
        return V1ConfigMap.class.getSimpleName().toLowerCase() + "." + objectName + ".resource-version"
    }

    String createResVersionSecretPropName(String objectName) {
        return V1Secret.class.getSimpleName().toLowerCase() + "." + objectName + ".resource-version"
    }
}
