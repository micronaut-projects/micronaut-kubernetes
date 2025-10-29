package io.micronaut.kubernetes.client.informer

import io.kubernetes.client.openapi.models.V1ConfigMap
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

import static io.micronaut.kubernetes.test.KubernetesOperations.createConfigMap
import static io.micronaut.kubernetes.test.KubernetesOperations.deleteConfigMap
import static io.micronaut.kubernetes.test.KubernetesOperations.modifyConfigMap

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
        def configMap = createConfigMap("map1", namespace, ["foo": "bar"])

        then:
        new PollingConditions().within(5) {
            assert resourceHandler.added.stream().filter(cm -> cm.metadata.name == "map1")
                    .findFirst().isPresent()
        }

        when:
        configMap.data.put("ping", "pong")
        modifyConfigMap(configMap)

        then:
        new PollingConditions().within(5) {
            assert resourceHandler.updated.size() == 1
        }

        when:
        deleteConfigMap("map1", namespace)

        then:
        new PollingConditions().within(5) {
            assert resourceHandler.deleted.size() == 1
        }
    }

    def "it can access config map local cache"() {
        given:
        SharedInformerCache informerCache = applicationContext.getBean(SharedInformerCache)

        when:
        createConfigMap("map1", namespace, ["foo": "bar"])

        then:
        new PollingConditions().eventually {
            List<V1ConfigMap> list = informerCache.getConfigMaps(namespace)
            list != null
            !list.isEmpty()
            list.stream().filter(cm -> cm.metadata.name == "map1").any()
        }
    }
}
