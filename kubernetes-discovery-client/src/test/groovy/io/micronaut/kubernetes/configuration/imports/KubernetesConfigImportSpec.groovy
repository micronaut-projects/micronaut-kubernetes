package io.micronaut.kubernetes.configuration.imports

import io.kubernetes.client.openapi.Configuration
import io.kubernetes.client.openapi.apis.CoreV1Api
import io.kubernetes.client.openapi.models.V1ConfigMap
import io.kubernetes.client.openapi.models.V1Pod
import io.kubernetes.client.openapi.models.V1PodSpec
import io.kubernetes.client.openapi.models.V1Secret
import io.micronaut.context.ApplicationContext
import io.micronaut.context.env.Environment
import io.micronaut.context.exceptions.ConfigurationException
import io.micronaut.kubernetes.test.K3sContainerSpec
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import spock.util.concurrent.PollingConditions

import static io.micronaut.kubernetes.test.KubernetesModels.getConfigMapModel
import static io.micronaut.kubernetes.test.KubernetesModels.getContainerModel
import static io.micronaut.kubernetes.test.KubernetesModels.getPodModel
import static io.micronaut.kubernetes.test.KubernetesModels.getPodSpecModel
import static io.micronaut.kubernetes.test.KubernetesModels.getSecretModel
import static io.micronaut.kubernetes.test.KubernetesOperations.createConfigMap
import static io.micronaut.kubernetes.test.KubernetesOperations.createNamespace
import static io.micronaut.kubernetes.test.KubernetesOperations.createPod
import static io.micronaut.kubernetes.test.KubernetesOperations.createSecret
import static io.micronaut.kubernetes.test.KubernetesOperations.deleteConfigMap
import static io.micronaut.kubernetes.test.KubernetesOperations.deleteSecret
import static io.micronaut.kubernetes.test.KubernetesOperations.replaceConfigMap
import static io.micronaut.kubernetes.test.KubernetesOperations.replaceSecret

class KubernetesConfigImportSpec extends K3sContainerSpec {

    private static final Logger LOG = LoggerFactory.getLogger(KubernetesConfigImportSpec.class)

    private static final NAMESPACE_NAME_1 = "micronaut-service-configuration-1"
    private static final NAMESPACE_NAME_2 = "micronaut-service-configuration-2"

    private static final JSON_CONFIG_MAP_NAME = "order-config-json"
    private static final PROP_CONFIG_MAP_NAME = "order-config-properties"
    private static final YAML_CONFIG_MAP_NAME = "order-config-yml"
    private static final LITERAL_CONFIG_MAP_NAME = "order-config-literal"

    private static final SECRET_NAME_1 = "test-secret-1"
    private static final SECRET_NAME_2 = "test-secret-2"
    private static final SECRET_NAME_3 = "test-secret-3"
    private static final SECRET_NAME_4 = "test-secret-4"

    // pod name should be equal to value that is passed as env variable to tests in build.gradle file
    private static final POD_NAME = "test-pod"

    void cleanup() {
        PropertySourceCache.get().clear()
        ImportDeclarationWatchIndex.reset()
        KubernetesLegacyImportMode.reset()
    }

    def cleanupSpec() {
        // remove cached client
        Configuration.setDefaultApiClient(null)
    }

    @Override
    Logger getLogger() {
        return LOG
    }

