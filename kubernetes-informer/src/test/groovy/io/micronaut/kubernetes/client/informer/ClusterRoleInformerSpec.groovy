package io.micronaut.kubernetes.client.informer


import io.fabric8.kubernetes.api.model.rbac.ClusterRole
import io.fabric8.kubernetes.api.model.rbac.ClusterRoleBuilder
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
@Property(name = "kubernetes.client.namespace", value = "micronaut-cluster-role-informer")
@Property(name = "spec.reuseNamespace", value = "false")
@Property(name = "spec.name", value = "ClusterRoleInformerSpec")
class ClusterRoleInformerSpec extends KubernetesSpecification {

    @Shared
    @Inject
    ApplicationContext applicationContext

    @Override
    def setupFixture(String namespace) {
        createNamespaceSafe(namespace)
    }

    def "cluster role informer is notified"() {
        given:
        ClusterRoleInformer resourceHandler = applicationContext.getBean(ClusterRoleInformer)
        def clusterRoles = operations.getClient(namespace).rbac().clusterRoles()

        expect:
        !resourceHandler.added.isEmpty()
        resourceHandler.updated.isEmpty()
        resourceHandler.deleted.isEmpty()

        when:
        ClusterRole clusterRole = clusterRoles.create(new ClusterRoleBuilder()
                .withNewMetadata()
                .withName("test-role")
                .endMetadata()
                .addNewRule()
                .withApiGroups("*")
                .withResources("*")
                .withVerbs("get")
                .endRule()
                .build()
        )

        then:
        new PollingConditions().within(5) {
            assert resourceHandler.added.stream()
                    .filter(n -> n.getMetadata().name == "test-role")
                    .findFirst()
                    .get()
        }

        when:
        def policyRule = clusterRole.getRules().get(0)
        policyRule.getVerbs().add("watch")
        clusterRoles.createOrReplace(clusterRole)

        then:
        new PollingConditions().within(5) {
            assert resourceHandler.updated.size() == 1
        }

        when:
        clusterRoles.delete(clusterRole)

        then:
        new PollingConditions().within(5) {
            assert resourceHandler.deleted.size() == 1
        }
    }
}
