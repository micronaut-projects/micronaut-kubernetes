package micronaut.informer

import groovy.util.logging.Slf4j
import io.fabric8.kubernetes.api.model.ContainerBuilder
import io.fabric8.kubernetes.api.model.ContainerPortBuilder
import io.fabric8.kubernetes.api.model.HTTPGetActionBuilder
import io.fabric8.kubernetes.api.model.IntOrString
import io.fabric8.kubernetes.api.model.LabelSelectorBuilder
import io.fabric8.kubernetes.api.model.ObjectMetaBuilder
import io.fabric8.kubernetes.api.model.PodSpecBuilder
import io.fabric8.kubernetes.api.model.PodTemplateSpecBuilder
import io.fabric8.kubernetes.api.model.ProbeBuilder
import io.fabric8.kubernetes.api.model.ServicePortBuilder
import io.fabric8.kubernetes.api.model.ServiceSpecBuilder
import io.fabric8.kubernetes.api.model.apps.DeploymentBuilder
import io.fabric8.kubernetes.api.model.apps.DeploymentSpecBuilder
import io.kubernetes.client.openapi.models.V1Secret
import io.micronaut.context.annotation.Property
import io.micronaut.context.annotation.Requires as MicronautRequires
import io.micronaut.context.env.Environment
import io.micronaut.http.MediaType
import io.micronaut.http.annotation.Get
import io.micronaut.http.client.annotation.Client
import io.micronaut.kubernetes.test.TestUtils
import io.micronaut.test.extensions.spock.annotation.MicronautTest
import jakarta.inject.Inject
import micronaut.informer.utils.KubernetesSpecification
import spock.lang.Requires
import spock.lang.Shared
import spock.util.concurrent.PollingConditions

import java.util.concurrent.TimeUnit

@MicronautTest(environments = [Environment.KUBERNETES], startApplication = false)
@Property(name = "spec.name", value = "SecretInformerControllerSpec")
@Property(name = "kubernetes.client.namespace", value = "micronaut-example-informer")
@Requires({ TestUtils.kubernetesApiAvailable() })
@Slf4j
class SecretInformerControllerSpec extends KubernetesSpecification {

    @Inject
    @Shared
    TestClient testClient

    @Override
    def setupFixture(String namespace) {
        createNamespaceSafe(namespace)
        createBaseResources(namespace)
        def imageName = getImageName("micronaut-kubernetes-informer-example")
        log.info("Image name: ${imageName}")

        def client = operations.getClient(namespace)
        def informerDeployment = client.apps().deployments().createOrReplace(
                new DeploymentBuilder()
                        .withMetadata(new ObjectMetaBuilder()
                                .withName("example-informer")
                                .build())
                        .withSpec(new DeploymentSpecBuilder()
                                .withSelector(new LabelSelectorBuilder()
                                        .addToMatchLabels("app", "example-informer")
                                        .build())
                                .withReplicas(1)
                                .withTemplate(new PodTemplateSpecBuilder()
                                        .withMetadata(new ObjectMetaBuilder()
                                                .withLabels(["app": "example-informer"])
                                                .build())
                                        .withSpec(new PodSpecBuilder()
                                                .withContainers(new ContainerBuilder()
                                                        .withName("informer")
                                                        .withImage(imageName)
                                                        .withImagePullPolicy("IfNotPresent")
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

        operations.createService("example-informer", namespace,
                new ServiceSpecBuilder()
                        .withType("LoadBalancer")
                        .withPorts(
                                new ServicePortBuilder()
                                        .withPort(8080)
                                        .withTargetPort(new IntOrString(8080))
                                        .build()
                        )
                        .withSelector(["app": "example-informer"])
                        .build())

        operations.portForwardService("example-informer", namespace, 8080, 8889)
    }

    void "test all"() {
        PollingConditions conditions = new PollingConditions(timeout: 30, delay: 2)
        expect:
        conditions.eventually {
            // kind adds "default-token" secret to every namespace. That's why we check with >= to make sure it works both with OKE and kind
            testClient.all().size() >= 3
            testClient.secret("mounted-secret")
            testClient.secret("another-secret")
            testClient.secret("test-secret")
            testClient.secret("test-secret").data.containsKey("username")
        }
    }


    void "test secret"() {
        given:
        PollingConditions conditions = new PollingConditions(timeout: 30, delay: 2)
        String secretName = "new-secret"

        expect:
        conditions.eventually {
            testClient.secret("another-secret")
        }

        when:
        operations.createSecret(secretName, namespace, ["foo": encodeSecret("bar")])

        then:
        conditions.eventually {
            testClient.all().size() >= 4
            testClient.secret("mounted-secret")
            testClient.secret("another-secret")
            testClient.secret("test-secret")
            testClient.secret(secretName)
        }

        when:
        operations.deleteSecret(secretName, namespace)

        then:
        conditions.eventually {
            testClient.all().size() >= 3
            testClient.secret("mounted-secret")
            testClient.secret("another-secret")
            testClient.secret("test-secret")
            !testClient.secret(secretName)
        }
    }

    @Client("http://localhost:8889")
    @MicronautRequires(property = "spec.name", value = "SecretInformerControllerSpec")
    static interface TestClient {

        @Get(uri = "/all", processes = MediaType.APPLICATION_JSON)
        Collection<V1Secret> all()

        @Get(uri = "/secret/{key}", processes = MediaType.APPLICATION_JSON)
        V1Secret secret(String key)
    }
}
