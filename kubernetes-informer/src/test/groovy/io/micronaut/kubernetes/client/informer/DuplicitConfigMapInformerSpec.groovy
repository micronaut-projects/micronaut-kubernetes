package io.micronaut.kubernetes.client.informer

import io.fabric8.kubernetes.api.model.ConfigMap
import io.kubernetes.client.openapi.models.V1ConfigMap
import io.micronaut.context.ApplicationContext
import io.micronaut.context.annotation.Property
import io.micronaut.context.env.Environment
import io.micronaut.inject.BeanDefinition
import io.micronaut.inject.qualifiers.Qualifiers
import io.micronaut.kubernetes.test.KubernetesSpecification
import io.micronaut.kubernetes.test.TestUtils
import io.micronaut.test.extensions.spock.annotation.MicronautTest
import jakarta.inject.Inject
import spock.lang.Requires
import spock.lang.Shared
import spock.util.concurrent.PollingConditions


@MicronautTest(environments = [Environment.KUBERNETES])
@Requires({ TestUtils.kubernetesApiAvailable() })
@Property(name = "kubernetes.client.namespace", value = "micronaut-informer")
@Property(name = "spec.reuseNamespace", value = "false")
@Property(name = "spec.name", value = "ConfigMapInformerSpec")
class DuplicitConfigMapInformerSpec extends KubernetesSpecification {

    @Shared
    @Inject
    ApplicationContext applicationContext

    @Override
    def setupFixture(String namespace) {
        createNamespaceSafe(namespace)
    }

    def "resource handlers with same type and namespace are notified"() {
        given:
        DuplicitConfigMapInformer informer1 = applicationContext.getBean(DuplicitConfigMapInformer.class)
        ConfigMapInformer informer2 = applicationContext.getBean(ConfigMapInformer.class)

        expect:
        informer1.updated.isEmpty() && informer1.deleted.isEmpty()
        informer2.updated.isEmpty() && informer2.deleted.isEmpty()

        when:
        operations.createConfigMap("map1", namespace, ["foo": "bar"])

        then:
        new PollingConditions().within(5) {
            assert informer1.added.stream().filter(cm -> cm.metadata.name == "map1")
                    .findFirst().isPresent()
            assert informer2.added.stream().filter(cm -> cm.metadata.name == "map1")
                    .findFirst().isPresent()
        }

        when:
        ConfigMap cm = operations.getConfigMap("map1", namespace)
        cm.data.put("ping", "pong")
        operations.modifyConfigMap("map1", namespace, cm.data)

        then:
        new PollingConditions().within(5) {
            assert informer1.updated.size() == 1
            assert informer2.updated.size() == 1
        }


        when:
        operations.deleteConfigMap("map1", namespace)

        then:
        new PollingConditions().within(5) {
            assert informer1.deleted.size() == 1
            assert informer2.deleted.size() == 1
        }
    }
}
