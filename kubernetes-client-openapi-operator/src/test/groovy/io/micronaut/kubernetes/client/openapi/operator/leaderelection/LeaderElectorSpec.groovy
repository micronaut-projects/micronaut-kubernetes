package io.micronaut.kubernetes.client.openapi.operator.leaderelection

import io.micronaut.context.ApplicationContext
import io.micronaut.context.ApplicationContextBuilder
import io.micronaut.context.annotation.Context
import io.micronaut.context.annotation.Primary
import io.micronaut.context.annotation.Requires
import io.micronaut.context.env.Environment
import io.micronaut.kubernetes.client.openapi.common.KubernetesObject
import io.micronaut.kubernetes.client.openapi.informer.SharedIndexInformer
import io.micronaut.kubernetes.client.openapi.informer.SharedIndexInformerFactory
import io.micronaut.kubernetes.client.openapi.informer.handler.Informer
import io.micronaut.kubernetes.client.openapi.model.V1Secret
import io.micronaut.kubernetes.client.openapi.operator.K3sContainerSpec
import io.micronaut.kubernetes.client.openapi.operator.Operator
import io.micronaut.kubernetes.client.openapi.operator.OperatorResourceLister
import io.micronaut.kubernetes.client.openapi.operator.controller.Controller
import io.micronaut.kubernetes.client.openapi.operator.controller.ControllerFactory
import io.micronaut.kubernetes.client.openapi.operator.controller.reconciler.Request
import io.micronaut.kubernetes.client.openapi.operator.controller.reconciler.ResourceReconciler
import io.micronaut.kubernetes.client.openapi.operator.controller.reconciler.Result
import io.micronaut.kubernetes.client.openapi.operator.leaderelection.event.LeaderChangedEvent
import io.micronaut.kubernetes.client.openapi.operator.workqueue.RateLimitingQueue
import io.micronaut.kubernetes.client.openapi.resolver.PodNameResolver
import io.micronaut.runtime.event.annotation.EventListener
import jakarta.inject.Singleton
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import spock.util.concurrent.PollingConditions

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import java.util.function.BiPredicate
import java.util.function.Function
import java.util.function.Predicate

class LeaderElectorSpec extends K3sContainerSpec {
    private static final Logger LOG = LoggerFactory.getLogger(LeaderElectorSpec.class)

    @Override
    Logger getLogger() {
        return LOG
    }

    def 'test leader election when lease lock is used'() {
        given:
        ApplicationContextBuilder contextBuilder1 = ApplicationContext.builder([
                'kubernetes.client.kube-config-path'                            : 'file:' + kubeConfigFile.toString(),
                'spec.name'                                                     : 'LeaderElectorSpec',
                'spec.part'                                                     : 'context1',
                'kubernetes.client.operator.leader-election.lock.resource-kind' : 'lease',
                'kubernetes.client.operator.leader-election.lock.resource-name' : 'test-lock',
                'kubernetes.client.operator.leader-election.lock.retry-period'  : '2s',
                'kubernetes.client.operator.leader-election.lock.renew-deadline': '3s',
                'kubernetes.client.operator.leader-election.lock.lease-duration': '4s'
        ], Environment.KUBERNETES)
        ApplicationContext context1 = contextBuilder1.build()

        ApplicationContextBuilder contextBuilder2 = ApplicationContext.builder([
                'kubernetes.client.kube-config-path'                            : 'file:' + kubeConfigFile.toString(),
                'spec.name'                                                     : 'LeaderElectorSpec',
                'spec.part'                                                     : 'context2',
                'kubernetes.client.operator.leader-election.lock.resource-kind' : 'lease',
                'kubernetes.client.operator.leader-election.lock.resource-name' : 'test-lock',
                'kubernetes.client.operator.leader-election.lock.retry-period'  : '2s',
                'kubernetes.client.operator.leader-election.lock.renew-deadline': '3s',
                'kubernetes.client.operator.leader-election.lock.lease-duration': '4s'
        ], Environment.KUBERNETES)
        ApplicationContext context2 = contextBuilder2.build()

        PollingConditions conditions = new PollingConditions(timeout: 5)

        when: 'only context1 is started'
        context1.start()

        then: 'context1 is a leader and its controllers are running'
        conditions.eventually {
            getFactory(context1).isRunning()
            getLeader(context1) == 'test-pod-name-1'
        }

        when: 'context2 is started'
        context2.start()

        then: 'context2 is not a leader and its controllers are not running'
        conditions.eventually {
            !getFactory(context2).isRunning()
            getLeader(context2) == 'test-pod-name-1'
        }

        when: 'context1 is stopped'
        context1.stop()

        then: 'context2 is a leader and its controllers are running'
        conditions.eventually {
            getFactory(context2).isRunning()
            getLeader(context2) == 'test-pod-name-2'
        }

        cleanup:
        context1.close()
        context2.close()
    }

