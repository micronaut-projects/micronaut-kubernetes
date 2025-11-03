package io.micronaut.kubernetes.test

import org.yaml.snakeyaml.Yaml
import spock.lang.Requires
import spock.lang.Specification

import java.nio.file.Paths

import static io.micronaut.kubernetes.test.KubernetesOperations.createConfigMap
import static io.micronaut.kubernetes.test.KubernetesOperations.createConfigMapFromFile
import static io.micronaut.kubernetes.test.KubernetesOperations.createDeploymentFromFile
import static io.micronaut.kubernetes.test.KubernetesOperations.createNamespace
import static io.micronaut.kubernetes.test.KubernetesOperations.createRole
import static io.micronaut.kubernetes.test.KubernetesOperations.createRoleBinding
import static io.micronaut.kubernetes.test.KubernetesOperations.createSecret
import static io.micronaut.kubernetes.test.KubernetesOperations.deleteNamespace
import static io.micronaut.kubernetes.test.KubernetesOperations.getConfigMap
import static io.micronaut.kubernetes.test.KubernetesOperations.getDeployment
import static io.micronaut.kubernetes.test.KubernetesOperations.getNamespace
import static io.micronaut.kubernetes.test.KubernetesOperations.getSecret

@Requires({ TestUtils.kubernetesApiAvailable() })
class KubernetesOperationsSpec extends Specification{

    def setupSpec() {
        createNamespace("test-namespace")
        getNamespace("test-namespace")
    }

    def cleanupSpec() {
        if (getNamespace("test-namespace") != null) {
            deleteNamespace("test-namespace")
        }
    }

    def "it creates rolebinding in namespace"(){
        when:
        def role = createRole("discoverer", "test-namespace")

        then:
        role.metadata.getCreationTimestamp()

        when:
        def roleBinding = createRoleBinding("discoverer-role", "test-namespace","discoverer")

        then:
        roleBinding.metadata.getCreationTimestamp()
    }

    def "it creates secret from literals"(){
        when:
        def secret = createSecret("secret", "test-namespace", ['key': "value".bytes])

        then:
        secret.getMetadata().getCreationTimestamp()

        when:
        secret = getSecret("secret", "test-namespace")

        then:
        secret.getData().get("key") == "value".bytes
    }

    def "it creates config map from data"(){
        given:
        def data = ["key": "value"]

        when:
        def map = createConfigMap("example-map", "test-namespace", data)

        then:
        map.getMetadata().getCreationTimestamp()

        when:
        map = getConfigMap("example-map", "test-namespace")

        then:
        map.getData().size()
        map.getData().get("key") == "value"
    }

    def "it create config map from yaml file"(){
        given:
        def filePath = Paths.get("src","test","resources", "k8s", "game.yml")

        when:
        def cm = createConfigMapFromFile("config-map-yaml", "test-namespace", filePath.toUri().toURL())

        then:
        cm.getMetadata().getCreationTimestamp()

        when:
        cm = getConfigMap("config-map-yaml", "test-namespace")

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

    def "it creates deployment from file"(){
        given:
        def path = Paths.get("src","test","resources", "k8s", "deployment.yml")

        when:
        def deployment = createDeploymentFromFile(path.toUri().toURL())

        then:
        deployment.metadata.creationTimestamp

        when:
        deployment = getDeployment(deployment.metadata.name, deployment.metadata.namespace)

        then:
        deployment.status.availableReplicas == 1
    }

    def "it creates deployment with overriden name and namespace from file"(){
        given:
        def path = Paths.get("src","test","resources", "k8s", "deployment.yml")
        createNamespace("other-namespace")

        when:
        def deployment = createDeploymentFromFile(path.toUri().toURL(), "other-name", "other-namespace")

        then:
        deployment.metadata.creationTimestamp
        deployment.status.availableReplicas == 1

        when:
        deployment = getDeployment("other-name", "other-namespace")

        then:
        deployment

        cleanup:
        deleteNamespace("other-namespace")
    }
}
