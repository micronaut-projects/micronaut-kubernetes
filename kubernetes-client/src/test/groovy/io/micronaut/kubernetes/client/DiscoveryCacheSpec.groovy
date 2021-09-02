package io.micronaut.kubernetes.client

import io.kubernetes.client.openapi.models.V1ClusterRole
import io.kubernetes.client.openapi.models.V1ConfigMap
import io.micronaut.context.annotation.Property
import io.micronaut.kubernetes.test.KubernetesSpecification
import io.micronaut.kubernetes.test.TestUtils
import io.micronaut.test.extensions.spock.annotation.MicronautTest
import jakarta.inject.Inject
import spock.lang.Requires
import spock.lang.Shared

@MicronautTest
@Requires({ TestUtils.kubernetesApiAvailable() })
@Property(name = "kubernetes.client.namespace", value = "default")
class DiscoveryCacheSpec extends KubernetesSpecification {

    @Inject
    @Shared
    DiscoveryCache discoveryCache

    def setupFixture(){
        // no-op
    }

    def "it resolves the api resources"() {
        expect:
        with(discoveryCache.findAll()){
            !it.isEmpty()
            it.size() > 0
            it.stream().filter(r -> r.kind == "ConfigMap").any()
        }
    }

    def "it resolves specific resources"() {
        expect:
        with(discoveryCache.find(V1ConfigMap)){
            it.isPresent()
            it.get().kind == "ConfigMap"
            it.get().preferredVersion == "v1"
            it.get().resourcePlural == "configmaps"
            it.get().group == ""
        }

        with(discoveryCache.find(V1ClusterRole)){
            it.isPresent()
            it.get().kind == "ClusterRole"
            it.get().preferredVersion == "v1"
            it.get().resourcePlural == "clusterroles"
            it.get().group == "rbac.authorization.k8s.io"
        }
    }
}
