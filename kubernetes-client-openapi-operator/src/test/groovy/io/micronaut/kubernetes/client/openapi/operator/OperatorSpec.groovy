package io.micronaut.kubernetes.client.openapi.operator

import io.micronaut.context.ApplicationContext
import io.micronaut.context.annotation.Context
import io.micronaut.context.annotation.Primary
import io.micronaut.context.annotation.Requires
import io.micronaut.context.env.Environment
import io.micronaut.kubernetes.client.openapi.api.CoreV1Api
import io.micronaut.kubernetes.client.openapi.common.KubernetesObject
import io.micronaut.kubernetes.client.openapi.informer.SharedIndexInformerFactory
import io.micronaut.kubernetes.client.openapi.informer.handler.Informer
import io.micronaut.kubernetes.client.openapi.model.V1ConfigMap
import io.micronaut.kubernetes.client.openapi.model.V1ObjectMeta
import io.micronaut.kubernetes.client.openapi.model.V1Secret
import io.micronaut.kubernetes.client.openapi.operator.controller.ControllerFactory
import io.micronaut.kubernetes.client.openapi.operator.controller.reconciler.Request
import io.micronaut.kubernetes.client.openapi.operator.controller.reconciler.ResourceReconciler
import io.micronaut.kubernetes.client.openapi.operator.controller.reconciler.Result
import io.micronaut.kubernetes.client.openapi.operator.util.ConfigMapOperation
import io.micronaut.kubernetes.client.openapi.operator.util.NamespaceOperation
import io.micronaut.kubernetes.client.openapi.operator.util.SecretOperation
import io.micronaut.kubernetes.client.openapi.resolver.PodNameResolver
import jakarta.annotation.PostConstruct
import jakarta.inject.Singleton
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import spock.util.concurrent.PollingConditions

import java.time.Duration
import java.util.function.BiPredicate
import java.util.function.Predicate

class OperatorSpec extends K3sContainerSpec {

    private static final Logger LOG = LoggerFactory.getLogger(OperatorSpec.class)

    private static final NAMESPACE_NAME_1 = 'operator-ns-1'
    private static final SECRET_NAME_11 = 'secret-11'
    private static final SECRET_NAME_12 = 'secret-12'
    private static final SECRET_NAME_13 = 'secret-13'
    private static final CONFIG_MAP_NAME_11 = 'config-map-11'
    private static final CONFIG_MAP_NAME_12 = 'config-map-12'
    private static final NAMESPACE_NAME_2 = 'operator-ns-2'
    private static final SECRET_NAME_21 = 'secret-21'

    @Override
    Logger getLogger() {
        return LOG
    }

    @Override
    def setupKubernetes(ApplicationContext context) {
        CoreV1Api api = context.getBean(CoreV1Api.class)

        NamespaceOperation namespaceOp = new NamespaceOperation(api)
        namespaceOp.createNamespace(NAMESPACE_NAME_1)
        namespaceOp.createNamespace(NAMESPACE_NAME_2)

        SecretOperation secretOp = new SecretOperation(api)
        secretOp.createSecret(SECRET_NAME_11, NAMESPACE_NAME_1, [:])
        secretOp.createSecret(SECRET_NAME_12, NAMESPACE_NAME_1, ['label-key': 'label-value-1'])
        secretOp.createSecret(SECRET_NAME_13, NAMESPACE_NAME_1, ['label-key': 'label-value-2'])
        secretOp.createSecret(SECRET_NAME_21, NAMESPACE_NAME_2, [:])

        ConfigMapOperation configMapOp = new ConfigMapOperation(api)
        configMapOp.createConfigMap(CONFIG_MAP_NAME_11, NAMESPACE_NAME_1, ['key1': 'value1'], [:])
        configMapOp.createConfigMap(CONFIG_MAP_NAME_12, NAMESPACE_NAME_1, ['key2': 'value2'], [:])
    }

