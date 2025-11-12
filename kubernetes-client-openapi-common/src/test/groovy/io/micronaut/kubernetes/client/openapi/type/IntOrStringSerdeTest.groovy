package io.micronaut.kubernetes.client.openapi.type

import io.micronaut.json.JsonMapper
import io.micronaut.kubernetes.client.openapi.model.V1ServicePort
import io.micronaut.kubernetes.client.openapi.model.V1ServiceSpec
import io.micronaut.test.extensions.spock.annotation.MicronautTest
import jakarta.inject.Inject
import spock.lang.Specification

@MicronautTest
class IntOrStringSerdeTest extends Specification {

    @Inject
    JsonMapper jsonMapper

    def 'deserialize when target port is int'() {
        given:
        String servicePortJson = '{"port":8000, "targetPort":8080}'

        when:
        V1ServicePort servicePort = jsonMapper.readValue(servicePortJson, V1ServicePort)

        then:
        servicePort.getTargetPort().getIntValue() == 8080
        servicePort.getPort() == 8000
    }

    def 'deserialize when target port is string'() {
        given:
        String servicePortJson = '{"port":8000, "targetPort":"8080"}'

        when:
        V1ServicePort servicePort = jsonMapper.readValue(servicePortJson, V1ServicePort)

        then:
        servicePort.getTargetPort().getStrValue() == "8080"
        servicePort.getPort() == 8000
    }

    def 'deserialize when target port is not provided'() {
        given:
        String servicePortJson = '{"port":8000}'

        when:
        V1ServicePort servicePort = jsonMapper.readValue(servicePortJson, V1ServicePort)

        then:
        !servicePort.getTargetPort()
        servicePort.getPort() == 8000
    }

    def 'deserialize when multiple target ports are provided'() {
        given:
        String serviceSpecJson = '{"clusterIP":"100.100.0.0","ports":[{"port":8001,"targetPort":8081},{"port":8002,"targetPort":"8082"}],"selector":{"test1":"test2"}}'

        when:
        V1ServiceSpec serviceSpec = jsonMapper.readValue(serviceSpecJson, V1ServiceSpec)

        then:
        serviceSpec.getClusterIP() == "100.100.0.0"
        serviceSpec.getPorts().size() == 2
        serviceSpec.getPorts().get(0).getTargetPort().getIntValue() == 8081
        serviceSpec.getPorts().get(1).getTargetPort().getStrValue() == "8082"
        serviceSpec.getSelector().size() == 1
        serviceSpec.getSelector().get("test1") == "test2"
    }

    def 'serialize when target port is int'() {
        given:
        V1ServicePort servicePort = new V1ServicePort(8000).targetPort(new IntOrString(8080))

        when:
        String servicePortJson = jsonMapper.writeValueAsString(servicePort)

        then:
        servicePortJson == '{"port":8000,"targetPort":8080}'
    }

    def 'serialize when target port is string'() {
        given:
        V1ServicePort servicePort = new V1ServicePort(8000).targetPort(new IntOrString("8080"))

        when:
        String servicePortJson = jsonMapper.writeValueAsString(servicePort)

        then:
        servicePortJson == '{"port":8000,"targetPort":"8080"}'
    }

    def 'serialize when target port is not provided'() {
        given:
        V1ServicePort servicePort = new V1ServicePort(8000)

        when:
        String servicePortJson = jsonMapper.writeValueAsString(servicePort)

        then:
        servicePortJson == '{"port":8000}'
    }

    def 'serialize when multiple target ports are provided'() {
        given:
        V1ServicePort servicePort1 = new V1ServicePort(8001).targetPort(new IntOrString(8081))
        V1ServicePort servicePort2 = new V1ServicePort(8002).targetPort(new IntOrString("8082"))
        V1ServiceSpec serviceSpec = new V1ServiceSpec().clusterIP("100.100.0.0").ports([servicePort1, servicePort2]).selector(["test1":"test2"])

        when:
        String serviceSpecJson = jsonMapper.writeValueAsString(serviceSpec)

        then:
        serviceSpecJson == '{"clusterIP":"100.100.0.0","ports":[{"port":8001,"targetPort":8081},{"port":8002,"targetPort":"8082"}],"selector":{"test1":"test2"}}'
    }
}