    def 'test leader election when config map lock is used'() {
        given:
        ApplicationContextBuilder contextBuilder1 = ApplicationContext.builder([
                'kubernetes.client.kube-config-path'                            : 'file:' + kubeConfigFile.toString(),
                'spec.name'                                                     : 'LeaderElectorSpec',
                'spec.part'                                                     : 'context1',
                'kubernetes.client.operator.leader-election.lock.resource-kind' : 'configmap',
                'kubernetes.client.operator.leader-election.lock.resource-name' : 'test-lock',
                'kubernetes.client.operator.leader-election.lock.retry-period'  : '2s',
                'kubernetes.client.operator.leader-election.lock.renew-deadline': '3s',
                'kubernetes.client.operator.leader-election.lock.lease-duration': '4s'
        ], Environment.KUBERNETES)
        ApplicationContext context1 = contextBuilder1.build()

        ApplicationContextBuilder contextBuilder2 = ApplicationContext.builder([
                'kubernetes.client.kube-config-path'                            : 'file:' + kubeConfigFile.toString(),
                'spec.name'                                                     : 'LeaderElectorSpec',
                'spec.part'                                                     : 'context2',
                'kubernetes.client.operator.leader-election.lock.resource-kind' : 'configmap',
                'kubernetes.client.operator.leader-election.lock.resource-name' : 'test-lock',
                'kubernetes.client.operator.leader-election.lock.retry-period'  : '2s',
                'kubernetes.client.operator.leader-election.lock.renew-deadline': '3s',
                'kubernetes.client.operator.leader-election.lock.lease-duration': '4s'
        ], Environment.KUBERNETES)
        ApplicationContext context2 = contextBuilder2.build()

        PollingConditions conditions = new PollingConditions(timeout: 5)

        when: 'only context1 is started'
        context1.start()

        then: 'context1 is a leader and its controllers are running'
        conditions.eventually {
            getFactory(context1).isRunning()
            getLeader(context1) == 'test-pod-name-1'
        }

        when: 'context2 is started'
        context2.start()

        then: 'context2 is not a leader and its controllers are not running'
        conditions.eventually {
            !getFactory(context2).isRunning()
            getLeader(context2) == 'test-pod-name-1'
        }

        when: 'context1 is stopped'
        context1.stop()

        then: 'context2 is a leader and its controllers are running'
        conditions.eventually {
            getFactory(context2).isRunning()
            getLeader(context2) == 'test-pod-name-2'
        }

        cleanup:
        context1.close()
        context2.close()
    }

