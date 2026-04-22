package io.micronaut.kubernetes.client.openapi.configuration.imports

import io.micronaut.context.ApplicationContext
import io.micronaut.context.env.Environment
import io.micronaut.context.env.PropertySource
import io.micronaut.context.exceptions.ConfigurationException
import io.micronaut.core.io.scan.ClassPathResourceLoader
import io.micronaut.kubernetes.client.openapi.api.CoreV1Api
import io.micronaut.kubernetes.client.openapi.configuration.KubernetesConfigurationClient
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

class KubernetesConfigImportSpec extends K3sContainerSpec {

    private static final Logger LOG = LoggerFactory.getLogger(KubernetesConfigImportSpec.class)

    private static final NAMESPACE_NAME_1 = "micronaut-service-configuration-1"
    private static final NAMESPACE_NAME_2 = "micronaut-service-configuration-2"

    private static final GAME_CONFIG_JSON_NAME = "game-config-json"
    private static final GAME_CONFIG_PROPERTIES_NAME = "game-config-properties"
    private static final GAME_CONFIG_YML_NAME = "game-config-yml"
    private static final LITERAL_CONFIG_NAME = "literal-config"

    private static final TEST_SECRET_1_PS_NAME = createSecretPropSourceName("test-secret-1")
    private static final TEST_SECRET_2_PS_NAME = createSecretPropSourceName("test-secret-2")
    private static final TEST_SECRET_3_PS_NAME = createSecretPropSourceName("test-secret-3")
    private static final TEST_SECRET_4_PS_NAME = createSecretPropSourceName("test-secret-4")
    private static final SECRET_LIST_PS_NAME = "Kubernetes V1SecretList"
    private static final SECRET_LIST_PS_KEY = "v1secretlist.resource-version"

    // pod name should be equal to value that is passed as env variable to tests in build.gradle file
    private static final POD_NAME = "test-pod"

    void setup() {
        PropertySourceCache.get().clear()
        ImportDeclarationWatchIndex.reset()
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

    void "read config maps by name"() {
        given:
        ApplicationContext context = ApplicationContext.run([
                "micronaut.config-client.enabled"    : false,
                "kubernetes.client.kube-config-path" : "file:" + kubeConfigFile.toString(),
                "kubernetes.client.namespace"        : NAMESPACE_NAME_1,
                "micronaut.config.import[0]"         : "kubernetes://config-map?name=game-config-json&watch=false",
                "micronaut.config.import[1]"         : "kubernetes://config-map?name=game-config-properties&watch=false",
                "micronaut.config.import[2]"         : "kubernetes://config-map?name=game-config-yml&watch=false",
                "micronaut.config.import[3]"         : "kubernetes://config-map?name=literal-config&watch=false"
        ], Environment.KUBERNETES)

        when:
        def propertySources = PropertySourceCache.get()

        then:
        propertySources.size() == 4

        PropertySource propertySourceJson = findByName(propertySources, GAME_CONFIG_JSON_NAME)
        propertySourceJson != null
        propertySourceJson.size() == 5
        propertySourceJson.get("enemies") == "monsters"
        propertySourceJson.get("lives") == 1
        propertySourceJson.get("secret.code.passphrase") == "mon"
        propertySourceJson.get("secret.code.allowed") == true
        propertySourceJson.contains(createResVersionConfigMapPropName(GAME_CONFIG_JSON_NAME))

        PropertySource propertySourceProp = findByName(propertySources, GAME_CONFIG_PROPERTIES_NAME)
        propertySourceProp != null
        propertySourceProp.size() == 5
        propertySourceProp.get("enemies") == "zombies"
        propertySourceProp.get("lives") == "2"
        propertySourceProp.get("secret.code.passphrase") == "zom"
        propertySourceProp.get("secret.code.allowed") == "false"
        propertySourceProp.contains(createResVersionConfigMapPropName(GAME_CONFIG_PROPERTIES_NAME))

        PropertySource propertySourceYml = findByName(propertySources, GAME_CONFIG_YML_NAME)
        propertySourceYml != null
        propertySourceYml.size() == 5
        propertySourceYml.get("enemies") == "aliens"
        propertySourceYml.get("lives") == 3
        propertySourceYml.get("secret.code.passphrase") == "ali"
        propertySourceYml.get("secret.code.allowed") == true
        propertySourceYml.contains(createResVersionConfigMapPropName(GAME_CONFIG_YML_NAME))

        PropertySource propertySourceLiteral = findByName(propertySources, LITERAL_CONFIG_NAME)
        propertySourceLiteral != null
        propertySourceLiteral.size() == 3
        propertySourceLiteral.get("special.how") == "very"
        propertySourceLiteral.get("special.type") == "charm"
        propertySourceLiteral.contains(createResVersionConfigMapPropName(LITERAL_CONFIG_NAME))

        cleanup:
        context.close()
    }

    PropertySource findByName(Map<ImportDeclaration, PropertySource> propertySources, String name) {
        return (PropertySource) propertySources.entrySet().stream()
                .filter(e -> e.getKey().name() != null && e.getKey().name().equals(name))    // or startsWith("pre")
                .map(Map.Entry::getValue)
                .findFirst()
                .orElse(null)
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
        return getConfigMapModel(GAME_CONFIG_JSON_NAME, ["game.json": jsonContent], ["podLabelKey1": "podLabelValue1"])
    }

    V1ConfigMap getPropertiesConfigMap() {
        def propertiesContent = '''\
            enemies=zombies
            lives=2
            secret.code.passphrase=zom
            secret.code.allowed=false\
        '''.stripIndent()
        return getConfigMapModel(GAME_CONFIG_PROPERTIES_NAME, ["game.properties": propertiesContent], ["podLabelKey2": "podLabelValue2"])
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
        return getConfigMapModel(GAME_CONFIG_YML_NAME, ["game.yml": ymlContent])
    }

    V1ConfigMap getLiteralConfigMap() {
        return getConfigMapModel(LITERAL_CONFIG_NAME, ["special.how": "very", "special.type": "charm"])
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
