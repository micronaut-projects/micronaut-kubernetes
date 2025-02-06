package io.micronaut.kubernetes.client.openapi.utils

import io.micronaut.kubernetes.client.openapi.model.CoreV1EndpointPort
import io.micronaut.kubernetes.client.openapi.model.V1EndpointAddress
import io.micronaut.kubernetes.client.openapi.model.V1EndpointSubset
import io.micronaut.kubernetes.client.openapi.model.V1Endpoints
import io.micronaut.kubernetes.client.openapi.model.V1Namespace
import io.micronaut.kubernetes.client.openapi.model.V1ObjectMeta
import io.micronaut.kubernetes.client.openapi.model.V1Service
import io.micronaut.kubernetes.client.openapi.model.V1ServicePort
import io.micronaut.kubernetes.client.openapi.model.V1ServiceSpec

class ModelUtils {

    static V1ObjectMeta getObjectMeta(String name) {
        return getObjectMeta(name, [:])
    }

    static V1ObjectMeta getObjectMeta(String name, Map<String, String> labels) {
        V1ObjectMeta objectMeta = new V1ObjectMeta()
        objectMeta.name(name)
        objectMeta.labels(labels)
        return objectMeta
    }

    static V1Namespace getNamespace(String name) {
        V1Namespace namespace = new V1Namespace()
        namespace.kind('Namespace')
        namespace.apiVersion('v1')
        namespace.metadata(getObjectMeta(name))
        return namespace
    }

    static V1ServicePort getServicePort(int port, String name) {
        V1ServicePort servicePort = new V1ServicePort(port)
        servicePort.setName(name)
        servicePort.setTargetPort(name)
        return servicePort
    }

    static V1ServiceSpec getServiceSpec(String clusterIP, List<V1ServicePort> ports) {
        V1ServiceSpec serviceSpec = new V1ServiceSpec()
        serviceSpec.clusterIP(clusterIP)
        serviceSpec.ports(ports)
        return serviceSpec
    }

    static V1ServiceSpec getServiceSpec(String externalName) {
        V1ServiceSpec serviceSpec = new V1ServiceSpec()
        serviceSpec.type("ExternalName")
        serviceSpec.externalName(externalName)
        return serviceSpec
    }

    static V1Service getService(String name, V1ServiceSpec spec) {
        return getService(name, spec, [:])
    }

    static V1Service getService(String name, V1ServiceSpec spec, Map<String, String> labels) {
        V1Service service = new V1Service()
        service.kind('Service')
        service.apiVersion('v1')
        service.metadata(getObjectMeta(name, labels))
        service.spec(spec)
        return service
    }

    static CoreV1EndpointPort getEndpointPort(int port) {
        return getEndpointPort(port, null)
    }

    static CoreV1EndpointPort getEndpointPort(int port, String name) {
        CoreV1EndpointPort endpointPort = new CoreV1EndpointPort(port)
        endpointPort.name(name)
        return endpointPort
    }

    static V1EndpointAddress getEndpointAddress(String ip) {
        return new V1EndpointAddress(ip)
    }

    static V1EndpointSubset getEndpointSubset(List<V1EndpointAddress> addresses, List<CoreV1EndpointPort> ports) {
        V1EndpointSubset endpointSubset = new V1EndpointSubset()
        endpointSubset.addresses(addresses)
        endpointSubset.ports(ports)
        return endpointSubset
    }

    static V1Endpoints getEndpoints(String name, List<V1EndpointSubset> subsets) {
        return getEndpoints(name, subsets, [:])
    }

    static V1Endpoints getEndpoints(String name, List<V1EndpointSubset> subsets, Map<String, String> labels) {
        V1Endpoints endpoints = new V1Endpoints()
        endpoints.kind('Endpoints')
        endpoints.apiVersion('v1')
        endpoints.metadata(getObjectMeta(name, labels))
        endpoints.subsets(subsets)
        return endpoints
    }
}
