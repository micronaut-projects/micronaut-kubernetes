package io.micronaut.kubernetes.client.openapi.informer

import io.micronaut.context.ApplicationContext
import io.micronaut.context.annotation.Context
import io.micronaut.context.annotation.Requires
import io.micronaut.kubernetes.client.openapi.informer.handler.Informer
import io.micronaut.kubernetes.client.openapi.informer.handler.ResourceEventHandler
import io.micronaut.kubernetes.client.openapi.model.V1Namespace
import io.micronaut.kubernetes.client.openapi.model.V1ObjectMeta
import io.micronaut.kubernetes.client.openapi.model.V1Secret
import io.micronaut.kubernetes.client.openapi.reactor.api.CoreV1ApiReactor
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.testcontainers.containers.output.Slf4jLogConsumer
import org.testcontainers.k3s.K3sContainer
import org.testcontainers.utility.DockerImageName
import spock.lang.AutoCleanup
import spock.lang.Shared
import spock.lang.Specification
import spock.util.concurrent.PollingConditions

import java.nio.file.Files
import java.nio.file.Path

class InformerSpec extends Specification {

    private static final Logger LOG_K3S = LoggerFactory.getLogger(InformerSpec.getName() + "K3S")

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

    @Shared
    @AutoCleanup
    K3sContainer k3s = new K3sContainer(DockerImageName.parse("rancher/k3s:v1.21.3-k3s1"))
            .withLogConsumer(new Slf4jLogConsumer(LOG_K3S))

    @Shared
    Path kubeConfigDir = Files.createTempDirectory("kube-temp-")

    @Shared
    Path kubeConfigFile = kubeConfigDir.resolve("config")

    def setupSpec() {
        k3s.start()
        kubeConfigFile.toFile().text = k3s.getKubeConfigYaml()
        setupKubernetes()
    }

    def cleanupSpec() {
        if (kubeConfigFile != null) {
            Files.deleteIfExists(kubeConfigFile)
        }
        if (kubeConfigDir) {
            Files.deleteIfExists(kubeConfigDir)
        }
    }

    def setupKubernetes() {
        try (ApplicationContext context = ApplicationContext.run([
                'kubernetes.client.kube-config-path': "file:" + kubeConfigFile.toString(),
                'spec.name'                         : 'SetupKubernetes'
        ])) {
            CoreV1ApiReactor api = context.getBean(CoreV1ApiReactor.class)
            createNamespace(api, NAMESPACE_NAME_1)
            createSecret(api, NAMESPACE_NAME_1, SECRET_NAME_11, [:])
            createSecret(api, NAMESPACE_NAME_1, SECRET_NAME_12, ['label-key': 'label-value-1'])
            createSecret(api, NAMESPACE_NAME_1, SECRET_NAME_13, ['label-key': 'label-value-2'])
            createNamespace(api, NAMESPACE_NAME_2)
            createSecret(api, NAMESPACE_NAME_2, SECRET_NAME_21, [:])
            createSecret(api, NAMESPACE_NAME_2, SECRET_NAME_22, ['label-key': 'label-value-1'])
            createSecret(api, NAMESPACE_NAME_2, SECRET_NAME_23, ['label-key': 'label-value-2'])
        }
    }

    def 'test secret informer'() {
        given:
        ApplicationContext context = ApplicationContext.run([
                'kubernetes.client.kube-config-path': "file:" + kubeConfigFile.toString(),
                'spec.name'                         : 'WithoutLabelSelector'
        ])
        CoreV1ApiReactor api = context.getBean(CoreV1ApiReactor.class)
        AllNamespacesEventHandler allNamespacesEventHandler = context.getBean(AllNamespacesEventHandler.class)
        FirstNamespaceEventHandler firstNamespaceEventHandler = context.getBean(FirstNamespaceEventHandler.class)
        SecondNamespaceEventHandler secondNamespaceEventHandler = context.getBean(SecondNamespaceEventHandler.class)

        when:
        createSecret(api, NAMESPACE_NAME_1, SECRET_NAME_14, [:])
        replaceSecret(api, NAMESPACE_NAME_1, SECRET_NAME_14, [:])
        deleteSecret(api, NAMESPACE_NAME_1, SECRET_NAME_14)
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
        CoreV1ApiReactor api = context.getBean(CoreV1ApiReactor.class)
        AllNamespacesLabelSelectorEventHandler allNamespacesEventHandler = context.getBean(AllNamespacesLabelSelectorEventHandler.class)
        FirstNamespaceLabelSelectorEventHandler firstNamespaceEventHandler = context.getBean(FirstNamespaceLabelSelectorEventHandler.class)
        SecondNamespaceLabelSelectorEventHandler secondNamespaceEventHandler = context.getBean(SecondNamespaceLabelSelectorEventHandler.class)

        when:
        createSecret(api, NAMESPACE_NAME_1, SECRET_NAME_15, ['label-key': 'label-value-1'])
        replaceSecret(api, NAMESPACE_NAME_1, SECRET_NAME_15, ['label-key': 'label-value-1'])
        deleteSecret(api, NAMESPACE_NAME_1, SECRET_NAME_15)
        createSecret(api, NAMESPACE_NAME_1, SECRET_NAME_16, [:])
        deleteSecret(api, NAMESPACE_NAME_1, SECRET_NAME_16)
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

    private void createNamespace(CoreV1ApiReactor api, String namespaceName) {
        V1Namespace namespace = new V1Namespace()
        namespace.kind('Namespace')
        namespace.apiVersion('v1')
        V1ObjectMeta objectMeta = new V1ObjectMeta()
        objectMeta.name(namespaceName)
        namespace.metadata(objectMeta)
        api.createNamespace(namespace, null, null, null, null)
                .block()
    }

    private void createSecret(CoreV1ApiReactor api, String namespaceName, String secretName, Map<String, String> secretLabels) {
        V1Secret secret = new V1Secret()
        secret.kind('Secret')
        secret.apiVersion('v1')
        V1ObjectMeta objectMeta = new V1ObjectMeta()
        objectMeta.name(TEST_SECRET_NAME_PREFIX + secretName)
        objectMeta.labels(secretLabels)
        secret.metadata(objectMeta)
        secret.data(["test-key": "test-value".bytes])
        api.createNamespacedSecret(namespaceName, secret, null, null, null, null)
                .block()
    }

    private void replaceSecret(CoreV1ApiReactor api, String namespaceName, String secretName, Map<String, String> secretLabels) {
        V1Secret secret = new V1Secret()
        secret.kind('Secret')
        secret.apiVersion('v1')
        V1ObjectMeta objectMeta = new V1ObjectMeta()
        objectMeta.name(TEST_SECRET_NAME_PREFIX + secretName)
        objectMeta.labels(secretLabels)
        secret.metadata(objectMeta)
        secret.data(["test-key": "new-test-value".bytes])
        api.replaceNamespacedSecret(TEST_SECRET_NAME_PREFIX + secretName, namespaceName, secret, null, null, null, null)
                .block()
    }

    private void deleteSecret(CoreV1ApiReactor api, String namespaceName, String secretName) {
        api.deleteNamespacedSecret(TEST_SECRET_NAME_PREFIX + secretName, namespaceName, null, null, null, null, null, null)
                .block()
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