    def 'test leader election when endpoints lock is used'() {
        given:
        ApplicationContextBuilder contextBuilder1 = ApplicationContext.builder([
                'kubernetes.client.kube-config-path'                            : 'file:' + kubeConfigFile.toString(),
                'spec.name'                                                     : 'LeaderElectorSpec',
                'spec.part'                                                     : 'context1',
                'kubernetes.client.operator.leader-election.lock.resource-kind' : 'endpoints',
                'kubernetes.client.operator.leader-election.lock.resource-name' : 'test-lock',
                'kubernetes.client.operator.leader-election.lock.retry-period'  : '2s',
                'kubernetes.client.operator.leader-election.lock.renew-deadline': '3s',
                'kubernetes.client.operator.leader-election.lock.lease-duration': '4s'
        ], Environment.KUBERNETES)
        ApplicationContext context1 = contextBuilder1.build()

        ApplicationContextBuilder contextBuilder2 = ApplicationContext.builder([
                'kubernetes.client.kube-config-path'                            : 'file:' + kubeConfigFile.toString(),
                'spec.name'                                                     : 'LeaderElectorSpec',
                'spec.part'                                                     : 'context2',
                'kubernetes.client.operator.leader-election.lock.resource-kind' : 'endpoints',
                'kubernetes.client.operator.leader-election.lock.resource-name' : 'test-lock',
                'kubernetes.client.operator.leader-election.lock.retry-period'  : '2s',
                'kubernetes.client.operator.leader-election.lock.renew-deadline': '3s',
                'kubernetes.client.operator.leader-election.lock.lease-duration': '4s'
        ], Environment.KUBERNETES)
        ApplicationContext context2 = contextBuilder2.build()

        PollingConditions conditions = new PollingConditions(timeout: 5)

        when: 'only context1 is started'
        context1.start()

        then: 'context1 is a leader and its controllers are running'
        conditions.eventually {
            getFactory(context1).isRunning()
            getLeader(context1) == 'test-pod-name-1'
        }

        when: 'context2 is started'
        context2.start()

        then: 'context2 is not a leader and its controllers are not running'
        conditions.eventually {
            !getFactory(context2).isRunning()
            getLeader(context2) == 'test-pod-name-1'
        }

        when: 'context1 is stopped'
        context1.stop()

        then: 'context2 is a leader and its controllers are running'
        conditions.eventually {
            getFactory(context2).isRunning()
            getLeader(context2) == 'test-pod-name-2'
        }

        cleanup:
        context1.close()
        context2.close()
    }

    private CustomControllerFactory getFactory(ApplicationContext context) {
        return ((CustomControllerFactory) context.getBean(ControllerFactory.class))
    }

    private String getLeader(ApplicationContext context) {
        return context.getBean(LeaderChangedListener.class).getLeader()
    }

    @Operator(informer = @Informer(apiType = V1Secret.class, namespace = Informer.ALL_NAMESPACES))
    @Requires(property = 'spec.name', value = 'LeaderElectorSpec')
    private static class SecretResourceReconciler implements ResourceReconciler<V1Secret> {
        @Override
        Result reconcile(Request request, OperatorResourceLister<V1Secret> lister) {
            return new Result(false)
        }
    }

    @Singleton
    @Primary
    @Requires(property = 'spec.name', value = 'LeaderElectorSpec')
    private static class CustomController implements Controller {
        private final AtomicBoolean running = new AtomicBoolean(false)

        private final String name

        CustomController(String name) {
            this.name = name
        }

        @Override
        String getName() {
            return name
        }

        @Override
        void shutdown() {
            running.set(false)
        }

        @Override
        void run() {
            running.set(true)
        }

        boolean isRunning() {
            return running.get()
        }
    }

    @Singleton
    @Primary
    @Requires(property = 'spec.name', value = 'LeaderElectorSpec')
    private static class CustomControllerFactory implements ControllerFactory {
        private final Map<String, CustomController> controllers = new ConcurrentHashMap<>()

        @Override
        <ApiType extends KubernetesObject> Controller createController(
                Class<ApiType> apiTypeClass, Set<String> namespaces, ResourceReconciler<ApiType> resourceReconciler) {
            return createController('test-name')
        }

        @Override
        <ApiType extends KubernetesObject> Controller createController(
                String name, Class<ApiType> apiTypeClass, Set<String> namespaces, ResourceReconciler<ApiType> resourceReconciler) {
            return createController(name)
        }

        @Override
        <ApiType extends KubernetesObject> Controller createController(
                String name, Class<ApiType> apiTypeClass, Set<String> namespaces,
                ResourceReconciler<ApiType> resourceReconciler, RateLimitingQueue<Request> workQueue) {
            return createController(name)
        }