    def cleanup() {
        try (ApplicationContext context = ApplicationContext.run([
                'spec.name'                                              : 'OperatorSpec',
                'spec.part'                                              : 'KubernetesCleanup',
                'kubernetes.client.kube-config-path'                     : 'file:' + kubeConfigFile.toString(),
                'kubernetes.client.operator.enabled'                     : false,
                'kubernetes.client.operator.leader-election.lock.enabled': false,
        ])) {
            CoreV1Api api = context.getBean(CoreV1Api.class)
            SecretOperation secretOp = new SecretOperation(api)
            secretOp.removeProcessedAnnotation(SECRET_NAME_11, NAMESPACE_NAME_1)
            secretOp.removeProcessedAnnotation(SECRET_NAME_12, NAMESPACE_NAME_1)
            secretOp.removeProcessedAnnotation(SECRET_NAME_13, NAMESPACE_NAME_1)
            secretOp.removeProcessedAnnotation(SECRET_NAME_21, NAMESPACE_NAME_2)
            ConfigMapOperation configMapOp = new ConfigMapOperation(api)
            configMapOp.removeProcessedAnnotation(CONFIG_MAP_NAME_11, NAMESPACE_NAME_1)
            configMapOp.removeProcessedAnnotation(CONFIG_MAP_NAME_12, NAMESPACE_NAME_1)
        }
    }

    def 'test operator when using all namespaces'() {
        given:
        ApplicationContext context = ApplicationContext.run([
                'kubernetes.client.kube-config-path'                            : 'file:' + kubeConfigFile.toString(),
                'spec.name'                                                     : 'OperatorSpec',
                'spec.part'                                                     : 'AllNamespaces',
                'kubernetes.client.operator.leader-election.lock.resource-name' : 'test-lock',
                'kubernetes.client.operator.leader-election.lock.retry-period'  : '2s',
                'kubernetes.client.operator.leader-election.lock.renew-deadline': '3s',
                'kubernetes.client.operator.leader-election.lock.lease-duration': '4s'
        ], Environment.KUBERNETES)
        CoreV1Api api = context.getBean(CoreV1Api.class)
        SecretOperation secretOp = new SecretOperation(api)

        when: 'there is no processed annotation at startup'
        PollingConditions conditions = new PollingConditions(timeout: 4)

        then: 'it is automatically added by controllers'
        conditions.eventually {
            secretOp.getProcessedAnnotation(SECRET_NAME_11, NAMESPACE_NAME_1) == 'processed'
            secretOp.getProcessedAnnotation(SECRET_NAME_21, NAMESPACE_NAME_2) == 'processed'
        }

        when: 'processed annotation is removed explicitly'
        secretOp.removeProcessedAnnotation(SECRET_NAME_11, NAMESPACE_NAME_1)
        secretOp.removeProcessedAnnotation(SECRET_NAME_21, NAMESPACE_NAME_2)

        then: 'it is automatically returned back by controllers'
        conditions.eventually {
            secretOp.getProcessedAnnotation(SECRET_NAME_11, NAMESPACE_NAME_1) == 'processed'
            secretOp.getProcessedAnnotation(SECRET_NAME_21, NAMESPACE_NAME_2) == 'processed'
        }

        cleanup:
        context.close()
    }

    def 'test operator when using all namespaces and filters in informer resource event handler'() {
        given:
        ApplicationContext context = ApplicationContext.run([
                'kubernetes.client.kube-config-path'                            : 'file:' + kubeConfigFile.toString(),
                'spec.name'                                                     : 'OperatorSpec',
                'spec.part'                                                     : 'AllNamespacesAndFilters',
                'spec.filter'                                                   : 'IncludeOnlyFilter',
                'kubernetes.client.operator.leader-election.lock.resource-name' : 'test-lock',
                'kubernetes.client.operator.leader-election.lock.retry-period'  : '2s',
                'kubernetes.client.operator.leader-election.lock.renew-deadline': '3s',
                'kubernetes.client.operator.leader-election.lock.lease-duration': '4s'
        ], Environment.KUBERNETES)
        CoreV1Api api = context.getBean(CoreV1Api.class)
        SecretOperation secretOp = new SecretOperation(api)

        when: 'there is no processed annotation at startup'
        PollingConditions conditions = new PollingConditions(timeout: 4)

        then: 'it is automatically added only to resources that satisfy the filters'
        conditions.eventually {
            secretOp.getProcessedAnnotation(SECRET_NAME_11, NAMESPACE_NAME_1) == 'processed'
            secretOp.getProcessedAnnotation(SECRET_NAME_21, NAMESPACE_NAME_2) == null
        }

        when: 'processed annotation is removed explicitly'
        secretOp.removeProcessedAnnotation(SECRET_NAME_11, NAMESPACE_NAME_1)

        then: 'it is automatically returned back only to resources that satisfy the filters'
        conditions.eventually {
            secretOp.getProcessedAnnotation(SECRET_NAME_11, NAMESPACE_NAME_1) == 'processed'
            secretOp.getProcessedAnnotation(SECRET_NAME_21, NAMESPACE_NAME_2) == null
        }

        cleanup:
        context.close()
    }

