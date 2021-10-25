package io.micronaut.kubernetes.discovery.informer

import io.micronaut.kubernetes.KubernetesConfiguration
import io.micronaut.kubernetes.discovery.KubernetesServiceConfiguration
import io.micronaut.kubernetes.discovery.KubernetesServiceInstanceProvider
import spock.lang.Shared
import spock.lang.Specification


class InstanceProviderInformerNamespaceResolverSpec extends Specification {

    @Shared
    KubernetesConfiguration kubernetesConfiguration

    @Shared
    KubernetesConfiguration.KubernetesDiscoveryConfiguration discoveryConfiguration

    @Shared
    KubernetesServiceInstanceProvider provider

    def setup() {
        kubernetesConfiguration = Stub()
        kubernetesConfiguration.getNamespace() >> "micronaut-kubernetes"

        discoveryConfiguration = Stub()
        discoveryConfiguration.getMode() >> "mode"

        provider = Stub()
        provider.getMode() >> "mode"
    }

    def "it returns application namespace for default mode"() {
        given:
        def resolver = new InstanceProviderInformerNamespaceResolver(kubernetesConfiguration,
                discoveryConfiguration, new ArrayList<KubernetesServiceConfiguration>())

        expect:
        resolver.resolveInformerNamespaces(provider) == ["micronaut-kubernetes"].toSet()
    }

    def "it returns namespace of manually configured service for the same provider mode"() {
        given:
        def resolver = new InstanceProviderInformerNamespaceResolver(kubernetesConfiguration, discoveryConfiguration,
                [new KubernetesServiceConfiguration("serviceId", "name", "manual-namespace", "mode", "", true),])

        expect:
        resolver.resolveInformerNamespaces(provider).contains("manual-namespace")
    }

    def "it does not return namespace of manually configured service for other provider"() {
        given:
        def resolver = new InstanceProviderInformerNamespaceResolver(kubernetesConfiguration, discoveryConfiguration,
                [new KubernetesServiceConfiguration("serviceId", "name", "manual-namespace", "other-mode", "", true),])

        expect:
        !resolver.resolveInformerNamespaces(provider).contains("manual-namespace")
    }
}
