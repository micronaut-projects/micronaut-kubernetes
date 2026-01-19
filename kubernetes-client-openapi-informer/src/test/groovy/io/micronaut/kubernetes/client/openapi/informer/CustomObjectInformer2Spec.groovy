package io.micronaut.kubernetes.client.openapi.informer

import io.micronaut.context.ApplicationContext
import io.micronaut.context.annotation.Context
import io.micronaut.context.annotation.Requires
import io.micronaut.kubernetes.client.openapi.api.ApiextensionsV1Api
import io.micronaut.kubernetes.client.openapi.api.CoreV1Api
import io.micronaut.kubernetes.client.openapi.informer.example2.CustomObject
import io.micronaut.kubernetes.client.openapi.informer.example2.CustomObjectApiReactor
import io.micronaut.kubernetes.client.openapi.informer.example2.CustomObjectCollection
import io.micronaut.kubernetes.client.openapi.informer.handler.Informer
import io.micronaut.kubernetes.client.openapi.informer.handler.ResourceEventHandler
import io.micronaut.kubernetes.client.openapi.model.V1CustomResourceDefinition
import io.micronaut.kubernetes.client.openapi.model.V1CustomResourceDefinitionNames
import io.micronaut.kubernetes.client.openapi.model.V1CustomResourceDefinitionSpec
import io.micronaut.kubernetes.client.openapi.model.V1CustomResourceDefinitionVersion
import io.micronaut.kubernetes.client.openapi.model.V1CustomResourceValidation
import io.micronaut.kubernetes.client.openapi.model.V1JSONSchemaProps
import io.micronaut.kubernetes.client.openapi.model.V1ObjectMeta
import io.micronaut.kubernetes.client.openapi.response.DeleteResponse
import io.micronaut.kubernetes.openapi.test.K3sContainerSpec
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import spock.util.concurrent.PollingConditions

import java.util.concurrent.ConcurrentHashMap

import static io.micronaut.kubernetes.openapi.test.KubernetesModels.getNamespaceModel
import static io.micronaut.kubernetes.openapi.test.KubernetesModels.getObjectMetaModel
import static io.micronaut.kubernetes.openapi.test.KubernetesOperations.createNamespace

class CustomObjectInformer2Spec extends K3sContainerSpec {

    private static final Logger LOG = LoggerFactory.getLogger(CustomObjectInformer2Spec.class)

    private static final NAMESPACE_NAME_1 = 'custom-object-informer-ns'

    @Override
    Logger getLogger() {
        return LOG
    }

    @Override
    def setupKubernetes(ApplicationContext context) {
        createCustomResourceDefinition(context.getBean(ApiextensionsV1Api.class))
        createNamespace(context.getBean(CoreV1Api.class), getNamespaceModel(NAMESPACE_NAME_1))
    }

    def 'test custom object informer'() {
        given:
        ApplicationContext context = ApplicationContext.run([
                'kubernetes.client.kube-config-path': "file:" + kubeConfigFile.toString(),
                'spec.name'                         : 'CustomObjectInformer2Spec'
        ])
        CustomObjectApiReactor customObjectApiReactor = context.getBean(CustomObjectApiReactor.class)
        CustomObjectEventHandler customObjectEventHandler = context.getBean(CustomObjectEventHandler.class)

        when:
        createCustomObject(context.getBean(CustomObjectApiReactor.class), NAMESPACE_NAME_1, getCustomObjectModel("custom-object-1", "test-value-1"))
        CustomObject customObject1 = readCustomObject(customObjectApiReactor, NAMESPACE_NAME_1, "custom-object-1")
        createCustomObject(customObjectApiReactor, NAMESPACE_NAME_1, getCustomObjectModel("custom-object-2", "test-value-2"))
        CustomObjectCollection customObjectList = listCustomObject(customObjectApiReactor, NAMESPACE_NAME_1)

        then:
        customObject1.getValue() == "test-value-1"
        customObjectList.getItems().size() == 2
        customObjectList.getItems().get(0).getValue() == "test-value-1"
        customObjectList.getItems().get(1).getValue() == "test-value-2"

        when:
        customObject1.setValue("test-value-1-new")
        replaceCustomObject(customObjectApiReactor, NAMESPACE_NAME_1, "custom-object-1", customObject1)
        deleteCustomObject(customObjectApiReactor, NAMESPACE_NAME_1, "custom-object-2")
        customObjectList = listCustomObject(customObjectApiReactor, NAMESPACE_NAME_1)
        def eventMessages = customObjectEventHandler.getEventMessages()
        PollingConditions conditions = new PollingConditions(timeout: 2)

        then:
        customObjectList.getItems().size() == 1
        customObjectList.getItems().get(0).getValue() == "test-value-1-new"
        conditions.eventually {
            eventMessages.size() == 2
            eventMessages.get("custom-object-1").size() == 2
            eventMessages.get("custom-object-1").get(0) == "Custom object added"
            eventMessages.get("custom-object-1").get(1) == "Custom object updated"
            eventMessages.get("custom-object-2").size() == 2
            eventMessages.get("custom-object-2").get(0) == 'Custom object added'
            eventMessages.get("custom-object-2").get(1) == 'Custom object deleted'
        }

        cleanup:
        context.close()
    }

