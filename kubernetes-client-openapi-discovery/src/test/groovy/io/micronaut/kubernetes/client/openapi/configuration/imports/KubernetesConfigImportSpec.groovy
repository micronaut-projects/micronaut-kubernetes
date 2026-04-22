package io.micronaut.kubernetes.client.openapi.configuration.imports

import io.micronaut.context.ApplicationContext
import io.micronaut.context.env.Environment
import io.micronaut.kubernetes.client.openapi.api.CoreV1Api
import io.micronaut.kubernetes.client.openapi.model.V1ConfigMap
import io.micronaut.kubernetes.client.openapi.model.V1Pod
import io.micronaut.kubernetes.client.openapi.model.V1PodSpec
import io.micronaut.kubernetes.client.openapi.model.V1Secret
import io.micronaut.kubernetes.openapi.test.K3sContainerSpec
import org.slf4j.Logger
import org.slf4j.LoggerFactory

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

        V1Secret secret1 = getSecretModel(SECRET_NAME_1, ["username": "user".bytes, "password": "pass".bytes])
        createSecret(api, NAMESPACE_NAME_1, secret1)
        V1Secret secret2 = getSecretModel(SECRET_NAME_2, ["secretKey2": "secretValue2".bytes], ["podLabelKey1": "podLabelValue1"])
        createSecret(api, NAMESPACE_NAME_1, secret2)
        V1Secret secret3 = getSecretModel(SECRET_NAME_3, ["secretKey3": "secretValue3".bytes], ["podLabelKey2": "podLabelValue2"])
        createSecret(api, NAMESPACE_NAME_1, secret3)

        V1PodSpec podSpec1 = getPodSpecModel([getContainerModel("test-cont-1")])
        V1Pod pod1 = getPodModel(POD_NAME, podSpec1, ["podLabelKey1": "podLabelValue1", "podLabelKey2": "podLabelValue2"])
        createPod(api, NAMESPACE_NAME_1, pod1)

        createNamespace(api, getNamespaceModel(NAMESPACE_NAME_2))

        createConfigMap(api, NAMESPACE_NAME_2, getLiteralConfigMap())

        V1Secret secret4 = getSecretModel(SECRET_NAME_4, ["secretKey4": "secretValue4".bytes])
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
                "micronaut.config.import[0]"         : "kubernetes://config-map?name=order-config-json&watch=false",
                "micronaut.config.import[1]"         : "kubernetes://config-map?name=order-config-properties&watch=false",
                "micronaut.config.import[2]"         : "kubernetes://config-map?name=order-config-yml&watch=false",
                "micronaut.config.import[3]"         : "kubernetes://config-map?name=order-config-literal&watch=false"
        ], Environment.KUBERNETES)

        expect:
        getProperty(context, "json-order-id", String.class) == "order-id-1"
        getProperty(context, "json-customer.customer-id", String.class) == "customer-id-1"
        getProperty(context, "json-customer.customer-name", String.class) == "customer-name-1"
        getProperty(context, "json-items[0].sku", String.class) == "sku-1"
        getProperty(context, "json-items[0].name", String.class) == "sku-name-1"
        getProperty(context, "json-items[0].quantity", Integer.class) == 11

        getProperty(context, "prop-order-id", String.class) == "order-id-2"
        getProperty(context, "prop-customer.customer-id", String.class) == "customer-id-2"
        getProperty(context, "prop-customer.customer-name", String.class) == "customer-name-2"
        getProperty(context, "prop-items[0].sku", String.class) == "sku-2"
        getProperty(context, "prop-items[0].name", String.class) == "sku-name-2"
        getProperty(context, "prop-items[0].quantity", String.class) == "22"

        getProperty(context, "yml-order-id", String.class) == "order-id-3"
        getProperty(context, "yml-customer.customer-id", String.class) == "customer-id-3"
        getProperty(context, "yml-customer.customer-name", String.class) == "customer-name-3"
        getProperty(context, "yml-items[0].sku", String.class) == "sku-3"
        getProperty(context, "yml-items[0].name", String.class) == "sku-name-3"
        getProperty(context, "yml-items[0].quantity", Integer.class) == 33

        getProperty(context, "literal.order-id", String.class) == "order-id-4"
        getProperty(context, "literal.customer-id", String.class) == "customer-id-4"

        cleanup:
        context.close()
    }

    def <T> T getProperty(ApplicationContext context, String name, Class<T> type) {
        return context.get(name, type).orElse(null)
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
        return getConfigMapModel(YAML_CONFIG_MAP_NAME, ["order.yml": ymlContent])
    }

    V1ConfigMap getLiteralConfigMap() {
        return getConfigMapModel(LITERAL_CONFIG_MAP_NAME, ["literal.orderId": "order-id-4", "literal.customerId": "customer-id-4"])
    }
}