    def 'test operator when using explicitly defined namespaces'() {
        given:
        ApplicationContext context = ApplicationContext.run([
                'kubernetes.client.kube-config-path'                            : 'file:' + kubeConfigFile.toString(),
                'spec.name'                                                     : 'OperatorSpec',
                'spec.part'                                                     : 'ExplicitNamespaces',
                'kubernetes.client.operator.leader-election.lock.resource-name' : 'test-lock',
                'kubernetes.client.operator.leader-election.lock.retry-period'  : '2s',
                'kubernetes.client.operator.leader-election.lock.renew-deadline': '3s',
                'kubernetes.client.operator.leader-election.lock.lease-duration': '4s'
        ], Environment.KUBERNETES)
        CoreV1Api api = context.getBean(CoreV1Api.class)
        SecretOperation secretOp = new SecretOperation(api)

        when: 'there is no processed annotation at startup'
        PollingConditions conditions = new PollingConditions(timeout: 4)

        then: 'it is automatically added by controllers'
        conditions.eventually {
            secretOp.getProcessedAnnotation(SECRET_NAME_11, NAMESPACE_NAME_1) == 'processed'
            secretOp.getProcessedAnnotation(SECRET_NAME_21, NAMESPACE_NAME_2) == 'processed'
        }

        when: 'processed annotation is removed explicitly'
        secretOp.removeProcessedAnnotation(SECRET_NAME_11, NAMESPACE_NAME_1)
        secretOp.removeProcessedAnnotation(SECRET_NAME_21, NAMESPACE_NAME_2)

        then: 'it is automatically returned back by controllers'
        conditions.eventually {
            secretOp.getProcessedAnnotation(SECRET_NAME_11, NAMESPACE_NAME_1) == 'processed'
            secretOp.getProcessedAnnotation(SECRET_NAME_21, NAMESPACE_NAME_2) == 'processed'
        }

        cleanup:
        context.close()
    }

    def 'test operator when using single namespace and label selector'() {
        given:
        ApplicationContext context = ApplicationContext.run([
                'kubernetes.client.kube-config-path'                            : 'file:' + kubeConfigFile.toString(),
                'spec.name'                                                     : 'OperatorSpec',
                'spec.part'                                                     : 'SingleNamespaceAndLabelSelector',
                'kubernetes.client.operator.leader-election.lock.resource-name' : 'test-lock',
                'kubernetes.client.operator.leader-election.lock.retry-period'  : '2s',
                'kubernetes.client.operator.leader-election.lock.renew-deadline': '3s',
                'kubernetes.client.operator.leader-election.lock.lease-duration': '4s'
        ], Environment.KUBERNETES)
        CoreV1Api api = context.getBean(CoreV1Api.class)
        SecretOperation secretOp = new SecretOperation(api)

        when: 'there is no processed annotation at startup'
        PollingConditions conditions = new PollingConditions(timeout: 4)

        then: 'it is automatically added only to resources that satisfy the label selector'
        conditions.eventually {
            secretOp.getProcessedAnnotation(SECRET_NAME_11, NAMESPACE_NAME_1) == null
            secretOp.getProcessedAnnotation(SECRET_NAME_12, NAMESPACE_NAME_1) == 'processed'
            secretOp.getProcessedAnnotation(SECRET_NAME_13, NAMESPACE_NAME_1) == null
        }

        when: 'processed annotation is removed explicitly'
        secretOp.removeProcessedAnnotation(SECRET_NAME_12, NAMESPACE_NAME_1)

        then: 'it is automatically returned back only to resources that satisfy the label selector'
        conditions.eventually {
            secretOp.getProcessedAnnotation(SECRET_NAME_11, NAMESPACE_NAME_1) == null
            secretOp.getProcessedAnnotation(SECRET_NAME_12, NAMESPACE_NAME_1) == 'processed'
            secretOp.getProcessedAnnotation(SECRET_NAME_13, NAMESPACE_NAME_1) == null
        }

        cleanup:
        context.close()
    }

