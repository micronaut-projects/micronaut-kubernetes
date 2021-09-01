package io.micronaut.kubernetes.client.informer


import io.fabric8.kubernetes.api.model.Namespace
import io.micronaut.context.ApplicationContext
import io.micronaut.context.annotation.Property
import io.micronaut.context.env.Environment
import io.micronaut.kubernetes.test.KubernetesSpecification
import io.micronaut.kubernetes.test.TestUtils
import io.micronaut.test.extensions.spock.annotation.MicronautTest
import jakarta.inject.Inject
import spock.lang.Requires
import spock.lang.Shared
import spock.util.concurrent.PollingConditions

@MicronautTest(environments = [Environment.KUBERNETES])
@Requires({ TestUtils.kubernetesApiAvailable() })
@Property(name = "kubernetes.client.namespace", value = "micronaut-namespace-informer")
@Property(name = "spec.reuseNamespace", value = "false")
@Property(name = "spec.name", value = "NamespaceInformerSpec")
class NamespaceInformerSpec extends KubernetesSpecification {

    @Shared
    @Inject
    ApplicationContext applicationContext

    @Override
    def setupFixture(String namespace) {
        createNamespaceSafe(namespace)
    }

    def "config map informer is notified"() {
        given:
        NamespaceInformer resourceHandler = applicationContext.getBean(NamespaceInformer)

        expect:
        !resourceHandler.added.isEmpty()
        resourceHandler.updated.isEmpty()
        resourceHandler.deleted.isEmpty()

        when:
        Namespace ns = operations.createNamespace("test-ns")

        then:
        new PollingConditions().within(5) {
            assert resourceHandler.added.stream()
                    .filter(n -> n.getMetadata().name == "test-ns")
                    .findFirst()
                    .get()
        }

        when:
        ns.getSpec().getFinalizers().add("finalizer")
        operations.updateNamespace(ns)

        then:
        new PollingConditions().within(5) {
            assert resourceHandler.updated.size() == 1
        }

        when:
        ns.getSpec().getFinalizers().clear()
        operations.updateNamespace(ns)

        then:
        new PollingConditions().within(5) {
            assert resourceHandler.updated.size() == 2
        }

        when:
        operations.deleteNamespace("test-ns")

        then:
        new PollingConditions().within(5) {
            assert resourceHandler.deleted.size() == 1
        }
    }
}
