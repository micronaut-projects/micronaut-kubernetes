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
@Property(name = "kubernetes.client.namespace", value = "micronaut-informer-labeled")
@Property(name = "spec.reuseNamespace", value = "false")
@Property(name = "spec.name", value = "ConfigMapLabelSelectorInformerSpec")
class ConfigMapLabelSelectorInformerSpec extends KubernetesSpecification {

    @Shared
    @Inject
    ApplicationContext applicationContext

    @Override
    def setupFixture(String namespace) {
        createNamespaceSafe(namespace)
    }

    def "config map informer with label selector is notified"() {
        given:
        ConfigMapLabelSelectorInformer resourceHandler = applicationContext.getBean(ConfigMapLabelSelectorInformer)

        expect:
        resourceHandler.added.isEmpty()
        resourceHandler.updated.isEmpty()
        resourceHandler.deleted.isEmpty()

        when:
        operations.createConfigMap("map1", namespace, ["foo": "bar"])
        sleep(3000)

        then:
        resourceHandler.added.size() == 0

        when:
        operations.createConfigMap("map2", namespace, ["foo": "bar"], ["environment": "test"])

        then:
        new PollingConditions().within(120) {
            assert resourceHandler.added.size() == 1
        }

        when:
        ConfigMap cm = operations.getConfigMap("map2", namespace)
        cm.data.put("ping", "pong")
        operations.modifyConfigMap(cm)

        then:
        new PollingConditions().within(5) {
            assert resourceHandler.updated.size() == 1
            assert resourceHandler.updated.first().getData().containsKey("ping")
        }
    }
}