    def 'test operator when informer and operator are manually created'() {
        given:
        ApplicationContext context = ApplicationContext.run([
                'kubernetes.client.kube-config-path'                            : 'file:' + kubeConfigFile.toString(),
                'spec.name'                                                     : 'OperatorSpec',
                'spec.part'                                                     : 'ManuallyCreated',
                'kubernetes.client.operator.leader-election.lock.resource-name' : 'test-lock',
                'kubernetes.client.operator.leader-election.lock.retry-period'  : '2s',
                'kubernetes.client.operator.leader-election.lock.renew-deadline': '3s',
                'kubernetes.client.operator.leader-election.lock.lease-duration': '4s'
        ], Environment.KUBERNETES)
        CoreV1Api api = context.getBean(CoreV1Api.class)
        SecretOperation secretOp = new SecretOperation(api)

        when: 'there is no processed annotation at startup'
        PollingConditions conditions = new PollingConditions(timeout: 4)

        then: 'it is automatically added by controllers'
        conditions.eventually {
            secretOp.getProcessedAnnotation(SECRET_NAME_11, NAMESPACE_NAME_1) == 'processed'
            secretOp.getProcessedAnnotation(SECRET_NAME_12, NAMESPACE_NAME_1) == 'processed'
        }

        when: 'processed annotation is removed explicitly'
        secretOp.removeProcessedAnnotation(SECRET_NAME_11, NAMESPACE_NAME_1)
        secretOp.removeProcessedAnnotation(SECRET_NAME_12, NAMESPACE_NAME_1)

        then: 'it is automatically returned back by controllers'
        conditions.eventually {
            secretOp.getProcessedAnnotation(SECRET_NAME_11, NAMESPACE_NAME_1) == 'processed'
            secretOp.getProcessedAnnotation(SECRET_NAME_12, NAMESPACE_NAME_1) == 'processed'
        }

        cleanup:
        context.close()
    }

    def 'test multiple operators'() {
        given:
        ApplicationContext context = ApplicationContext.run([
                'kubernetes.client.kube-config-path'                            : 'file:' + kubeConfigFile.toString(),
                'spec.name'                                                     : 'OperatorSpec',
                'spec.part'                                                     : 'MultipleOperators',
                'kubernetes.client.operator.leader-election.lock.resource-name' : 'test-lock',
                'kubernetes.client.operator.leader-election.lock.retry-period'  : '2s',
                'kubernetes.client.operator.leader-election.lock.renew-deadline': '3s',
                'kubernetes.client.operator.leader-election.lock.lease-duration': '4s'
        ], Environment.KUBERNETES)
        CoreV1Api api = context.getBean(CoreV1Api.class)
        SecretOperation secretOp = new SecretOperation(api)
        ConfigMapOperation configMapOp = new ConfigMapOperation(api)

        when: 'there is no processed annotation at startup'
        PollingConditions conditions = new PollingConditions(timeout: 4)

        then: 'it is automatically added by controllers'
        conditions.eventually {
            secretOp.getProcessedAnnotation(SECRET_NAME_11, NAMESPACE_NAME_1) == 'processed'
            secretOp.getProcessedAnnotation(SECRET_NAME_12, NAMESPACE_NAME_1) == 'processed'
            configMapOp.getProcessedAnnotation(CONFIG_MAP_NAME_11, NAMESPACE_NAME_1) == 'processed'
            configMapOp.getProcessedAnnotation(CONFIG_MAP_NAME_12, NAMESPACE_NAME_1) == 'processed'
        }

        when: 'processed annotation is removed explicitly'
        secretOp.removeProcessedAnnotation(SECRET_NAME_11, NAMESPACE_NAME_1)
        secretOp.removeProcessedAnnotation(SECRET_NAME_12, NAMESPACE_NAME_1)
        configMapOp.removeProcessedAnnotation(CONFIG_MAP_NAME_11, NAMESPACE_NAME_1)
        configMapOp.removeProcessedAnnotation(CONFIG_MAP_NAME_12, NAMESPACE_NAME_1)

        then: 'it is automatically returned back by controllers'
        conditions.eventually {
            secretOp.getProcessedAnnotation(SECRET_NAME_11, NAMESPACE_NAME_1) == 'processed'
            secretOp.getProcessedAnnotation(SECRET_NAME_12, NAMESPACE_NAME_1) == 'processed'
            configMapOp.getProcessedAnnotation(CONFIG_MAP_NAME_11, NAMESPACE_NAME_1) == 'processed'
            configMapOp.getProcessedAnnotation(CONFIG_MAP_NAME_12, NAMESPACE_NAME_1) == 'processed'
        }

        cleanup:
        context.close()
    }

