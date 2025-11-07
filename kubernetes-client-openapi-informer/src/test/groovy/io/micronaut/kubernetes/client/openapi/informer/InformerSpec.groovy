package io.micronaut.kubernetes.client.openapi.informer

import io.micronaut.context.ApplicationContext
import io.micronaut.context.annotation.Context
import io.micronaut.context.annotation.Requires
import io.micronaut.kubernetes.client.openapi.api.CoreV1Api
import io.micronaut.kubernetes.client.openapi.informer.handler.Informer
import io.micronaut.kubernetes.client.openapi.informer.handler.ResourceEventHandler
import io.micronaut.kubernetes.client.openapi.model.V1Secret
import io.micronaut.kubernetes.openapi.test.K3sContainerSpec
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import spock.util.concurrent.PollingConditions

import static io.micronaut.kubernetes.openapi.test.KubernetesModels.getNamespaceModel
import static io.micronaut.kubernetes.openapi.test.KubernetesModels.getSecretModel
import static io.micronaut.kubernetes.openapi.test.KubernetesOperations.createNamespace
import static io.micronaut.kubernetes.openapi.test.KubernetesOperations.createSecret
import static io.micronaut.kubernetes.openapi.test.KubernetesOperations.deleteSecret
import static io.micronaut.kubernetes.openapi.test.KubernetesOperations.replaceSecret

class InformerSpec extends K3sContainerSpec {

    private static final Logger LOG = LoggerFactory.getLogger(InformerSpec.class)

    private static final String TEST_SECRET_NAME_PREFIX = "informer-"

    private static final NAMESPACE_NAME_1 = 'secret-informer-ns-1'
    private static final SECRET_NAME_11 = 'test-11'
    private static final SECRET_NAME_12 = 'test-12'
    private static final SECRET_NAME_13 = 'test-13'
    private static final SECRET_NAME_14 = 'test-14'
    private static final SECRET_NAME_15 = 'test-15'
    private static final SECRET_NAME_16 = 'test-16'
    private static final NAMESPACE_NAME_2 = 'secret-informer-ns-2'
    private static final SECRET_NAME_21 = 'test-21'
    private static final SECRET_NAME_22 = 'test-22'
    private static final SECRET_NAME_23 = 'test-23'

    @Override
    Logger getLogger() {
        return LOG
    }

    @Override
    def setupKubernetes(ApplicationContext context) {
        CoreV1Api api = context.getBean(CoreV1Api.class)
        createNamespace(api, getNamespaceModel(NAMESPACE_NAME_1))
        createSecretUsingPrefix(api, NAMESPACE_NAME_1, SECRET_NAME_11, [:])
        createSecretUsingPrefix(api, NAMESPACE_NAME_1, SECRET_NAME_12, ['label-key': 'label-value-1'])
        createSecretUsingPrefix(api, NAMESPACE_NAME_1, SECRET_NAME_13, ['label-key': 'label-value-2'])
        createNamespace(api, getNamespaceModel(NAMESPACE_NAME_2))
        createSecretUsingPrefix(api, NAMESPACE_NAME_2, SECRET_NAME_21, [:])
        createSecretUsingPrefix(api, NAMESPACE_NAME_2, SECRET_NAME_22, ['label-key': 'label-value-1'])
        createSecretUsingPrefix(api, NAMESPACE_NAME_2, SECRET_NAME_23, ['label-key': 'label-value-2'])
    }