        @Override
        <ApiType extends KubernetesObject> Controller createController(
                String name, Class<ApiType> apiTypeClass, Set<String> namespaces, ResourceReconciler<ApiType> resourceReconciler,
                RateLimitingQueue<Request> workQueue, Predicate<ApiType> onAddFilterPredicate, BiPredicate<ApiType, ApiType> onUpdateFilterPredicate,
                BiPredicate<ApiType, Boolean> onDeleteFilterPredicate) {
            return createController(name)
        }

        CustomController createController(String name) {
            CustomController controller = new CustomController(name)
            controllers.put(controller.getName(), controller)
            return controller
        }

        @Override
        void startControllers() {
            controllers.values().forEach(controller -> controller.run())
        }

        @Override
        void stopControllers() {
            controllers.values().forEach(controller -> controller.shutdown())
        }

        boolean isRunning() {
            for (CustomController customController : controllers.values()) {
                if (!customController.isRunning()) {
                    return false
                }
            }
            return true
        }
    }

    @Singleton
    @Primary
    @Requires(property = 'spec.name', value = 'LeaderElectorSpec')
    private static class CustomSharedIndexInformerFactory implements SharedIndexInformerFactory {
        @Override
        <ApiType extends KubernetesObject> SharedIndexInformer<ApiType> sharedIndexInformerFor(
                Class<ApiType> apiTypeClass, String namespace) {
            return null
        }

        @Override
        <ApiType extends KubernetesObject> SharedIndexInformer<ApiType> sharedIndexInformerFor(
                Class<ApiType> apiTypeClass, String namespace, String labelSelector) {
            return null
        }

        @Override
        <ApiType extends KubernetesObject> SharedIndexInformer<ApiType> sharedIndexInformerFor(
                Class<ApiType> apiTypeClass, String namespace, String labelSelector, boolean waitForInitialSync) {
            return null
        }

        @Override
        <ApiType extends KubernetesObject> SharedIndexInformer<ApiType> sharedIndexInformerFor(
                Class<ApiType> apiTypeClass, String namespace, String labelSelector, boolean waitForInitialSync, long resyncPeriodMillis) {
            return null
        }

        @Override
        <ApiType extends KubernetesObject> SharedIndexInformer<ApiType> sharedIndexInformerFor(
                Class<ApiType> apiTypeClass, String namespace, String labelSelector, boolean waitForInitialSync,
                long resyncPeriodMillis, Function<ApiType, String> cacheKeyFunction, Map<String, Function<ApiType, List<String>>> cacheIndexFunctions) {
            return null
        }

        @Override
        <ApiType extends KubernetesObject> List<SharedIndexInformer<ApiType>> sharedIndexInformersFor(
                Class<ApiType> apiTypeClass, List<String> namespaces, String labelSelector, boolean waitForInitialSync, long resyncPeriodMillis) {
            return null
        }

        @Override
        <ApiType extends KubernetesObject> SharedIndexInformer<ApiType> getExistingSharedIndexInformer(
                Class<ApiType> apiTypeClass, String namespace) {
            return null
        }

        @Override
        void startAllRegisteredInformers() {}

        @Override
        void stopAllRegisteredInformers() {}
    }

    @Singleton
    @Requires(property = 'spec.name', value = 'LeaderElectorSpec')
    private static class LeaderChangedListener {

        private String leader

        String getLeader() {
            return leader
        }

        @EventListener
        void leaderChanged(LeaderChangedEvent leaderChangedEvent) {
            leader = leaderChangedEvent.leaderElectionRecord().holderIdentity()
        }
    }

    @Context
    @Primary
    @Requires(property = 'spec.name', value = 'LeaderElectorSpec')
    @Requires(property = 'spec.part', value = 'context1')
    private static class CustomPodNameResolver1 implements PodNameResolver {
        @Override
        Optional<String> getPodName() {
            return Optional.of('test-pod-name-1')
        }
    }

    @Context
    @Primary
    @Requires(property = 'spec.name', value = 'LeaderElectorSpec')
    @Requires(property = 'spec.part', value = 'context2')
    private static class CustomPodNameResolver2 implements PodNameResolver {
        @Override
        Optional<String> getPodName() {
            return Optional.of('test-pod-name-2')
        }
    }
}