    private static abstract class BaseResourceEventHandler<ApiType extends KubernetesObject> implements ResourceReconciler<ApiType> {

        private final CoreV1Api coreV1Api

        BaseResourceEventHandler(CoreV1Api coreV1Api) {
            this.coreV1Api = coreV1Api
        }

        @Override
        Result reconcile(Request request, OperatorResourceLister<ApiType> lister) {
            Optional<ApiType> objectOpt = lister.get(request)
            if (objectOpt.isPresent()) {
                ApiType object = objectOpt.get()
                V1ObjectMeta metadata = object.getMetadata()

                Map<String, String> annotations = metadata.getAnnotations()
                if (annotations == null) {
                    annotations = new HashMap<>()
                    metadata.setAnnotations(annotations)
                }

                if (!annotations.containsKey('io.micronaut.operator')) {
                    annotations.put('io.micronaut.operator', 'processed')
                    String name = object.getMetadata().getName()
                    String namespace = object.getMetadata().getNamespace()
                    try {
                        if (object instanceof V1Secret) {
                            coreV1Api.replaceNamespacedSecret(name, namespace, object, null, null, null, null)
                        } else if (object instanceof V1ConfigMap) {
                            coreV1Api.replaceNamespacedConfigMap(name, namespace, object, null, null, null, null)
                        }
                    } catch (Exception e) {
                        LOG.error('Failed to update resource', e)
                        return new Result(true, Duration.ofSeconds(2))
                    }
                }
            }
            return new Result(false)
        }
    }

    @Operator(informer = @Informer(apiType = V1Secret.class, namespace = Informer.ALL_NAMESPACES))
    @Requires(property = 'spec.name', value = 'OperatorSpec')
    @Requires(property = 'spec.part', value = 'AllNamespaces')
    private static class AllNamespacesResourceReconciler extends BaseResourceEventHandler<V1Secret> {
        AllNamespacesResourceReconciler(CoreV1Api coreV1Api) {
            super(coreV1Api)
        }
    }

    @Operator(informer = @Informer(apiType = V1Secret.class, namespace = Informer.ALL_NAMESPACES), onAddFilter = OnAdd, onUpdateFilter = OnUpdate, onDeleteFilter = OnDelete)
    @Requires(property = 'spec.name', value = 'OperatorSpec')
    @Requires(property = 'spec.part', value = 'AllNamespacesAndFilters')
    private static class AllNamespacesAndFiltersResourceReconciler extends BaseResourceEventHandler<V1Secret> {
        AllNamespacesAndFiltersResourceReconciler(CoreV1Api coreV1Api) {
            super(coreV1Api)
        }
    }

    @Operator(informer = @Informer(apiType = V1Secret.class, namespaces = [NAMESPACE_NAME_1, NAMESPACE_NAME_2]))
    @Requires(property = 'spec.name', value = 'OperatorSpec')
    @Requires(property = 'spec.part', value = 'ExplicitNamespaces')
    private static class ExplicitNamespacesResourceReconciler extends BaseResourceEventHandler<V1Secret> {
        ExplicitNamespacesResourceReconciler(CoreV1Api coreV1Api) {
            super(coreV1Api)
        }
    }

    @Operator(informer = @Informer(apiType = V1Secret.class, namespace = NAMESPACE_NAME_1, labelSelector = 'label-key=label-value-1'))
    @Requires(property = 'spec.name', value = 'OperatorSpec')
    @Requires(property = 'spec.part', value = 'SingleNamespaceAndLabelSelector')
    private static class SingleNamespaceAndLabelSelectorResourceReconciler extends BaseResourceEventHandler<V1Secret> {
        SingleNamespaceAndLabelSelectorResourceReconciler(CoreV1Api coreV1Api) {
            super(coreV1Api)
        }
    }