    def 'test secret informer'() {
        given:
        ApplicationContext context = ApplicationContext.run([
                'kubernetes.client.kube-config-path': "file:" + kubeConfigFile.toString(),
                'spec.name'                         : 'WithoutLabelSelector'
        ])
        CoreV1Api api = context.getBean(CoreV1Api.class)
        AllNamespacesEventHandler allNamespacesEventHandler = context.getBean(AllNamespacesEventHandler.class)
        FirstNamespaceEventHandler firstNamespaceEventHandler = context.getBean(FirstNamespaceEventHandler.class)
        SecondNamespaceEventHandler secondNamespaceEventHandler = context.getBean(SecondNamespaceEventHandler.class)

        when:
        createSecretUsingPrefix(api, NAMESPACE_NAME_1, SECRET_NAME_14, [:])
        replaceSecretUsingPrefix(api, NAMESPACE_NAME_1, SECRET_NAME_14, [:])
        deleteSecretUsingPrefix(api, NAMESPACE_NAME_1, SECRET_NAME_14)
        def allNamespacesEventMessages = allNamespacesEventHandler.getEventMessages()
        def firstNamespacesEventMessages = firstNamespaceEventHandler.getEventMessages()
        def secondNamespacesEventMessages = secondNamespaceEventHandler.getEventMessages()

        PollingConditions conditions = new PollingConditions(timeout: 2)

        then:
        conditions.eventually {
            allNamespacesEventMessages.size() == 7
            allNamespacesEventMessages.get(SECRET_NAME_11).size() == 1
            allNamespacesEventMessages.get(SECRET_NAME_11).get(0) == 'Secret added'
            allNamespacesEventMessages.get(SECRET_NAME_12).size() == 1
            allNamespacesEventMessages.get(SECRET_NAME_12).get(0) == 'Secret added'
            allNamespacesEventMessages.get(SECRET_NAME_13).size() == 1
            allNamespacesEventMessages.get(SECRET_NAME_13).get(0) == 'Secret added'
            allNamespacesEventMessages.get(SECRET_NAME_21).size() == 1
            allNamespacesEventMessages.get(SECRET_NAME_21).get(0) == 'Secret added'
            allNamespacesEventMessages.get(SECRET_NAME_22).size() == 1
            allNamespacesEventMessages.get(SECRET_NAME_22).get(0) == 'Secret added'
            allNamespacesEventMessages.get(SECRET_NAME_23).size() == 1
            allNamespacesEventMessages.get(SECRET_NAME_23).get(0) == 'Secret added'
            allNamespacesEventMessages.get(SECRET_NAME_14).size() == 3
            allNamespacesEventMessages.get(SECRET_NAME_14).get(0) == 'Secret added'
            allNamespacesEventMessages.get(SECRET_NAME_14).get(1) == 'Secret updated'
            allNamespacesEventMessages.get(SECRET_NAME_14).get(2) == 'Secret deleted'

            firstNamespacesEventMessages.size() == 4
            firstNamespacesEventMessages.get(SECRET_NAME_11).size() == 1
            firstNamespacesEventMessages.get(SECRET_NAME_11).get(0) == 'Secret added'
            firstNamespacesEventMessages.get(SECRET_NAME_12).size() == 1
            firstNamespacesEventMessages.get(SECRET_NAME_12).get(0) == 'Secret added'
            firstNamespacesEventMessages.get(SECRET_NAME_13).size() == 1
            firstNamespacesEventMessages.get(SECRET_NAME_13).get(0) == 'Secret added'
            firstNamespacesEventMessages.get(SECRET_NAME_14).size() == 3
            firstNamespacesEventMessages.get(SECRET_NAME_14).get(0) == 'Secret added'
            firstNamespacesEventMessages.get(SECRET_NAME_14).get(1) == 'Secret updated'
            firstNamespacesEventMessages.get(SECRET_NAME_14).get(2) == 'Secret deleted'

            secondNamespacesEventMessages.size() == 3
            secondNamespacesEventMessages.get(SECRET_NAME_21).size() == 1
            secondNamespacesEventMessages.get(SECRET_NAME_21).get(0) == 'Secret added'
            secondNamespacesEventMessages.get(SECRET_NAME_22).size() == 1
            secondNamespacesEventMessages.get(SECRET_NAME_22).get(0) == 'Secret added'
            secondNamespacesEventMessages.get(SECRET_NAME_23).size() == 1
            secondNamespacesEventMessages.get(SECRET_NAME_23).get(0) == 'Secret added'
        }

        cleanup:
        context.close()
    }