    @Override
    def setupKubernetes(ApplicationContext context) {
        // remove cached client
        Configuration.setDefaultApiClient(null)
        // set new client
        context.getBean(CoreV1Api.class)

        createNamespace(NAMESPACE_NAME_1)

        createConfigMap(NAMESPACE_NAME_1, getJsonConfigMap())
        createConfigMap(NAMESPACE_NAME_1, getPropertiesConfigMap())
        createConfigMap(NAMESPACE_NAME_1, getYmlConfigMap())
        createConfigMap(NAMESPACE_NAME_1, getLiteralConfigMap())

        V1Secret secret1 = getSecretModel(SECRET_NAME_1, ["secretKey11": "secretValue11".bytes, "secretKey12": "secretValue12".bytes], ["podLabelKey1": "podLabelValue1"])
        createSecret(NAMESPACE_NAME_1, secret1)
        V1Secret secret2 = getSecretModel(SECRET_NAME_2, ["secretKey2": "secretValue2".bytes], ["podLabelKey2": "podLabelValue2"])
        createSecret(NAMESPACE_NAME_1, secret2)
        V1Secret secret3 = getSecretModel(SECRET_NAME_3, ["secretKey3": "secretValue3".bytes], ["podLabelKey1": "podLabelValue1"])
        createSecret(NAMESPACE_NAME_1, secret3)

        V1PodSpec podSpec1 = getPodSpecModel([getContainerModel("test-cont-1")])
        V1Pod pod1 = getPodModel(POD_NAME, podSpec1, ["podLabelKey1": "podLabelValue1", "podLabelKey2": "podLabelValue2"])
        createPod(NAMESPACE_NAME_1, pod1)

        createNamespace(NAMESPACE_NAME_2)

        createConfigMap(NAMESPACE_NAME_2, getLiteralConfigMap())

        V1Secret secret4 = getSecretModel(SECRET_NAME_4, ["secretKey4": "secretValue4".bytes])
        createSecret(NAMESPACE_NAME_2, secret4)

        V1PodSpec podSpec2 = getPodSpecModel([getContainerModel("test-cont-2")])
        V1Pod pod2 = getPodModel(POD_NAME, podSpec2, ["podLabelKey20": "podLabelValue20"])
        createPod(NAMESPACE_NAME_2, pod2)
    }

    void "read config maps by name"() {
        given:
        ApplicationContext context = ApplicationContext.run([
                "micronaut.config-client.enabled"    : false,
                "kubernetes.client.kube-config-path" : kubeConfigFile.toString(),
                "kubernetes.client.namespace"        : NAMESPACE_NAME_1,
                "micronaut.config.import[0]"         : "kubernetes://config-map?name=order-config-json&watch=false",
                "micronaut.config.import[1]"         : "kubernetes://config-map?name=order-config-properties&watch=false",
                "micronaut.config.import[2]"         : "kubernetes://config-map?name=order-config-yml&watch=false",
                "micronaut.config.import[3]"         : "kubernetes://config-map?name=order-config-literal&watch=false"
        ], Environment.KUBERNETES)

        expect:
        getStringProperty(context, "json-order-id") == "order-id-1"
        getStringProperty(context, "json-customer.customer-id") == "customer-id-1"
        getStringProperty(context, "json-customer.customer-name") == "customer-name-1"
        getStringProperty(context, "json-items[0].sku") == "sku-1"
        getStringProperty(context, "json-items[0].name") == "sku-name-1"
        getIntProperty(context, "json-items[0].quantity",) == 11

        getStringProperty(context, "prop-order-id") == "order-id-2"
        getStringProperty(context, "prop-customer.customer-id") == "customer-id-2"
        getStringProperty(context, "prop-customer.customer-name") == "customer-name-2"
        getStringProperty(context, "prop-items[0].sku") == "sku-2"
        getStringProperty(context, "prop-items[0].name") == "sku-name-2"
        getIntProperty(context, "prop-items[0].quantity") == 22

        getStringProperty(context, "yml-order-id") == "order-id-3"
        getStringProperty(context, "yml-customer.customer-id") == "customer-id-3"
        getStringProperty(context, "yml-customer.customer-name") == "customer-name-3"
        getStringProperty(context, "yml-items[0].sku") == "sku-3"
        getStringProperty(context, "yml-items[0].name") == "sku-name-3"
        getIntProperty(context, "yml-items[0].quantity") == 33

        getStringProperty(context, "literal.order-id") == "order-id-4"
        getStringProperty(context, "literal.customer-id") == "customer-id-4"

        cleanup:
        context?.close()
    }

    void "read secrets by name"() {
        given:
        ApplicationContext context = ApplicationContext.run([
                "micronaut.config-client.enabled"    : false,
                "kubernetes.client.kube-config-path" : kubeConfigFile.toString(),
                "kubernetes.client.namespace"        : NAMESPACE_NAME_1,
                "micronaut.config.import[0]"         : "kubernetes://secret?name=test-secret-1&watch=false",
                "micronaut.config.import[1]"         : "kubernetes://secret?name=test-secret-2&watch=false",
                "micronaut.config.import[2]"         : "kubernetes://secret?name=test-secret-3&watch=false"
        ], Environment.KUBERNETES)

        expect:
        getStringProperty(context, "secretKey11") == "secretValue11"
        getStringProperty(context, "secretKey12") == "secretValue12"
        getStringProperty(context, "secretKey2") == "secretValue2"
        getStringProperty(context, "secretKey3") == "secretValue3"

        cleanup:
        context?.close()
    }

