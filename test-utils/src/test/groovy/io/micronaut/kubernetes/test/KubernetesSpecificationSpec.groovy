package io.micronaut.kubernetes.test

import io.micronaut.context.annotation.Property
import io.micronaut.context.env.Environment
import io.micronaut.test.extensions.spock.annotation.MicronautTest
import spock.lang.Requires

@MicronautTest(environments = [Environment.KUBERNETES])
@Requires({ TestUtils.kubernetesApiAvailable() })
class KubernetesSpecificationSpec extends KubernetesSpecification {

    def "it created example service"(){
        expect:
        def deployment = operations.getDeployment("example-service", namespace)
        deployment.status.availableReplicas == 2
    }

    def "it created example client"(){
        expect:
        def deployment = operations.getDeployment("example-client", namespace)
        deployment.status.availableReplicas == 1
    }

    def "it created secure deployment"(){
        expect:
        def deployment = operations.getDeployment("secure-deployment", namespace)
        deployment.status.availableReplicas == 1
    }
}
