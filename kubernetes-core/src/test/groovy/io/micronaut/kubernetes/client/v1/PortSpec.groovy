package io.micronaut.kubernetes.client.v1

import spock.lang.Specification

class PortSpec extends Specification {

    void "test isSecure()"(String portName, int portNumber, boolean expectedSecure) {
        given:
        Port port = new Port().tap {
            name = portName
            port = portNumber
        }

        when:
        boolean isSecure = port.isSecure()

        then:
        isSecure == expectedSecure

        where:
        portName    | portNumber    | expectedSecure
        null        | 1234          | false
        "foo"       | 1234          | false
        "https"     | 1234          | true
        null        | 443           | true
        "foo"       | 443           | true
        "https"     | 443           | true
        null        | 8443          | true
        "foo"       | 8443          | true
        "https"     | 8443          | true
    }

}