    void "read config maps by labels"() {
        given:
        ApplicationContext context = ApplicationContext.run([
                "micronaut.config-client.enabled"    : false,
                "kubernetes.client.kube-config-path" : kubeConfigFile.toString(),
                "kubernetes.client.namespace"        : NAMESPACE_NAME_1,
                "micronaut.config.import[0]"         : "kubernetes://config-map?labels=podLabelKey1=podLabelValue1&watch=false"
        ], Environment.KUBERNETES)

        expect:
        getStringProperty(context, "json-order-id") == "order-id-1"
        getStringProperty(context, "json-customer.customer-id") == "customer-id-1"
        getStringProperty(context, "json-customer.customer-name") == "customer-name-1"
        getStringProperty(context, "json-items[0].sku") == "sku-1"
        getStringProperty(context, "json-items[0].name") == "sku-name-1"
        getIntProperty(context, "json-items[0].quantity",) == 11

        getStringProperty(context, "prop-order-id") == null
        getStringProperty(context, "prop-customer.customer-id") == null
        getStringProperty(context, "prop-customer.customer-name") == null
        getStringProperty(context, "prop-items[0].sku") == null
        getStringProperty(context, "prop-items[0].name") == null
        getIntProperty(context, "prop-items[0].quantity") == null

        getStringProperty(context, "yml-order-id") == "order-id-3"
        getStringProperty(context, "yml-customer.customer-id") == "customer-id-3"
        getStringProperty(context, "yml-customer.customer-name") == "customer-name-3"
        getStringProperty(context, "yml-items[0].sku") == "sku-3"
        getStringProperty(context, "yml-items[0].name") == "sku-name-3"
        getIntProperty(context, "yml-items[0].quantity") == 33

        getStringProperty(context, "literal.order-id") == null
        getStringProperty(context, "literal.customer-id") == null

        cleanup:
        context?.close()
    }

    void "read secrets by labels"() {
        given:
        ApplicationContext context = ApplicationContext.run([
                "micronaut.config-client.enabled"    : false,
                "kubernetes.client.kube-config-path" : kubeConfigFile.toString(),
                "kubernetes.client.namespace"        : NAMESPACE_NAME_1,
                "micronaut.config.import[0]"         : "kubernetes://secret?labels=podLabelKey1=podLabelValue1&watch=false"
        ], Environment.KUBERNETES)

        expect:
        getStringProperty(context, "secretKey11") == "secretValue11"
        getStringProperty(context, "secretKey12") == "secretValue12"
        getStringProperty(context, "secretKey2") == null
        getStringProperty(context, "secretKey3") == "secretValue3"

        cleanup:
        context?.close()
    }

    void "read config maps by pod labels"() {
        given:
        ApplicationContext context = ApplicationContext.run([
                "micronaut.config-client.enabled"    : false,
                "kubernetes.client.kube-config-path" : kubeConfigFile.toString(),
                "kubernetes.client.namespace"        : NAMESPACE_NAME_1,
                "micronaut.config.import[0]"         : "kubernetes://config-map?podLabels=podLabelKey1&watch=false"
        ], Environment.KUBERNETES)

        expect:
        getStringProperty(context, "json-order-id") == "order-id-1"
        getStringProperty(context, "json-customer.customer-id") == "customer-id-1"
        getStringProperty(context, "json-customer.customer-name") == "customer-name-1"
        getStringProperty(context, "json-items[0].sku") == "sku-1"
        getStringProperty(context, "json-items[0].name") == "sku-name-1"
        getIntProperty(context, "json-items[0].quantity",) == 11

        getStringProperty(context, "prop-order-id") == null
        getStringProperty(context, "prop-customer.customer-id") == null
        getStringProperty(context, "prop-customer.customer-name") == null
        getStringProperty(context, "prop-items[0].sku") == null
        getStringProperty(context, "prop-items[0].name") == null
        getIntProperty(context, "prop-items[0].quantity") == null

        getStringProperty(context, "yml-order-id") == "order-id-3"
        getStringProperty(context, "yml-customer.customer-id") == "customer-id-3"
        getStringProperty(context, "yml-customer.customer-name") == "customer-name-3"
        getStringProperty(context, "yml-items[0].sku") == "sku-3"
        getStringProperty(context, "yml-items[0].name") == "sku-name-3"
        getIntProperty(context, "yml-items[0].quantity") == 33

        getStringProperty(context, "literal.order-id") == null
        getStringProperty(context, "literal.customer-id") == null

        cleanup:
        context?.close()
    }