    @Operator(informer = @Informer(apiType = V1Secret.class, namespace = NAMESPACE_NAME_1))
    @Requires(property = 'spec.name', value = 'OperatorSpec')
    @Requires(property = 'spec.part', value = 'MultipleOperators')
    private static class MultipleOperatorsSecretResourceReconciler extends BaseResourceEventHandler<V1Secret> {
        MultipleOperatorsSecretResourceReconciler(CoreV1Api coreV1Api) {
            super(coreV1Api)
        }
    }

    @Operator(informer = @Informer(apiType = V1ConfigMap.class, namespace = NAMESPACE_NAME_1))
    @Requires(property = 'spec.name', value = 'OperatorSpec')
    @Requires(property = 'spec.part', value = 'MultipleOperators')
    private static class MultipleOperatorsConfigMapResourceReconciler extends BaseResourceEventHandler<V1ConfigMap> {
        MultipleOperatorsConfigMapResourceReconciler(CoreV1Api coreV1Api) {
            super(coreV1Api)
        }
    }

    @Context
    @Requires(property = 'spec.name', value = 'OperatorSpec')
    @Requires(property = 'spec.part', value = 'ManuallyCreated')
    private static class ManualSetup {
        private final SharedIndexInformerFactory informerFactory
        private final ControllerFactory controllerFactory
        private final CoreV1Api coreV1Api

        ManualSetup(SharedIndexInformerFactory informerFactory, ControllerFactory controllerFactory, CoreV1Api coreV1Api) {
            this.informerFactory = informerFactory
            this.controllerFactory = controllerFactory
            this.coreV1Api = coreV1Api
        }

        @PostConstruct
        void createController() {
            informerFactory.sharedIndexInformerFor(V1Secret.class, NAMESPACE_NAME_1)
            controllerFactory.createController('test-name', V1Secret.class, Collections.singleton(NAMESPACE_NAME_1), new ManualResourceReconciler(coreV1Api))
        }
    }

    private static class ManualResourceReconciler extends BaseResourceEventHandler<V1Secret> {
        ManualResourceReconciler(CoreV1Api coreV1Api) {
            super(coreV1Api)
        }
    }

    @Context
    @Primary
    @Requires(property = 'spec.name', value = 'OperatorSpec')
    private static class CustomPodNameResolver implements PodNameResolver {
        @Override
        Optional<String> getPodName() {
            return Optional.of('test-pod-name')
        }
    }

    @Singleton
    @Requires(property = 'spec.name', value = 'OperatorSpec')
    @Requires(property = 'spec.filter', value = 'IncludeOnlyFilter')
    private static class OnAdd implements Predicate<KubernetesObject> {
        @Override
        boolean test(KubernetesObject kubernetesObject) {
            return Objects.equals(NAMESPACE_NAME_1, kubernetesObject.getMetadata().getNamespace())
                    && Objects.equals(SECRET_NAME_11, kubernetesObject.getMetadata().getName())
        }
    }

    @Singleton
    @Requires(property = 'spec.name', value = 'OperatorSpec')
    @Requires(property = 'spec.filter', value = 'IncludeOnlyFilter')
    private static class OnUpdate implements BiPredicate<KubernetesObject, KubernetesObject> {
        @Override
        boolean test(KubernetesObject kubernetesObject, KubernetesObject kubernetesObject2) {
            return Objects.equals(NAMESPACE_NAME_1, kubernetesObject.getMetadata().getNamespace())
                    && Objects.equals(SECRET_NAME_11, kubernetesObject.getMetadata().getName())
        }
    }

    @Singleton
    @Requires(property = 'spec.name', value = 'OperatorSpec')
    @Requires(property = 'spec.filter', value = 'IncludeOnlyFilter')
    private static class OnDelete implements BiPredicate<KubernetesObject, Boolean> {
        @Override
        boolean test(KubernetesObject kubernetesObject, Boolean aBoolean) {
            return Objects.equals(NAMESPACE_NAME_1, kubernetesObject.getMetadata().getNamespace())
                    && Objects.equals(SECRET_NAME_11, kubernetesObject.getMetadata().getName())
        }
    }
}
