package micronaut.informer

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
import io.kubernetes.client.openapi.models.V1Secret
import io.micronaut.context.annotation.Property
import io.micronaut.context.annotation.Requires as MicronautRequires
import io.micronaut.context.env.Environment
import io.micronaut.http.MediaType
import io.micronaut.http.annotation.Get
import io.micronaut.http.client.annotation.Client
import io.micronaut.kubernetes.test.KubectlPortForward
import io.micronaut.kubernetes.test.KubernetesSpecification
import io.micronaut.kubernetes.test.TestUtils
import io.micronaut.test.extensions.spock.annotation.MicronautTest
import jakarta.inject.Inject
import spock.lang.AutoCleanup
import spock.lang.Requires
import spock.lang.Shared
import spock.util.concurrent.PollingConditions

import static io.micronaut.kubernetes.test.KubernetesModels.getObjectMetaModel
import static io.micronaut.kubernetes.test.KubernetesModels.getServicePortModel
import static io.micronaut.kubernetes.test.KubernetesModels.getServiceSpecTypeModel
import static io.micronaut.kubernetes.test.KubernetesOperations.createDeployment
import static io.micronaut.kubernetes.test.KubernetesOperations.createSecret
import static io.micronaut.kubernetes.test.KubernetesOperations.createService
import static io.micronaut.kubernetes.test.KubernetesOperations.deleteSecret
import static io.micronaut.kubernetes.test.KubernetesOperations.portForwardService

@MicronautTest(environments = [Environment.KUBERNETES], startApplication = false)
@Property(name = "spec.name", value = "SecretInformerControllerSpec")
@Property(name = "spec.reuseNamespace", value = "false")
@Property(name = "kubernetes.client.namespace", value = "micronaut-example-informer")
@Requires({ TestUtils.kubernetesApiAvailable() })
class SecretInformerControllerSpec extends KubernetesSpecification {

    @Inject
    @Shared
    TestClient testClient

    @Shared
    @AutoCleanup
    KubectlPortForward kubectlPortForward

    @Override
    def setupFixture(String namespace) {
        createNamespaceSafe(namespace)
        createBaseResources(namespace)

        def deployment = new V1Deployment()
                .metadata(getObjectMetaModel("example-informer"))
                .spec(new V1DeploymentSpec()
                        .selector(new V1LabelSelector().matchLabels(["app": "example-informer"]))
                        .replicas(1)
                        .template(new V1PodTemplateSpec()
                                .metadata(getObjectMetaModel(null, ["app": "example-informer"]))
                                .spec(new V1PodSpec()
                                        .containers([
                                                new V1Container()
                                                        .name("informer")
                                                        .image("micronaut-kubernetes-informer-example")
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
                "example-informer",
                namespace,
                getServiceSpecTypeModel("LoadBalancer", [getServicePortModel(8080, 8080)], ["app": "example-informer"]))

        kubectlPortForward = portForwardService("example-informer", namespace, 8080, 8889)
    }

    void "test all"() {
        expect:
        testClient.all().metadata.name.findAll { !it.startsWith("default-token") }.size() == 3
        testClient.secret("test-secret")
        testClient.secret("test-secret").data.containsKey("username")
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
        createSecret(secretName, namespace, ["foo": "bar".bytes])

        then:
        conditions.eventually {
            testClient.all().metadata.name.findAll { !it.startsWith("default-token") }.size() == 4
            testClient.secret(secretName)
        }

        when:
        deleteSecret(secretName, namespace)

        then:
        conditions.eventually {
            testClient.all().metadata.name.findAll { !it.startsWith("default-token") }.size() == 3
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
