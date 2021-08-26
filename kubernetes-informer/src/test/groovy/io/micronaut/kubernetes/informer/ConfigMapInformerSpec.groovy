package io.micronaut.kubernetes.informer

import io.fabric8.kubernetes.api.model.ConfigMap
import io.fabric8.kubernetes.api.model.ConfigMapList
import io.micronaut.context.ApplicationContext
import io.micronaut.context.annotation.Property
import io.micronaut.context.env.Environment
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
class ConfigMapInformerSpec extends KubernetesSpecification {

    @Shared
    @Inject
    ApplicationContext applicationContext

    @Override
    def setupFixture(String namespace) {
        createNamespaceSafe(namespace)
    }

    def "config map informer is notified"() {
        given:
        ConfigMapInformer resourceHandler = applicationContext.getBean(ConfigMapInformer)

        expect:
        resourceHandler.updated.isEmpty()
        resourceHandler.deleted.isEmpty()

        when:
        operations.createConfigMap("map1", namespace, ["foo": "bar"])

        then:
        new PollingConditions().within(5) {
            assert resourceHandler.added.size() == 2
        }

        when:
        ConfigMap cm = operations.getConfigMap("map1", namespace)
        cm.data.put("ping", "pong")
        operations.modifyConfigMap("map1", namespace, cm.data)

        then:
        new PollingConditions().within(5) {
            assert resourceHandler.updated.size() == 1
        }

        when:
        operations.deleteConfigMap("map1", namespace)

        then:
        new PollingConditions().within(5) {
            assert resourceHandler.deleted.size() == 1
        }
    }
}