    void "read secrets by pod labels"() {
        given:
        ApplicationContext context = ApplicationContext.run([
                "micronaut.config-client.enabled"    : false,
                "kubernetes.client.kube-config-path" : kubeConfigFile.toString(),
                "kubernetes.client.namespace"        : NAMESPACE_NAME_1,
                "micronaut.config.import[0]"         : "kubernetes://secret?podLabels=podLabelKey1&watch=false"
        ], Environment.KUBERNETES)

        expect:
        getStringProperty(context, "secretKey11") == "secretValue11"
        getStringProperty(context, "secretKey12") == "secretValue12"
        getStringProperty(context, "secretKey2") == null
        getStringProperty(context, "secretKey3") == "secretValue3"

        cleanup:
        context?.close()
    }

    void "fail to read config maps when pod label is missing"() {
        given:
        def properties = [
                "micronaut.config-client.enabled"    : false,
                "kubernetes.client.kube-config-path" : kubeConfigFile.toString(),
                "kubernetes.client.namespace"        : NAMESPACE_NAME_1,
                "micronaut.config.import[0]"         : "kubernetes://config-map?podLabels=aaa&watch=false&exceptionOnPodLabelsMissing=true"
        ]

        when:
        ApplicationContext.run(properties, Environment.KUBERNETES)

        then:
        System.getenv("KUBERNETES_SERVICE_HOST") == "localhost"
        System.getenv("HOSTNAME") == POD_NAME
        def e = thrown(ConfigurationException)
        e.message.startsWith("Pod metadata does not contain label")
    }

    void "fail to read secrets when pod label is missing"() {
        given:
        def properties = [
                "micronaut.config-client.enabled"    : false,
                "kubernetes.client.kube-config-path" : kubeConfigFile.toString(),
                "kubernetes.client.namespace"        : NAMESPACE_NAME_1,
                "micronaut.config.import[0]"         : "kubernetes://secret?podLabels=aaa&watch=false&exceptionOnPodLabelsMissing=true"
        ]

        when:
        ApplicationContext.run(properties, Environment.KUBERNETES)

        then:
        System.getenv("KUBERNETES_SERVICE_HOST") == "localhost"
        System.getenv("HOSTNAME") == POD_NAME
        def e = thrown(ConfigurationException)
        e.message.startsWith("Pod metadata does not contain label")
    }

    void "read config maps by name when watcher enabled"() {
        given:
        ApplicationContext context = ApplicationContext.run([
                "micronaut.config-client.enabled"    : false,
                "kubernetes.client.kube-config-path" : kubeConfigFile.toString(),
                "kubernetes.client.namespace"        : NAMESPACE_NAME_1,
                "micronaut.config.import[0]"         : "optional:kubernetes://config-map?name=configmap-temp-1&watch=true"
        ], Environment.KUBERNETES)
        def conditions = new PollingConditions(timeout: 2)

        expect:
        getStringProperty(context, "test-key-1") == null
        getStringProperty(context, "test-key-2") == null

        when:
        V1ConfigMap configMap = getConfigMapModel("configmap-temp-1", ["test-key-1": "test-value-1", "test-key-2": "test-value-2"])
        createConfigMap(NAMESPACE_NAME_1, configMap)

        then:
        conditions.eventually {
            getStringProperty(context, "test-key-1") == "test-value-1"
            getStringProperty(context, "test-key-2") == "test-value-2"
        }

        when:
        deleteConfigMap("configmap-temp-1", NAMESPACE_NAME_1)

        then:
        conditions.eventually {
            getStringProperty(context, "test-key-1") == null
            getStringProperty(context, "test-key-2") == null
        }

        cleanup:
        context?.close()
    }

