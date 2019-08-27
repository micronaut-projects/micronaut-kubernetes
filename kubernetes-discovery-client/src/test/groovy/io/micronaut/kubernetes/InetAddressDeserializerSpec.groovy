package io.micronaut.kubernetes

import com.fasterxml.jackson.databind.ObjectMapper
import io.micronaut.kubernetes.client.v1.services.ServiceSpec
import io.micronaut.test.annotation.MicronautTest
import spock.lang.Specification

import javax.inject.Inject

@MicronautTest
class InetAddressDeserializerSpec extends Specification {

    @Inject ObjectMapper mapper

    void "it can deserialize cluster IPs"() {
        given:
        String json = '''{
    "ports": [
      {
        "protocol": "TCP",
        "port": 8082,
        "targetPort": 8082,
        "nodePort": 32402
      }
    ],
    "selector": {
      "app": "example-client"
    },
    "clusterIP": "10.103.102.160",
    "type": "LoadBalancer",
    "sessionAffinity": "None",
    "externalTrafficPolicy": "Cluster"
  }'''

        when:
        ServiceSpec serviceSpec = mapper.readValue(json, ServiceSpec.class)

        then:
        serviceSpec.clusterIp == InetAddress.getByName("10.103.102.160")
    }

    void "it handles clusterIPs of 'None'"() {
        given:
        String json = '''{
    "ports": [
      {
        "protocol": "TCP",
        "port": 8082,
        "targetPort": 8082,
        "nodePort": 32402
      }
    ],
    "selector": {
      "app": "example-client"
    },
    "clusterIP": "None",
    "type": "LoadBalancer",
    "sessionAffinity": "None",
    "externalTrafficPolicy": "Cluster"
  }'''
        when:
        ServiceSpec serviceSpec = mapper.readValue(json, ServiceSpec.class)

        then:
        serviceSpec.clusterIp == null
    }


}
