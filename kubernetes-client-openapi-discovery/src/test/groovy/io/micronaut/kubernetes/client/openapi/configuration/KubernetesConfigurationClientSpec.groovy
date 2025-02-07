package io.micronaut.kubernetes.client.openapi.configuration

import io.micronaut.context.ApplicationContext
import io.micronaut.context.env.Environment
import io.micronaut.context.exceptions.ConfigurationException
import io.micronaut.core.io.scan.ClassPathResourceLoader
import io.micronaut.kubernetes.client.openapi.K3sContainerSpec
import io.micronaut.kubernetes.client.openapi.model.V1ConfigMap
import io.micronaut.kubernetes.client.openapi.model.V1Pod
import io.micronaut.kubernetes.client.openapi.model.V1PodSpec
import io.micronaut.kubernetes.client.openapi.reactor.api.CoreV1ApiReactor
import io.micronaut.kubernetes.client.openapi.utils.ModelUtils
import io.micronaut.kubernetes.client.openapi.utils.OperationUtils
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.nio.file.Path

class KubernetesConfigurationClientSpec extends K3sContainerSpec {

    private static final Logger LOG = LoggerFactory.getLogger(KubernetesConfigurationClientSpec.class)

    private static final NAMESPACE_NAME = "micronaut-service-configuration"

    private static final GAME_CONFIG_JSON_PS_NAME = createConfigMapPropSourceName("game-config-json")
    private static final GAME_CONFIG_PROPERTIES_PS_NAME = createConfigMapPropSourceName("game-config-properties")
    private static final GAME_CONFIG_YML_PS_NAME = createConfigMapPropSourceName("game-config-yml")
    private static final LITERAL_CONFIG_PS_NAME = createConfigMapPropSourceName("literal-config")
    private static final CONFIG_MAP_LIST_PS_NAME = "Kubernetes V1ConfigMapList"
    private static final CONFIG_MAP_LIST_PS_KEY = "v1configmaplist.resource-version"

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
    def setupKubernetes(CoreV1ApiReactor api) {
        OperationUtils.createNamespace(api, ModelUtils.getNamespace(NAMESPACE_NAME))

        OperationUtils.createConfigMap(api, NAMESPACE_NAME, getJsonConfigMap())
        OperationUtils.createConfigMap(api, NAMESPACE_NAME, getPropertiesConfigMap())
        OperationUtils.createConfigMap(api, NAMESPACE_NAME, getYmlConfigMap())
        OperationUtils.createConfigMap(api, NAMESPACE_NAME, getLiteralConfigMap())

        V1PodSpec podSpec = ModelUtils.getPodSpec([ModelUtils.getContainer("test-cont-1")])
        V1Pod pod = ModelUtils.getPod(POD_NAME, podSpec, ["podLabelKey1": "podLabelValue1", "podLabelKey2": "podLabelValue2"])
        OperationUtils.createPod(api, NAMESPACE_NAME, pod)
    }

    void "read json, properties, yml and literal config maps"() {
        given:
        ApplicationContext context = ApplicationContext.run([
                "micronaut.config-client.enabled"      : true,
                "kubernetes.client.kube-config-path"   : "file:" + kubeConfigFile.toString(),
                "kubernetes.client.namespace"          : NAMESPACE_NAME,
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

    void "test includes filter for config maps"() {
        given:
        ApplicationContext context = ApplicationContext.run([
                "micronaut.config-client.enabled"       : true,
                "kubernetes.client.kube-config-path"    : "file:" + kubeConfigFile.toString(),
                "kubernetes.client.namespace"           : NAMESPACE_NAME,
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
                "kubernetes.client.namespace"           : NAMESPACE_NAME,
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
                "kubernetes.client.namespace"           : NAMESPACE_NAME,
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

    void "test pod label key filter for config maps"() {
        given:
        ApplicationContext context = ApplicationContext.run([
                "micronaut.config-client.enabled"         : true,
                "kubernetes.client.kube-config-path"      : "file:" + kubeConfigFile.toString(),
                "kubernetes.client.namespace"             : NAMESPACE_NAME,
                "kubernetes.client.config-maps.enabled"   : true,
                "kubernetes.client.config-maps.use-api"   : true,
                "kubernetes.client.config-maps.watch"     : false,
                "kubernetes.client.config-maps.pod-labels": ["podLabelKey1"]
        ], Environment.KUBERNETES)

        when:
        def propertySources = KubernetesConfigurationClient.propertySourceCache

        then:
        System.getenv("KUBERNETES_SERVICE_HOST") == "localhost" // set env variable manually if test is not run by gradle
        System.getenv("HOSTNAME") == POD_NAME // set env variable manually if test is not run by gradle
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
                "kubernetes.client.namespace"                                  : NAMESPACE_NAME,
                "kubernetes.client.config-maps.enabled"                        : true,
                "kubernetes.client.config-maps.use-api"                        : true,
                "kubernetes.client.config-maps.watch"                          : false,
                "kubernetes.client.config-maps.pod-labels"                     : ["podLabelKey1", "podLabelKey100"],
                "kubernetes.client.config-maps.exception-on-pod-labels-missing": true
        ]

        when:
        ApplicationContext.run(properties, Environment.KUBERNETES)

        then:
        System.getenv("KUBERNETES_SERVICE_HOST") == "localhost" // set env variable manually if test is not run by gradle
        System.getenv("HOSTNAME") == POD_NAME // set env variable manually if test is not run by gradle
        def e = thrown(ConfigurationException)
        e.message.startsWith("Pod metadata does not contain label")
    }

    void "test reading of config maps from mounted volumes"() {
        given:
        def loader = ClassPathResourceLoader.defaultLoader(this.class.getClassLoader())
        def mountedJsonUrl = loader.getResource("classpath:kubernetes/mounted.json")
                .orElseThrow(() -> new FileNotFoundException("File 'kubernetes/mounted.json' not found on classpath"))
        Path parentPath = Path.of(mountedJsonUrl.toURI()).parent
        ApplicationContext context = ApplicationContext.run([
                "micronaut.config-client.enabled"      : true,
                "kubernetes.client.kube-config-path"   : "file:" + kubeConfigFile.toString(),
                "kubernetes.client.namespace"          : NAMESPACE_NAME,
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
        return ModelUtils.getConfigMap("game-config-json", ["game.json": jsonContent], ["podLabelKey1": "podLabelValue1"])
    }

    V1ConfigMap getPropertiesConfigMap() {
        def propertiesContent = '''\
            enemies=zombies
            lives=2
            secret.code.passphrase=zom
            secret.code.allowed=false\
        '''.stripIndent()
        return ModelUtils.getConfigMap("game-config-properties", ["game.properties": propertiesContent], ["podLabelKey2": "podLabelValue2"])
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
        return ModelUtils.getConfigMap("game-config-yml", ["game.yml": ymlContent])
    }

    V1ConfigMap getLiteralConfigMap() {
        return ModelUtils.getConfigMap("literal-config", ["special.how": "very", "special.type": "charm"])
    }

    static String createConfigMapPropSourceName(String objectName) {
        return objectName + " (Kubernetes V1ConfigMap)"
    }

    String createResVersionConfigMapPropName(String objectName) {
        return V1ConfigMap.class.getSimpleName().toLowerCase() + "." + objectName + ".resource-version"
    }
}
