/*
 * Copyright 2017-2025 original authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.micronaut.kubernetes.openapi.test

import io.micronaut.kubernetes.client.openapi.model.CoreV1EndpointPort
import io.micronaut.kubernetes.client.openapi.model.RbacV1Subject
import io.micronaut.kubernetes.client.openapi.model.V1ClusterRole
import io.micronaut.kubernetes.client.openapi.model.V1ConfigMap
import io.micronaut.kubernetes.client.openapi.model.V1Container
import io.micronaut.kubernetes.client.openapi.model.V1EndpointAddress
import io.micronaut.kubernetes.client.openapi.model.V1EndpointSubset
import io.micronaut.kubernetes.client.openapi.model.V1Endpoints
import io.micronaut.kubernetes.client.openapi.model.V1HTTPGetAction
import io.micronaut.kubernetes.client.openapi.model.V1Namespace
import io.micronaut.kubernetes.client.openapi.model.V1ObjectMeta
import io.micronaut.kubernetes.client.openapi.model.V1Pod
import io.micronaut.kubernetes.client.openapi.model.V1PodSpec
import io.micronaut.kubernetes.client.openapi.model.V1PolicyRule
import io.micronaut.kubernetes.client.openapi.model.V1Probe
import io.micronaut.kubernetes.client.openapi.model.V1Role
import io.micronaut.kubernetes.client.openapi.model.V1RoleBinding
import io.micronaut.kubernetes.client.openapi.model.V1RoleRef
import io.micronaut.kubernetes.client.openapi.model.V1Secret
import io.micronaut.kubernetes.client.openapi.model.V1Service
import io.micronaut.kubernetes.client.openapi.model.V1ServicePort
import io.micronaut.kubernetes.client.openapi.model.V1ServiceSpec

class KubernetesModels {

    static V1ObjectMeta getObjectMetaModel(String name, Map<String, String> labels = [:], Map<String, String> annotations = [:]) {
        return new V1ObjectMeta().name(name).labels(labels).annotations(annotations)
    }

    static V1Namespace getNamespaceModel(String name) {
        return new V1Namespace().kind('Namespace').metadata(getObjectMetaModel(name))
    }

    static V1PolicyRule getPolicyRuleModel(List<String> apiGroups = [], List<String> verbs = [], List<String> resources = []) {
        return new V1PolicyRule(verbs).apiGroups(apiGroups).resources(resources)
    }

    static V1Role getRoleModel(String name, List<V1PolicyRule> rules = []) {
        return new V1Role().kind('Role').metadata(getObjectMetaModel(name)).rules(rules)
    }

    static RbacV1Subject getSubjectModel(String kind, String name, String namespace) {
        return new RbacV1Subject(kind, name).namespace(namespace)
    }

    static V1RoleRef getRoleRefModel(String name) {
        return new V1RoleRef('rbac.authorization.k8s.io', 'Role', name)
    }

    static V1RoleBinding getRoleBindingModel(String name, V1RoleRef roleRef, List<RbacV1Subject> subjects) {
        return new V1RoleBinding(roleRef).metadata(getObjectMetaModel(name)).subjects(subjects)
    }

    static V1ConfigMap getConfigMapModel(String name, Map<String, String> data, Map<String, String> labels = [:], Map<String, String> annotations = [:]) {
        return new V1ConfigMap().kind('ConfigMap').metadata(getObjectMetaModel(name, labels, annotations)).data(data)
    }

    static V1Secret getSecretModel(String name, Map<String, byte[]> data, Map<String, String> labels = [:]) {
        return new V1Secret().kind('Secret').metadata(getObjectMetaModel(name, labels)).data(data)
    }

    static V1ServicePort getServicePortModel(int port, String targetPort, String name = null) {
        return new V1ServicePort(port).name(name).targetPort(targetPort)
    }

    static V1ServiceSpec getServiceSpecTypeModel(String type, List<V1ServicePort> ports, Map<String, String> selector = [:], String externalName = null) {
        return new V1ServiceSpec().type(type).ports(ports).selector(selector).externalName(externalName)
    }

    static V1ServiceSpec getServiceSpecClusterIPModel(String clusterIP, List<V1ServicePort> ports, Map<String, String> selector = [:]) {
        return new V1ServiceSpec().clusterIP(clusterIP).ports(ports).selector(selector)
    }

    static V1Service getServiceModel(String name, V1ServiceSpec spec, Map<String, String> labels = [:]) {
        return new V1Service().kind('Service').metadata(getObjectMetaModel(name, labels)).spec(spec)
    }

    static V1Endpoints getEndpointsModel(String name, List<V1EndpointSubset> subsets, Map<String, String> labels = [:], Map<String, String> annotations = [:]) {
        return getEndpointsModel(name, labels, annotations).subsets(subsets)
    }

    static V1Endpoints getEndpointsModel(String name, Map<String, String> labels = [:], Map<String, String> annotations = [:]) {
        return new V1Endpoints().kind('Endpoints').metadata(getObjectMetaModel(name, labels, annotations))
    }

    static V1EndpointSubset getEndpointSubsetModel(List<V1EndpointAddress> addresses, List<CoreV1EndpointPort> ports) {
        return new V1EndpointSubset().addresses(addresses).ports(ports)
    }

    static CoreV1EndpointPort getEndpointPortModel(int port, String name = null) {
        return new CoreV1EndpointPort(port).name(name)
    }

    static V1EndpointAddress getEndpointAddressModel(String ip) {
        return new V1EndpointAddress(ip)
    }

    static V1ClusterRole getClusterRoleModel(String name, List<V1PolicyRule> rules) {
        return new V1ClusterRole().kind('ClusterRole').metadata(getObjectMetaModel(name)).rules(rules)
    }

    static V1Container getContainerModel(String name) {
        return new V1Container(name).image("test-image")
    }

    static V1PodSpec getPodSpecModel(List<V1Container> containers) {
        return new V1PodSpec(containers)
    }

    static V1Pod getPodModel(String name, V1PodSpec podSpec, Map<String, String> labels = [:]) {
        return new V1Pod().kind('Pod').metadata(getObjectMetaModel(name, labels)).spec(podSpec)
    }

    static V1HTTPGetAction getHTTPGetActionModel(String port, String path) {
        return new V1HTTPGetAction(port).path(path)
    }

    static V1Probe getProbeModel(V1HTTPGetAction httpGetAction, Integer initialDelaySeconds, Integer periodSeconds, Integer failureThreshold) {
        return new V1Probe().httpGet(httpGetAction).initialDelaySeconds(initialDelaySeconds).periodSeconds(periodSeconds).failureThreshold(failureThreshold)
    }
}
