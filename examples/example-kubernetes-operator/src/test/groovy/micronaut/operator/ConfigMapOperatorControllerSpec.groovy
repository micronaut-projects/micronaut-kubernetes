package micronaut.operator

import io.kubernetes.client.custom.IntOrString
import io.kubernetes.client.openapi.models.V1Container
import io.kubernetes.client.openapi.models.V1ContainerPort
import io.kubernetes.client.openapi.models.V1Deployment
import io.kubernetes.client.openapi.models.V1DeploymentSpec
import io.kubernetes.client.openapi.models.V1HTTPGetAction
import io.kubernetes.client.openapi.models.V1LabelSelector
import io.kubernetes.client.openapi.models.V1PodSpec
import io.kubernetes.client.openapi.models.V1PodTemplateSpec
import io.kubernetes.client.openapi.models.V1Probe
import io.micronaut.context.annotation.Property
import io.micronaut.context.env.Environment
import io.micronaut.kubernetes.test.KubernetesSpecification
import io.micronaut.kubernetes.test.TestUtils
import io.micronaut.test.extensions.spock.annotation.MicronautTest
import spock.lang.Requires
import spock.util.concurrent.PollingConditions

import static io.micronaut.kubernetes.test.KubernetesModels.getObjectMetaModel
import static io.micronaut.kubernetes.test.KubernetesModels.getServicePortModel
import static io.micronaut.kubernetes.test.KubernetesModels.getServiceSpecTypeModel
import static io.micronaut.kubernetes.test.KubernetesOperations.createConfigMap
import static io.micronaut.kubernetes.test.KubernetesOperations.createDeployment
import static io.micronaut.kubernetes.test.KubernetesOperations.createRole
import static io.micronaut.kubernetes.test.KubernetesOperations.createRoleBinding
import static io.micronaut.kubernetes.test.KubernetesOperations.createService
import static io.micronaut.kubernetes.test.KubernetesOperations.deleteConfigMap
import static io.micronaut.kubernetes.test.KubernetesOperations.deleteConfigMapNotFoundSafe
import static io.micronaut.kubernetes.test.KubernetesOperations.getConfigMap
import static io.micronaut.kubernetes.test.KubernetesOperations.getConfigMapNotFoundSafe

@MicronautTest(startApplication = false, environments = [Environment.KUBERNETES])
@Property(name = "spec.name", value = "ConfigMapOperatorControllerSpec")
@Property(name = "spec.reuseNamespace", value = "false")
@Property(name = "kubernetes.client.namespace", value = "micronaut-example-operator")
@Requires({ TestUtils.kubernetesApiAvailable() })
class ConfigMapOperatorControllerSpec extends KubernetesSpecification {

    static String configMapName = "new-configmap"

    @Override
    def setupFixture(String namespace) {
        createNamespaceSafe(namespace)
        deleteConfigMapNotFoundSafe(configMapName, namespace)

        createRole("operator-reconciler-role",
                namespace,
                [""],
                ["get", "list", "watch", "create", "update", "patch", "delete"],
                ["configmaps"])

        createRole("operator-lease-role",
                namespace,
                ["coordination.k8s.io"],
                ["get", "create", "update", "delete"],
                ["leases"]
        )
        createRoleBinding("operator-reconciler", namespace, "operator-reconciler-role")
        createRoleBinding("operator-lease-role", namespace, "operator-lease-role")

        def deployment = new V1Deployment()
                .metadata(getObjectMetaModel("example-operator"))
                .spec(new V1DeploymentSpec()
                        .selector(new V1LabelSelector().matchLabels(["app": "example-operator"]))
                        .replicas(1)
                        .template(new V1PodTemplateSpec()
                                .metadata(getObjectMetaModel(null, ["app": "example-operator"]))
                                .spec(new V1PodSpec()
                                        .containers([
                                                new V1Container()
                                                        .name("operator")
                                                        .image("micronaut-kubernetes-operator-example")
                                                        .imagePullPolicy("Never")
                                                        .ports([
                                                                new V1ContainerPort()
                                                                        .name("http")
                                                                        .containerPort(8080)
                                                        ])
                                                        .livenessProbe(new V1Probe()
                                                                .httpGet(new V1HTTPGetAction()
                                                                        .path("/health/liveness")
                                                                        .port(new IntOrString(8080)))
                                                                .initialDelaySeconds(1)
                                                                .periodSeconds(1)
                                                                .failureThreshold(10)
                                                        )
                                                        .readinessProbe(new V1Probe()
                                                                .httpGet(new V1HTTPGetAction()
                                                                        .path("/health/readiness")
                                                                        .port(new IntOrString(8080)))
                                                                .initialDelaySeconds(1)
                                                                .periodSeconds(1)
                                                                .failureThreshold(10)
                                                        )
                                        ])
                                )
                        )
                )

        createDeployment(namespace, deployment)

        createService(
                "example-operator",
                namespace,
                getServiceSpecTypeModel("LoadBalancer", [getServicePortModel(8080, 8080)], ["app": "example-operator"]))
    }

    void "test reconciler"() {
        given:
        PollingConditions conditions = new PollingConditions(timeout: 30, delay: 2)

        expect:
        !getConfigMapNotFoundSafe(configMapName, namespace)

        when:
        createConfigMap(configMapName, namespace)

        then:
        conditions.eventually {
            getConfigMap(configMapName, namespace).getMetadata().getAnnotations().containsKey("io.micronaut.operator")
        }

        when:
        deleteConfigMap(configMapName, namespace)

        then:
        conditions.eventually {
            !getConfigMapNotFoundSafe(configMapName, namespace)
        }
    }
}
