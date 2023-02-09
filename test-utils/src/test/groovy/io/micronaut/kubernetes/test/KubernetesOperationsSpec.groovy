package io.micronaut.kubernetes.test

import io.fabric8.kubernetes.client.KubernetesClientException
import io.micronaut.context.exceptions.ConfigurationException
import org.yaml.snakeyaml.Yaml
import spock.lang.Requires
import spock.lang.Shared
import spock.lang.Specification

import java.nio.file.Paths

@Requires({ TestUtils.kubernetesApiAvailable() })
class KubernetesOperationsSpec extends Specification {

    @Shared
    KubernetesOperations operations = new KubernetesOperations()

    def setupSpec() {
        operations.createNamespace("test-namespace")
        operations.getNamespace("test-namespace")
    }

    def cleanupSpec() {
        if (operations.getNamespace("test-namespace") != null) {
            operations.deleteNamespace("test-namespace")
        }
    }

    def "it creates rolebinding in namespace"() {
        when:
        def role = operations.createRole("discoverer", "test-namespace")

        then:
        role.metadata.getCreationTimestamp()

        when:
        def roleBinding = operations.createRoleBinding("discoverer-role", "test-namespace","discoverer")

        then:
        roleBinding.metadata.getCreationTimestamp()
    }

    def "it creates secret from literals"() {
        given:
        String password64 = Base64.encoder.encodeToString("value".bytes)

        when:
        def secret = operations.createSecret("secret", "test-namespace", ['key': password64])

        then:
        secret.getMetadata().getCreationTimestamp()

        when:
        secret = operations.getSecret("secret", "test-namespace")

        then:
        Base64.decoder.decode(secret.getData().get("key")) == "value".bytes
    }

    def "it creates config map from data"() {
        given:
        def data = ["key": "value"]

        when:
        def map = operations.createConfigMap("example-map", "test-namespace", data)

        then:
        map.getMetadata().getCreationTimestamp()

        when:
        map = operations.getConfigMap("example-map", "test-namespace")

        then:
        map.getData().size()
        map.getData().get("key") == "value"
    }

    def "it create config map from yaml file"() {
        given:
        def filePath = Paths.get("src","test","resources", "k8s", "game.yml")

        when:
        def cm = operations.createConfigMapFromFile("config-map-yaml", "test-namespace", filePath.toUri().toURL())

        then:
        cm.getMetadata().getCreationTimestamp()

        when:
        cm = operations.getConfigMap("config-map-yaml", "test-namespace")

        then:
        cm.getData().containsKey("game.yml")

        when:
        Yaml yaml = new Yaml()
        def files = new ArrayList<Map<String,Object>>()
        def gameYaml = yaml.loadAll(cm.getData().get("game.yml"))
        gameYaml.forEach(files::add)

        then:
        files[0]["enemies"] == "aliens"
        files[1]["enemies"]["cheat"]["level"] == "noGoodRotten"
        files[2]["secret"]["code"]["passphrase"] == "UUDDLRLRBABAS"
    }

    def "it creates deployment from file"() {
        given:
        def path = Paths.get("src","test","resources", "k8s", "deployment.yml")

        when:
        def deployment = operations.createDeploymentFromFile(path.toUri().toURL())

        then:
        deployment.metadata.creationTimestamp

        when:
        deployment = operations.getDeployment(deployment.metadata.name, deployment.metadata.namespace)

        then:
        deployment.status.availableReplicas == 1
    }

    def "it creates deployment with overriden name and namespace from file"() {
        given:
        def path = Paths.get("src","test","resources", "k8s", "deployment.yml")
        operations.createNamespace("other-namespace")

        when:
        operations.createDeploymentFromFile(path.toUri().toURL(), "other-name", "other-namespace")

        then:
        def e = thrown(KubernetesClientException)
        e.message.contains("Namespace mismatch")

        cleanup:
        operations.deleteNamespace("other-namespace")
    }
}