    void "read secrets by name when watcher enabled"() {
        given:
        ApplicationContext context = ApplicationContext.run([
                "micronaut.config-client.enabled"    : false,
                "kubernetes.client.kube-config-path" : kubeConfigFile.toString(),
                "kubernetes.client.namespace"        : NAMESPACE_NAME_1,
                "micronaut.config.import[0]"         : "optional:kubernetes://secret?name=secret-temp-1&watch=true"
        ], Environment.KUBERNETES)
        def conditions = new PollingConditions(timeout: 2)

        expect:
        getStringProperty(context, "test-secret-key-1") == null
        getStringProperty(context, "test-secret-key-2") == null

        when:
        V1Secret secret = getSecretModel("secret-temp-1", ["test-secret-key-1": "test-secret-value-1".bytes, "test-secret-key-2": "test-secret-value-2".bytes])
        createSecret(NAMESPACE_NAME_1, secret)

        then:
        conditions.eventually {
            getStringProperty(context, "test-secret-key-1") == "test-secret-value-1"
            getStringProperty(context, "test-secret-key-2") == "test-secret-value-2"
        }

        when:
        deleteSecret("secret-temp-1", NAMESPACE_NAME_1)

        then:
        conditions.eventually {
            getStringProperty(context, "test-secret-key-1") == null
            getStringProperty(context, "test-secret-key-2") == null
        }

        cleanup:
        context?.close()
    }

    void "read config maps by labels when watcher enabled"() {
        given:
        ApplicationContext context = ApplicationContext.run([
                "micronaut.config-client.enabled"    : false,
                "kubernetes.client.kube-config-path" : kubeConfigFile.toString(),
                "kubernetes.client.namespace"        : NAMESPACE_NAME_1,
                "micronaut.config.import[0]"         : "optional:kubernetes://config-map?labels=test-label-key=test-label-value&watch=true"
        ], Environment.KUBERNETES)
        def conditions = new PollingConditions(timeout: 2)

        expect:
        getStringProperty(context, "test-key-1") == null
        getStringProperty(context, "test-key-2") == null
        getStringProperty(context, "test-key-3") == null

        when:
        V1ConfigMap configMap1 = getConfigMapModel("configmap-temp-1", ["test-key-1": "test-value-1"], ["test-label-key": "test-label-value"])
        createConfigMap(NAMESPACE_NAME_1, configMap1)
        V1ConfigMap configMap2 = getConfigMapModel("configmap-temp-2", ["test-key-2": "test-value-2"])
        createConfigMap(NAMESPACE_NAME_1, configMap2)
        V1ConfigMap configMap3 = getConfigMapModel("configmap-temp-3", ["test-key-3": "test-value-3"], ["test-label-key": "test-label-value"])
        createConfigMap(NAMESPACE_NAME_1, configMap3)

        then:
        conditions.eventually {
            getStringProperty(context, "test-key-1") == "test-value-1"
            getStringProperty(context, "test-key-2") == null
            getStringProperty(context, "test-key-3") == "test-value-3"
        }

        when:
        configMap1 = getConfigMapModel("configmap-temp-1", ["test-key-1": "test-value-1"])
        replaceConfigMap(NAMESPACE_NAME_1, configMap1)

        then:
        conditions.eventually {
            getStringProperty(context, "test-key-1") == null
            getStringProperty(context, "test-key-2") == null
            getStringProperty(context, "test-key-3") == "test-value-3"
        }

        when:
        deleteConfigMap("configmap-temp-3", NAMESPACE_NAME_1)

        then:
        conditions.eventually {
            getStringProperty(context, "test-key-1") == null
            getStringProperty(context, "test-key-2") == null
            getStringProperty(context, "test-key-3") == null
        }

        when:
        configMap2 = getConfigMapModel("configmap-temp-2", ["test-key-2": "test-value-2"], ["test-label-key": "test-label-value"])
        replaceConfigMap(NAMESPACE_NAME_1, configMap2)

        then:
        conditions.eventually {
            getStringProperty(context, "test-key-1") == null
            getStringProperty(context, "test-key-2") == "test-value-2"
            getStringProperty(context, "test-key-3") == null
        }

        cleanup:
        context?.close()
    }