    private static void createCustomResourceDefinition(ApiextensionsV1Api apiextensionsV1Api) {
        V1JSONSchemaProps jsonSchemaProps = new V1JSONSchemaProps()
        jsonSchemaProps.setType("object")
        jsonSchemaProps.setProperties([
                "apiVersion": getProp("string"),
                "kind"      : getProp("string"),
                "metadata"  : getProp("object"),
                "value"      : getProp("string")
        ])

        V1CustomResourceValidation customResourceValidation = new V1CustomResourceValidation()
        customResourceValidation.setOpenAPIV3Schema(jsonSchemaProps)

        V1CustomResourceDefinitionVersion customResourceDefinitionVersion = new V1CustomResourceDefinitionVersion("v1", true, true)
        customResourceDefinitionVersion.setSchema(customResourceValidation)

        V1CustomResourceDefinitionSpec customResourceDefinitionSpec = new V1CustomResourceDefinitionSpec(
                "custom.test.io",
                new V1CustomResourceDefinitionNames("CustomObject", "customobjects").singular("customobject"),
                "Namespaced",
                [customResourceDefinitionVersion])

        V1CustomResourceDefinition customResourceDefinition = new V1CustomResourceDefinition(customResourceDefinitionSpec)
        V1ObjectMeta metadata = new V1ObjectMeta()
        metadata.setName("customobjects.custom.test.io")
        customResourceDefinition.setMetadata(metadata)

        apiextensionsV1Api.createCustomResourceDefinition(customResourceDefinition, null, null, null, null)
    }

    private static V1JSONSchemaProps getProp(String type, Map<String, V1JSONSchemaProps> props = [:]) {
        V1JSONSchemaProps specProp = new V1JSONSchemaProps()
        specProp.setType(type)
        specProp.setProperties(props)
        return specProp
    }

    private static CustomObject getCustomObjectModel(String name, String value) {
        CustomObject customObject = new CustomObject()
        customObject.setApiVersion("custom.test.io/v1")
        customObject.setKind("CustomObject")
        customObject.setMetadata(getObjectMetaModel(name))
        customObject.setValue(value)
        return customObject
    }

    private static void createCustomObject(CustomObjectApiReactor customObjectApiReactor, String namespace, CustomObject customObject) {
        customObjectApiReactor.createNamespacedCustomObject(namespace, customObject, null, null, null, null).block()
    }

    private static DeleteResponse<CustomObject> deleteCustomObject(CustomObjectApiReactor customObjectApiReactor, String namespace, String name) {
        return customObjectApiReactor.deleteNamespacedCustomObject(name, namespace, null, null, null, null, null, null, null).block()
    }

    private static CustomObjectCollection listCustomObject(CustomObjectApiReactor customObjectApiReactor, String namespace) {
        return customObjectApiReactor.listNamespacedCustomObject(namespace, null, null, null, null, null, null, null, null, null, null, null).block()
    }

    private static CustomObject readCustomObject(CustomObjectApiReactor customObjectApiReactor, String namespace, String name) {
        return customObjectApiReactor.readNamespacedCustomObject(name, namespace, null).block()
    }

    private static void replaceCustomObject(CustomObjectApiReactor customObjectApiReactor, String namespace, String name, CustomObject customObject) {
        customObjectApiReactor.replaceNamespacedCustomObject(name, namespace, customObject, null, null, null, null).block()
    }

    @Context
    @Informer(apiType = CustomObject.class, namespace = NAMESPACE_NAME_1)
    @Requires(property = 'spec.name', value = 'CustomObjectInformer2Spec')
    private static final class CustomObjectEventHandler implements ResourceEventHandler<CustomObject> {

        private final Map<String, List<String>> eventMessages = new ConcurrentHashMap<>()

        Map<String, List<String>> getEventMessages() {
            return eventMessages
        }

        @Override
        void onAdd(CustomObject obj) {
            String name = obj.getMetadata().getName()
            eventMessages.computeIfAbsent(name, k -> new ArrayList<>()).add("Custom object added")
        }

        @Override
        void onUpdate(CustomObject oldObj, CustomObject newObj) {
            String name = oldObj.getMetadata().getName()
            eventMessages.computeIfAbsent(name, k -> new ArrayList<>()).add("Custom object updated")
        }

        @Override
        void onDelete(CustomObject obj, boolean deletedFinalStateUnknown) {
            String name = obj.getMetadata().getName()
            eventMessages.computeIfAbsent(name, k -> new ArrayList<>()).add("Custom object deleted")
        }
    }
}
