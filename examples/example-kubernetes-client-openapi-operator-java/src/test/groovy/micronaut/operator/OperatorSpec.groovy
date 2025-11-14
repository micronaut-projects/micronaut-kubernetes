package micronaut.operator

import io.micronaut.context.annotation.Property
import io.micronaut.context.env.Environment
import io.micronaut.kubernetes.openapi.test.KubernetesSpecification
import io.micronaut.kubernetes.openapi.test.TestUtils
import io.micronaut.test.extensions.spock.annotation.MicronautTest
import spock.lang.Requires
import spock.util.concurrent.PollingConditions

import static io.micronaut.kubernetes.openapi.test.KubernetesModels.getConfigMapModel
import static io.micronaut.kubernetes.openapi.test.KubernetesOperations.createConfigMap
import static io.micronaut.kubernetes.openapi.test.KubernetesOperations.getConfigMap

@MicronautTest(environments = [Environment.KUBERNETES])
@Requires({ TestUtils.kubernetesApiAvailable() })
@Property(name = "spec.type", value = "example-test")
@Property(name = "spec.name", value = "OperatorSpec")
@Property(name = "kubernetes.client.namespace", value = "example-openapi-operator")
@Property(name = "kubernetes.client.operator.leader-election.lock.enabled", value = "false")
class OperatorSpec extends KubernetesSpecification {

    @Override
    def createResources() {
        createDeployment("example-operator", "micronaut-kubernetes-example-operator-openapi", 8082, false)
    }

    void "test config map reconciler"() {
        given:
        PollingConditions conditions = new PollingConditions(timeout: 30, delay: 1)
        String configMapName = "test-configmap-1"

        when:
        createConfigMap(coreV1Api, namespace, getConfigMapModel(configMapName, ["foo": "bar"]))

        then:
        conditions.eventually {
            def configMap = getConfigMap(coreV1Api, configMapName, namespace)
            def metadata = configMap.metadata
            def annotations = metadata.annotations
            assert annotations != null && annotations["io.micronaut.operator"] == "processed"
        }
    }
}
