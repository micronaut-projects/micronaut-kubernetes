package io.micronaut.kubernetes.client.openapi.configuration

import io.micronaut.context.ApplicationContext
import io.micronaut.context.env.Environment
import io.micronaut.kubernetes.client.openapi.K3sContainerSpec
import io.micronaut.kubernetes.client.openapi.model.V1ConfigMap
import io.micronaut.kubernetes.client.openapi.reactor.api.CoreV1ApiReactor
import io.micronaut.kubernetes.client.openapi.utils.ModelUtils
import io.micronaut.kubernetes.client.openapi.utils.OperationUtils
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class KubernetesConfigurationClientSpec extends K3sContainerSpec {

    private static final Logger LOG = LoggerFactory.getLogger(KubernetesConfigurationClientSpec.class)

    private static final NAMESPACE_NAME = "micronaut-service-configuration"

    private static final GAME_CONFIG_JSON_PS_NAME = createConfigMapPropSourceName("game-config-json")
    private static final GAME_CONFIG_PROPERTIES_PS_NAME = createConfigMapPropSourceName("game-config-properties")
    private static final GAME_CONFIG_YML_PS_NAME = createConfigMapPropSourceName("game-config-yml")
    private static final MOUNTED_CONFIGMAP_PS_NAME = createConfigMapPropSourceName("mounted-configmap")
    private static final LITERAL_CONFIG_PS_NAME = createConfigMapPropSourceName("literal-config")

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
        OperationUtils.createConfigMap(api, NAMESPACE_NAME, getMountedYmlConfigMap())
        OperationUtils.createConfigMap(api, NAMESPACE_NAME, getLiteralConfigMap())
    }

    void "read json, properties, yml and literal config maps "() {
        given:
        ApplicationContext context = ApplicationContext.run([
                "micronaut.config-client.enabled"      : true,
                "kubernetes.client.kube-config-path"   : "file:" + kubeConfigFile.toString(),
                "kubernetes.client.namespace"          : NAMESPACE_NAME,
                "kubernetes.client.config-maps.enabled": true,
                "kubernetes.client.config-maps.use-api": true
        ], Environment.KUBERNETES)

        when:
        def propertySources = KubernetesConfigurationClient.propertySourceCache

        then:
        propertySources.size() == 6

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

        propertySources.get(MOUNTED_CONFIGMAP_PS_NAME) != null
        propertySources.get(MOUNTED_CONFIGMAP_PS_NAME).size() == 2
        propertySources.get(MOUNTED_CONFIGMAP_PS_NAME).get("mounted.foo") == "bar"
        propertySources.get(MOUNTED_CONFIGMAP_PS_NAME).contains(createResVersionConfigMapPropName("mounted-configmap"))

        propertySources.get(LITERAL_CONFIG_PS_NAME) != null
        propertySources.get(LITERAL_CONFIG_PS_NAME).size() == 3
        propertySources.get(LITERAL_CONFIG_PS_NAME).get("special.how") == "very"
        propertySources.get(LITERAL_CONFIG_PS_NAME).get("special.type") == "charm"
        propertySources.get(LITERAL_CONFIG_PS_NAME).contains(createResVersionConfigMapPropName("literal-config"))

        propertySources.get("Kubernetes V1ConfigMapList") != null
        propertySources.get("Kubernetes V1ConfigMapList").size() == 1
        propertySources.get("Kubernetes V1ConfigMapList").contains("v1configmaplist.resource-version")

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
        return ModelUtils.getConfigMap("game-config-json", ["game.json": jsonContent])
    }

    V1ConfigMap getPropertiesConfigMap() {
        def propertiesContent = '''\
            enemies=zombies
            lives=2
            secret.code.passphrase=zom
            secret.code.allowed=false\
        '''.stripIndent()
        return ModelUtils.getConfigMap("game-config-properties", ["game.properties": propertiesContent])
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

    V1ConfigMap getMountedYmlConfigMap() {
        def ymlContent = '''\
            mounted:
              foo: bar\
        '''.stripIndent()
        return ModelUtils.getConfigMap("mounted-configmap", ["mounted.yml": ymlContent])
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
