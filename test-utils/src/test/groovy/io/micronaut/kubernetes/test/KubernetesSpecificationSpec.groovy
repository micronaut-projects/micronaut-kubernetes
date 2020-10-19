package io.micronaut.kubernetes.test

class KubernetesSpecificationSpec extends KubernetesSpecification{

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
