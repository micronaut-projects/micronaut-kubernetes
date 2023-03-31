package io.micronaut.kubernetes.client.operator

import io.kubernetes.client.extended.controller.ControllerManager
import io.kubernetes.client.extended.controller.DefaultController
import io.kubernetes.client.extended.controller.LeaderElectingController
import io.micronaut.context.ApplicationContext
import io.micronaut.context.annotation.Property
import io.micronaut.context.env.Environment
import io.micronaut.core.type.Argument
import io.micronaut.kubernetes.test.KubernetesSpecification
import io.micronaut.test.extensions.spock.annotation.MicronautTest
import jakarta.inject.Inject
import spock.util.concurrent.PollingConditions

@MicronautTest(environments = [Environment.KUBERNETES])
@Property(name = "spec.name", value = "ConfigMapResourceReconcilerSpec")
@Property(name = "micronaut.application.name", value = "simple-reconciler")
@Property(name = "kubernetes.client.namespace", value = "simple-reconciler")
@Property(name = "kubernetes.client.operator.leader-election.lock.resource-kind", value = "configmap")
// the lock is intentionally configured to use default namespace since the simple-reconciler namespace
// will be created once the setup happens
@Property(name = "kubernetes.client.operator.leader-election.lock.resource-namespace", value = "default")
@Property(name = "kubernetes.client.operator.leader-election.lock.resource-name", value = "test-lock")
@Property(name = "spec.reuseNamespace", value = "false")
class ConfigMapResourceReconcilerSpec extends KubernetesSpecification{

    @Inject
    ApplicationContext applicationContext

    @Inject
    ConfigMapResourceReconciler configMapOperator

    @Override
    def setupFixture(String namespace) {
        createNamespaceSafe(namespace)
        operations.deleteConfigMap("test-lock", "default")
    }

    def "it receives the requests"(){
        given:
        PollingConditions conditions = new PollingConditions()

        expect:
        conditions.within(5) {
            configMapOperator.leaseAcquired.get()
        }

        when:
        operations.createConfigMap("first", namespace, ["foo":"bar"])

        then:
        conditions.within(10) {
            configMapOperator.requestList.contains("first")
        }

        cleanup:
        operations.deleteConfigMap("first", namespace)
    }

    def "context contains controllers"(){
        expect:
        applicationContext.getBean(Argument.of(DefaultController, "V1ConfigMap"))
        applicationContext.getBean(Argument.of(ControllerManager, "V1ConfigMap"))
        applicationContext.getBean(Argument.of(LeaderElectingController, "V1ConfigMap"))
    }
}
