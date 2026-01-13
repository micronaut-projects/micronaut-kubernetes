package io.micronaut.kubernetes.discovery

import groovy.util.logging.Slf4j
import io.micronaut.core.async.publisher.Publishers
import io.micronaut.discovery.ServiceInstance
import io.micronaut.kubernetes.KubernetesConfiguration
import io.micronaut.kubernetes.KubernetesConfiguration.KubernetesDiscoveryConfiguration
import org.reactivestreams.Publisher
import spock.lang.Specification

import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

@Slf4j('LOG')
class KubernetesDiscoveryClientRaceCondSpec extends Specification {

    def 'race condition check'() {
        given:
        def executor = Executors.newFixedThreadPool(100)
        def concurrentModificationExceptionThrown = new AtomicBoolean(false)
        def kubernetesConfiguration = new KubernetesConfiguration(() -> "namespace-name")
        kubernetesConfiguration.setDiscovery(new KubernetesDiscoveryConfiguration())
        def instanceProvider = new KubernetesServiceInstanceProvider() {
            @Override
            String getMode() {
                return KubernetesDiscoveryConfiguration.DEFAULT_MODE
            }

            @Override
            Publisher<List<ServiceInstance>> getInstances(KubernetesServiceConfiguration serviceConfiguration) {
                return Publishers.just(List.of())
            }

            @Override
            Publisher<String> getServiceIds(String namespace) {
                return Publishers.just('none')
            }
        }
        def kubernetesDiscoveryClient = new KubernetesDiscoveryClient(null, kubernetesConfiguration, kubernetesConfiguration.getDiscovery(), [], [instanceProvider], null)

        when:
        1000.times {
            def serviceId = "service-id-${it / 10}"
            executor.execute {
                try {
                    kubernetesDiscoveryClient.getInstances(serviceId)
                } catch (ConcurrentModificationException e) {
                    LOG.info('Caught ConcurrentModificationException for service {}', serviceId, e)
                    concurrentModificationExceptionThrown.set(true)
                }
            }
        }
        executor.shutdown()
        executor.awaitTermination(1, TimeUnit.SECONDS)

        then:
        !concurrentModificationExceptionThrown.get()
    }
}
