package micronaut.operator

import io.fabric8.kubernetes.api.model.*
import io.fabric8.kubernetes.api.model.apps.DeploymentBuilder
import io.fabric8.kubernetes.api.model.apps.DeploymentSpecBuilder
import io.micronaut.context.annotation.Property
import io.micronaut.context.env.Environment
import io.micronaut.kubernetes.test.KubernetesSpecification
import io.micronaut.kubernetes.test.TestUtils
import io.micronaut.test.extensions.spock.annotation.MicronautTest
import spock.lang.Requires
import spock.util.concurrent.PollingConditions

import java.util.concurrent.TimeUnit

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
        operations.deleteConfigMap(configMapName, namespace)

        operations.createRole("operator-reconciler-role", namespace,
                "",
                ["get", "list", "watch", "create", "update", "patch", "delete"],
                ["configmaps"])

        operations.createRole("operator-lease-role", namespace,
                "coordination.k8s.io",
                ["get", "create", "update", "delete"],
                ["leases"]
        )
        operations.createRoleBinding("operator-reconciler", namespace, "operator-reconciler-role")
        operations.createRoleBinding("operator-lease-role", namespace, "operator-lease-role")

        def client = operations.getClient(namespace)
        def informerDeployment = client.apps().deployments().createOrReplace(
                new DeploymentBuilder()
                        .withMetadata(new ObjectMetaBuilder()
                                .withName("example-operator")
                                .build())
                        .withSpec(new DeploymentSpecBuilder()
                                .withSelector(new LabelSelectorBuilder()
                                        .addToMatchLabels("app", "example-operator")
                                        .build())
                                .withReplicas(1)
                                .withTemplate(new PodTemplateSpecBuilder()
                                        .withMetadata(new ObjectMetaBuilder()
                                                .withLabels(["app": "example-operator"])
                                                .build())
                                        .withSpec(new PodSpecBuilder()
                                                .withContainers(new ContainerBuilder()
                                                        .withName("operator")
                                                        .withImage("micronaut-kubernetes-operator-example")
                                                        .withImagePullPolicy("Never")
                                                        .withPorts(new ContainerPortBuilder()
                                                                .withName("http")
                                                                .withContainerPort(8080)
                                                                .build())
                                                        .withLivenessProbe(new ProbeBuilder()
                                                                .withHttpGet(new HTTPGetActionBuilder()
                                                                        .withPath("/health/liveness")
                                                                        .withPort(new IntOrString(8080))
                                                                        .build())
                                                                .withInitialDelaySeconds(1)
                                                                .withPeriodSeconds(1)
                                                                .withFailureThreshold(10)
                                                                .build())
                                                        .withReadinessProbe(new ProbeBuilder()
                                                                .withHttpGet(new HTTPGetActionBuilder()
                                                                        .withPath("/health/readiness")
                                                                        .withPort(new IntOrString(8080))
                                                                        .build())
                                                                .withInitialDelaySeconds(1)
                                                                .withPeriodSeconds(1)
                                                                .withFailureThreshold(10)
                                                                .build())
                                                        .build())
                                                .build())
                                        .build())
                                .build())
                        .build())

        client.apps().deployments().inNamespace(informerDeployment.getMetadata().getNamespace())
                .withName(informerDeployment.getMetadata().getName()).waitUntilReady(250, TimeUnit.SECONDS)

        operations.createService("example-operator", namespace,
                new ServiceSpecBuilder()
                        .withType("LoadBalancer")
                        .withPorts(
                                new ServicePortBuilder()
                                        .withPort(8080)
                                        .withTargetPort(new IntOrString(8080))
                                        .build()
                        )
                        .withSelector(["app": "example-operator"])
                        .build())
    }

    void "test reconciler"() {
        given:
        PollingConditions conditions = new PollingConditions(timeout: 30, delay: 2)

        expect:
        !operations.getConfigMap(configMapName, namespace);

        when:
        operations.createConfigMap(configMapName, namespace)

        then:
        conditions.eventually {
            operations.getConfigMap(configMapName, namespace).getMetadata().getAnnotations().containsKey("io.micronaut.operator")
        }

        when:
        operations.deleteConfigMap(configMapName, namespace)

        then:
        conditions.eventually {
            !operations.getConfigMap(configMapName, namespace)
        }
    }
}
