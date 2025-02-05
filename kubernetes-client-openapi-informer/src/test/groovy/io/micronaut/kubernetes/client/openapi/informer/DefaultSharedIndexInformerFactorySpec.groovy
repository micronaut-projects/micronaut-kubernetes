package io.micronaut.kubernetes.client.openapi.informer

import io.micronaut.context.annotation.Property
import io.micronaut.kubernetes.client.openapi.model.V1Secret
import io.micronaut.test.extensions.spock.annotation.MicronautTest
import jakarta.inject.Inject
import spock.lang.Specification

@MicronautTest
@Property(name = "kubernetes.client.informer.enabled", value = "false")
class DefaultSharedIndexInformerFactorySpec extends Specification {

    @Inject
    SharedIndexInformerFactory sharedIndexInformerFactory

    def 'test informer created on namespace and global level'() {
        when:
        def informer1 = sharedIndexInformerFactory.sharedIndexInformerFor(V1Secret.class, null)
        def informer2 = sharedIndexInformerFactory.sharedIndexInformerFor(V1Secret.class, "test-namespace")
        def informer3 = sharedIndexInformerFactory.getExistingSharedIndexInformer(V1Secret.class, null)
        def informer4 = sharedIndexInformerFactory.getExistingSharedIndexInformer(V1Secret.class, "test-namespace")

        then:
        informer1 != null
        informer1 == informer3
        informer3.getIndexer() != null
        informer2 != null
        informer2 == informer4
        informer4.getIndexer() != null
    }

    def 'test informer created for each namespace'() {
        when:
        def informers = sharedIndexInformerFactory.sharedIndexInformersFor(V1Secret.class, Arrays.asList("test1", "test2"), null, false, 0)
        def informer1 = sharedIndexInformerFactory.getExistingSharedIndexInformer(V1Secret.class, "test1")
        def informer2 = sharedIndexInformerFactory.getExistingSharedIndexInformer(V1Secret.class, "test2")

        then:
        informers != null
        informers.get(0) == informer1
        informers.get(1) == informer2
    }

    def 'test api type class not provided'() {
        when:
        sharedIndexInformerFactory.sharedIndexInformerFor(null, null)

        then:
        def error = thrown(IllegalArgumentException)
        error.message == "The apiTypeClass must be provided"
    }

    def 'test list of namespaces not provided'() {
        when:
        sharedIndexInformerFactory.sharedIndexInformersFor(V1Secret.class, null, null, false, 0)

        then:
        def error = thrown(IllegalArgumentException)
        error.message == "The list of namespaces must be provided"
    }

    def 'test namespaces contain empty string'() {
        when:
        sharedIndexInformerFactory.sharedIndexInformersFor(V1Secret.class, Arrays.asList("", "test2"), null, false, 0)

        then:
        def error = thrown(IllegalArgumentException)
        error.message == "The namespaces list must not contain empty strings"
    }

    def 'test informer has been already created'() {
        when:
        sharedIndexInformerFactory.sharedIndexInformerFor(V1Secret.class, null, "test1")
        sharedIndexInformerFactory.sharedIndexInformerFor(V1Secret.class, null, "test2")

        then:
        def error = thrown(IllegalStateException)
        error.message == "Informer has been already created for apiTypeClass=io.micronaut.kubernetes.client.openapi.model.V1Secret and namespace=null"
    }
}
