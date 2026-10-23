package io.micronaut.kubernetes.client.openapi.discovery.provider

import io.micronaut.kubernetes.client.openapi.model.V1ObjectMeta
import spock.lang.Specification

class KubernetesDiscoveryUtilsSpec extends Specification {

    void "builds a usable service URI for #address with port #portNumber"() {
        when:
        def instance = KubernetesDiscoveryUtils.buildServiceInstance(
            'orders', portName, portNumber, address, new V1ObjectMeta().labels(labels))
        def uri = instance.URI

        then:
        uri.toString() == expectedUri
        uri.host == expectedHost
        uri.port == (portNumber == null ? -1 : portNumber)
        instance.id == 'orders'

        where:
        address          | portName | portNumber | labels           | expectedUri                    | expectedHost
        'fd00::123'      | 'http'   | 8080       | [:]              | 'http://[fd00::123]:8080'       | '[fd00::123]'
        'fd00::123'      | 'https'  | 8443       | [:]              | 'https://[fd00::123]:8443'      | '[fd00::123]'
        'fd00::123'      | null     | null       | [:]              | 'http://[fd00::123]'            | '[fd00::123]'
        'fd00::123'      | 'http'   | 8080       | [secure: 'true'] | 'https://[fd00::123]:8080'      | '[fd00::123]'
        '10.0.0.1'       | 'http'   | 8080       | [:]              | 'http://10.0.0.1:8080'          | '10.0.0.1'
        'orders.example' | 'http'   | 8080       | [:]              | 'http://orders.example:8080'    | 'orders.example'
        'orders.example' | null     | null       | [:]              | 'http://orders.example'         | 'orders.example'
    }
}
