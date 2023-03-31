package io.micronaut.kubernetes.client.operator.controller

import io.kubernetes.client.extended.controller.Controller
import io.micronaut.context.env.Environment
import io.micronaut.kubernetes.client.operator.ControllerConfiguration
import io.micronaut.test.extensions.spock.annotation.MicronautTest
import jakarta.inject.Inject
import spock.lang.Specification

@MicronautTest(environments = [Environment.KUBERNETES])
class DefaultControllerManagerBuilderSpec extends Specification{

    @Inject
    DefaultControllerManagerBuilder builder;

    def "it builds manager builder"(){
        given:
        ControllerConfiguration operator = Stub()
        Controller controller1 = Mock(Controller)
        Controller controller2 = Mock(Controller)

        when:
        def controllerManager = builder.build(operator, [controller1, controller2])

        then:
        controllerManager

        when:
        controllerManager.run()

        then:
        1 * controller1.run()
        1 * controller2.run()

        when:
        controllerManager.shutdown()

        then:
        1 * controller1.shutdown()
        1 * controller2.shutdown()
    }
}