    def 'test secret informer when label selector used'() {
        given:
        ApplicationContext context = ApplicationContext.run([
                'kubernetes.client.kube-config-path': "file:" + kubeConfigFile.toString(),
                'spec.name'                         : 'WithLabelSelector'
        ])
        CoreV1Api api = context.getBean(CoreV1Api.class)
        AllNamespacesLabelSelectorEventHandler allNamespacesEventHandler = context.getBean(AllNamespacesLabelSelectorEventHandler.class)
        FirstNamespaceLabelSelectorEventHandler firstNamespaceEventHandler = context.getBean(FirstNamespaceLabelSelectorEventHandler.class)
        SecondNamespaceLabelSelectorEventHandler secondNamespaceEventHandler = context.getBean(SecondNamespaceLabelSelectorEventHandler.class)

        when:
        createSecretUsingPrefix(api, NAMESPACE_NAME_1, SECRET_NAME_15, ['label-key': 'label-value-1'])
        replaceSecretUsingPrefix(api, NAMESPACE_NAME_1, SECRET_NAME_15, ['label-key': 'label-value-1'])
        deleteSecretUsingPrefix(api, NAMESPACE_NAME_1, SECRET_NAME_15)
        createSecretUsingPrefix(api, NAMESPACE_NAME_1, SECRET_NAME_16, [:])
        deleteSecretUsingPrefix(api, NAMESPACE_NAME_1, SECRET_NAME_16)
        def allNamespacesEventMessages = allNamespacesEventHandler.getEventMessages()
        def firstNamespacesEventMessages = firstNamespaceEventHandler.getEventMessages()
        def secondNamespacesEventMessages = secondNamespaceEventHandler.getEventMessages()

        PollingConditions conditions = new PollingConditions(timeout: 2)

        then:
        conditions.eventually {
            allNamespacesEventMessages.size() == 3
            allNamespacesEventMessages.get(SECRET_NAME_12).size() == 1
            allNamespacesEventMessages.get(SECRET_NAME_12).get(0) == 'Secret added'
            allNamespacesEventMessages.get(SECRET_NAME_22).size() == 1
            allNamespacesEventMessages.get(SECRET_NAME_22).get(0) == 'Secret added'
            allNamespacesEventMessages.get(SECRET_NAME_15).size() == 3
            allNamespacesEventMessages.get(SECRET_NAME_15).get(0) == 'Secret added'
            allNamespacesEventMessages.get(SECRET_NAME_15).get(1) == 'Secret updated'
            allNamespacesEventMessages.get(SECRET_NAME_15).get(2) == 'Secret deleted'

            firstNamespacesEventMessages.size() == 2
            firstNamespacesEventMessages.get(SECRET_NAME_12).size() == 1
            firstNamespacesEventMessages.get(SECRET_NAME_12).get(0) == 'Secret added'
            firstNamespacesEventMessages.get(SECRET_NAME_15).size() == 3
            firstNamespacesEventMessages.get(SECRET_NAME_15).get(0) == 'Secret added'
            firstNamespacesEventMessages.get(SECRET_NAME_15).get(1) == 'Secret updated'
            firstNamespacesEventMessages.get(SECRET_NAME_15).get(2) == 'Secret deleted'

            secondNamespacesEventMessages.size() == 1
            secondNamespacesEventMessages.get(SECRET_NAME_22).size() == 1
            secondNamespacesEventMessages.get(SECRET_NAME_22).get(0) == 'Secret added'
        }

        cleanup:
        context.close()
    }

    def 'test secret informer wait for initial sync'() {
        given:
        ApplicationContext context = ApplicationContext.run([
                'kubernetes.client.kube-config-path': "file:" + kubeConfigFile.toString(),
                'spec.name'                         : 'WaitOnInitialSync'
        ])
        def sharedIndexInformerFactory = context.getBean(SharedIndexInformerFactory.class)
        def eventHandler = context.getBean(FirstNamespaceWaitOnInitialSyncEventHandler.class)

        when:
        def informer = sharedIndexInformerFactory.getExistingSharedIndexInformer(V1Secret.class, NAMESPACE_NAME_1)
        def keys = informer.getIndexer().listKeys()
        def eventMessages = eventHandler.getEventMessages()

        then:
        keys.size() == 2
        keys.get(0) == NAMESPACE_NAME_1 + "/" + TEST_SECRET_NAME_PREFIX + SECRET_NAME_12
        keys.get(1) == NAMESPACE_NAME_1 + "/" + TEST_SECRET_NAME_PREFIX + SECRET_NAME_13
        eventMessages.size() == 2
        eventMessages.get(SECRET_NAME_12).size() == 1
        eventMessages.get(SECRET_NAME_12).get(0) == 'Secret added'
        eventMessages.get(SECRET_NAME_13).size() == 1
        eventMessages.get(SECRET_NAME_13).get(0) == 'Secret added'

        cleanup:
        context.close()
    }