    void "read secrets by labels when watcher enabled"() {
        given:
        ApplicationContext context = ApplicationContext.run([
                "micronaut.config-client.enabled"    : false,
                "kubernetes.client.kube-config-path" : kubeConfigFile.toString(),
                "kubernetes.client.namespace"        : NAMESPACE_NAME_1,
                "micronaut.config.import[0]"         : "optional:kubernetes://secret?labels=test-label-key=test-label-value&watch=true"
        ], Environment.KUBERNETES)
        def conditions = new PollingConditions(timeout: 2)

        expect:
        getStringProperty(context, "test-secret-key-1") == null
        getStringProperty(context, "test-secret-key-2") == null
        getStringProperty(context, "test-secret-key-3") == null

        when:
        V1Secret secret1 = getSecretModel("secret-temp-1", ["test-secret-key-1": "test-secret-value-1".bytes], ["test-label-key": "test-label-value"])
        createSecret(NAMESPACE_NAME_1, secret1)
        V1Secret secret2 = getSecretModel("secret-temp-2", ["test-secret-key-2": "test-secret-value-2".bytes])
        createSecret(NAMESPACE_NAME_1, secret2)
        V1Secret secret3 = getSecretModel("secret-temp-3", ["test-secret-key-3": "test-secret-value-3".bytes], ["test-label-key": "test-label-value"])
        createSecret(NAMESPACE_NAME_1, secret3)

        then:
        conditions.eventually {
            getStringProperty(context, "test-secret-key-1") == "test-secret-value-1"
            getStringProperty(context, "test-secret-key-2") == null
            getStringProperty(context, "test-secret-key-3") == "test-secret-value-3"
        }

        when:
        secret1 = getSecretModel("secret-temp-1", ["test-secret-key-1": "test-secret-value-1".bytes])
        replaceSecret(NAMESPACE_NAME_1, secret1)

        then:
        conditions.eventually {
            getStringProperty(context, "test-secret-key-1") == null
            getStringProperty(context, "test-secret-key-2") == null
            getStringProperty(context, "test-secret-key-3") == "test-secret-value-3"
        }

        when:
        deleteSecret("secret-temp-3", NAMESPACE_NAME_1)

        then:
        conditions.eventually {
            getStringProperty(context, "test-secret-key-1") == null
            getStringProperty(context, "test-secret-key-2") == null
            getStringProperty(context, "test-secret-key-3") == null
        }

        when:
        secret2 = getSecretModel("secret-temp-2", ["test-secret-key-2": "test-secret-value-2".bytes], ["test-label-key": "test-label-value"])
        replaceSecret(NAMESPACE_NAME_1, secret2)

        then:
        conditions.eventually {
            getStringProperty(context, "test-secret-key-1") == null
            getStringProperty(context, "test-secret-key-2") == "test-secret-value-2"
            getStringProperty(context, "test-secret-key-3") == null
        }

        cleanup:
        context?.close()
    }

    String getStringProperty(ApplicationContext context, String name) {
        return context.get(name, String.class).orElse(null)
    }

    Integer getIntProperty(ApplicationContext context, String name) {
        return context.get(name, Integer.class).orElse(null)
    }

    V1ConfigMap getJsonConfigMap() {
        def jsonContent = '''\
            {
              "jsonOrderId": "order-id-1",
              "jsonCustomer": {
                "customerId": "customer-id-1",
                "customerName": "customer-name-1"
              },
              "jsonItems": [
                {
                  "sku": "sku-1",
                  "name": "sku-name-1",
                  "quantity": 11
                }
              ]
            }\
        '''.stripIndent()
        return getConfigMapModel(JSON_CONFIG_MAP_NAME, ["order.json": jsonContent], ["podLabelKey1": "podLabelValue1"])
    }

    V1ConfigMap getPropertiesConfigMap() {
        def propertiesContent = '''\
            propOrderId=order-id-2
            propCustomer.customerId=customer-id-2
            propCustomer.customerName=customer-name-2
            propItems[0].sku=sku-2
            propItems[0].name=sku-name-2
            propItems[0].quantity=22\
        '''.stripIndent()
        return getConfigMapModel(PROP_CONFIG_MAP_NAME, ["order.properties": propertiesContent], ["podLabelKey2": "podLabelValue2"])
    }

    V1ConfigMap getYmlConfigMap() {
        def ymlContent = '''\
            ymlOrderId: "order-id-3"
            ymlCustomer:
              customerId: customer-id-3
              customerName: customer-name-3
            ymlItems:
              - sku: sku-3
                name: sku-name-3
                quantity: 33\
        '''.stripIndent()
        return getConfigMapModel(YAML_CONFIG_MAP_NAME, ["order.yml": ymlContent], ["podLabelKey1": "podLabelValue1"])
    }

    V1ConfigMap getLiteralConfigMap() {
        return getConfigMapModel(LITERAL_CONFIG_MAP_NAME, ["literal.orderId": "order-id-4", "literal.customerId": "customer-id-4"])
    }
}
