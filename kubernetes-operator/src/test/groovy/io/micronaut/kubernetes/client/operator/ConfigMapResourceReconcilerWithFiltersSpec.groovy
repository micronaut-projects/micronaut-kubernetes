package io.micronaut.kubernetes.client.operator

import io.kubernetes.client.openapi.models.V1ConfigMap
import io.micronaut.context.ApplicationContext
import io.micronaut.context.annotation.Property
import io.micronaut.context.env.Environment
import io.micronaut.kubernetes.test.KubernetesSpecification
import io.micronaut.test.extensions.spock.annotation.MicronautTest
import jakarta.inject.Inject
import spock.util.concurrent.PollingConditions

import static io.micronaut.kubernetes.test.KubernetesOperations.createConfigMap
import static io.micronaut.kubernetes.test.KubernetesOperations.deleteConfigMap
import static io.micronaut.kubernetes.test.KubernetesOperations.deleteConfigMapNotFoundSafe
import static io.micronaut.kubernetes.test.KubernetesOperations.modifyConfigMap

@MicronautTest(environments = [Environment.KUBERNETES])
@Property(name = "spec.name", value = "ConfigMapResourceReconcilerWithFiltersSpec")
@Property(name = "micronaut.application.name", value = "simple-reconciler")
@Property(name = "kubernetes.client.namespace", value = "simple-reconciler")
// the lock is intentionally configured to use default namespace since the simple-reconciler namespace
// will be created once the setup happens
@Property(name = "kubernetes.client.operator.leader-election.lock.resource-namespace", value = "default")
@Property(name = "kubernetes.client.operator.leader-election.lock.resource-name", value = "test-lock-2")
@Property(name = "spec.reuseNamespace", value = "false")
class ConfigMapResourceReconcilerWithFiltersSpec extends KubernetesSpecification{

    @Inject
    ApplicationContext applicationContext

    @Inject
    ConfigMapResourceReconcilerWithFilters configMapOperator

    @Inject
    OnAddFilter onAddFilter

    @Inject
    OnUpdateFilter onUpdateFilter

    @Inject
    OnDeleteFilter onDeleteFilter

    @Override
    def setupFixture(String namespace) {
        createNamespaceSafe(namespace)
        deleteConfigMapNotFoundSafe("test-lock-2", "default")
    }

    def "it receives the requests"(){
        given:
        PollingConditions conditions = new PollingConditions()

        expect:
        conditions.within(5) {
            configMapOperator.leaseAcquired.get()
        }

        when:
        V1ConfigMap first = createConfigMap("first", namespace, ["foo":"bar"])
        V1ConfigMap second = createConfigMap("second", namespace, ["foo":"bar"], [:], ["io.micronaut.operator":"true"])

        then:
        conditions.within(5) {
            !configMapOperator.requestList.contains("first")
            configMapOperator.requestList.contains("second")
        }

        when:
        configMapOperator.requestList.clear()
        first.data.put("bum", "bac")
        modifyConfigMap(first)

        second.data.put("bum", "bac")
        modifyConfigMap(second)

        then:
        conditions.within(5) {
            !configMapOperator.requestList.contains("first")
            configMapOperator.requestList.contains("second")
        }

        when:
        configMapOperator.requestList.clear()
        deleteConfigMap("first", namespace)
        deleteConfigMap("second", namespace)

        then:
        conditions.within(10) {
            !configMapOperator.requestList.contains("first")
            configMapOperator.requestList.contains("second")
        }
    }
}
