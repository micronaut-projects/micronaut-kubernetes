package io.micronaut.kubernetes.client

import io.kubernetes.client.openapi.ApiClient
import io.kubernetes.client.openapi.apis.CoreV1Api
import io.kubernetes.client.openapi.apis.NodeV1alpha1Api
import io.micronaut.context.ApplicationContext
import io.micronaut.context.annotation.Requires
import io.micronaut.test.extensions.spock.annotation.MicronautTest
import spock.lang.Specification

import javax.inject.Inject

@MicronautTest
@Requires(beans = ApiClient.class)
class ApisSpec extends Specification {

    @Inject
    ApplicationContext context

    def "it creates core api"(){
        expect:
        context.containsBean(CoreV1Api)
    }

    def "it creates node api"(){
        expect:
        context.containsBean(NodeV1alpha1Api)
    }
}