    private static void createSecretUsingPrefix(CoreV1Api api, String namespace, String secretName, Map<String, String> secretLabels) {
        createSecret(api, namespace, getSecretModel(TEST_SECRET_NAME_PREFIX + secretName, ["test-key": "test-value".bytes], secretLabels))
    }

    private static void replaceSecretUsingPrefix(CoreV1Api api, String namespace, String secretName, Map<String, String> secretLabels) {
        replaceSecret(api, namespace, getSecretModel(TEST_SECRET_NAME_PREFIX + secretName, ["test-key": "new-test-value".bytes], secretLabels))
    }

    private static void deleteSecretUsingPrefix(CoreV1Api api, String namespace, String secretName) {
        deleteSecret(api, namespace, TEST_SECRET_NAME_PREFIX + secretName)
    }

    private static class BaseResourceEventHandler implements ResourceEventHandler<V1Secret> {

        private final Map<String, List<String>> eventMessages = new HashMap<>()

        Map<String, List<String>> getEventMessages() {
            return eventMessages
        }

        @Override
        void onAdd(V1Secret obj) {
            String name = obj.getMetadata().getName()
            if (name.startsWith(TEST_SECRET_NAME_PREFIX)) {
                name = name.substring(TEST_SECRET_NAME_PREFIX.length())
                eventMessages.computeIfAbsent(name, k -> new ArrayList<>()).add("Secret added")
            }
        }

        @Override
        void onUpdate(V1Secret oldObj, V1Secret newObj) {
            String name = oldObj.getMetadata().getName()
            if (name.startsWith(TEST_SECRET_NAME_PREFIX)) {
                name = name.substring(TEST_SECRET_NAME_PREFIX.length())
                eventMessages.computeIfAbsent(name, k -> new ArrayList<>()).add("Secret updated")
            }
        }

        @Override
        void onDelete(V1Secret obj, boolean deletedFinalStateUnknown) {
            String name = obj.getMetadata().getName()
            if (name.startsWith(TEST_SECRET_NAME_PREFIX)) {
                name = name.substring(TEST_SECRET_NAME_PREFIX.length())
                eventMessages.computeIfAbsent(name, k -> new ArrayList<>()).add("Secret deleted")
            }
        }
    }

    @Context
    @Informer(apiType = V1Secret.class, namespace = Informer.ALL_NAMESPACES)
    @Requires(property = 'spec.name', value = 'WithoutLabelSelector')
    private static final class AllNamespacesEventHandler extends BaseResourceEventHandler {}

    @Context
    @Informer(apiType = V1Secret.class, namespace = NAMESPACE_NAME_1)
    @Requires(property = 'spec.name', value = 'WithoutLabelSelector')
    private static final class FirstNamespaceEventHandler extends BaseResourceEventHandler {}

    @Context
    @Informer(apiType = V1Secret.class, namespace = NAMESPACE_NAME_2)
    @Requires(property = 'spec.name', value = 'WithoutLabelSelector')
    private static final class SecondNamespaceEventHandler extends BaseResourceEventHandler {}

    @Context
    @Informer(apiType = V1Secret.class, namespace = Informer.ALL_NAMESPACES, labelSelector = "label-key=label-value-1")
    @Requires(property = 'spec.name', value = 'WithLabelSelector')
    private static final class AllNamespacesLabelSelectorEventHandler extends BaseResourceEventHandler {}

    @Context
    @Informer(apiType = V1Secret.class, namespace = NAMESPACE_NAME_1, labelSelector = "label-key=label-value-1")
    @Requires(property = 'spec.name', value = 'WithLabelSelector')
    private static final class FirstNamespaceLabelSelectorEventHandler extends BaseResourceEventHandler {}

    @Context
    @Informer(apiType = V1Secret.class, namespace = NAMESPACE_NAME_2, labelSelector = "label-key=label-value-1")
    @Requires(property = 'spec.name', value = 'WithLabelSelector')
    private static final class SecondNamespaceLabelSelectorEventHandler extends BaseResourceEventHandler {}

    @Context
    @Informer(apiType = V1Secret.class, namespace = NAMESPACE_NAME_1, labelSelector = "label-key", waitForInitialSync = true)
    @Requires(property = 'spec.name', value = 'WaitOnInitialSync')
    private static final class FirstNamespaceWaitOnInitialSyncEventHandler extends BaseResourceEventHandler {}
}
