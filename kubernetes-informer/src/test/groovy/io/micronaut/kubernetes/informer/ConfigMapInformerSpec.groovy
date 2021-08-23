package io.micronaut.kubernetes.informer

import io.micronaut.context.ApplicationContext
import io.micronaut.test.extensions.spock.annotation.MicronautTest
import jakarta.inject.Inject
import spock.lang.Shared
import spock.lang.Specification


@MicronautTest
class ConfigMapInformerSpec extends Specification{

    @Shared
    @Inject
    ApplicationContext applicationContext

    def "informer is registered"(){
        expect:
        applicationContext.containsBean(SharedInformerFactoryFactory)
        applicationContext.getBean(SharedInformerFactoryFactory)
        applicationContext.containsBean(SharedInformerListener)
        applicationContext.getBean(SharedInformerListener)
        applicationContext.getBean(ConfigMapInformer)
        applicationContext.getBean(DefaultSharedInformerFactory)
    }

}
