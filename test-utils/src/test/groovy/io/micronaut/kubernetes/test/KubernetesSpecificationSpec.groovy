package io.micronaut.kubernetes.test

import io.micronaut.context.env.Environment
import io.micronaut.test.extensions.spock.annotation.MicronautTest
import spock.lang.Requires

import static io.micronaut.kubernetes.test.KubernetesOperations.getDeployment

@MicronautTest(environments = [Environment.KUBERNETES])
@Requires({ TestUtils.kubernetesApiAvailable() })
class KubernetesSpecificationSpec extends KubernetesSpecification{

    def "it created example service"(){
        expect:
        def deployment = getDeployment("example-service", namespace)
        deployment.status.availableReplicas == 2
    }

    def "it created example client"(){
        expect:
        def deployment = getDeployment("example-client", namespace)
        deployment.status.availableReplicas == 1
    }

    def "it created secure deployment"(){
        expect:
        def deployment = getDeployment("secure-deployment", namespace)
        deployment.status.availableReplicas == 1
    }
}
