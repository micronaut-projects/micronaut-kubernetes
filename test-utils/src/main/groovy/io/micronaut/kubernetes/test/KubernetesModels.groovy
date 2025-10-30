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
package io.micronaut.kubernetes.test

import io.kubernetes.client.custom.IntOrString
import io.kubernetes.client.openapi.models.RbacV1Subject
import io.kubernetes.client.openapi.models.V1ClusterRole
import io.kubernetes.client.openapi.models.V1ConfigMap
import io.kubernetes.client.openapi.models.V1Endpoints
import io.kubernetes.client.openapi.models.V1Namespace
import io.kubernetes.client.openapi.models.V1ObjectMeta
import io.kubernetes.client.openapi.models.V1PolicyRule
import io.kubernetes.client.openapi.models.V1Role
import io.kubernetes.client.openapi.models.V1RoleBinding
import io.kubernetes.client.openapi.models.V1RoleRef
import io.kubernetes.client.openapi.models.V1Secret
import io.kubernetes.client.openapi.models.V1Service
import io.kubernetes.client.openapi.models.V1ServicePort
import io.kubernetes.client.openapi.models.V1ServiceSpec

class KubernetesModels {

    static V1ObjectMeta getObjectMetaModel(String name, Map<String, String> labels = [:], Map<String, String> annotations = [:]) {
        return new V1ObjectMeta().name(name).labels(labels).annotations(annotations)
    }

    static V1Namespace getNamespaceModel(String name) {
        return new V1Namespace().kind('Namespace').metadata(getObjectMetaModel(name))
    }

    static V1PolicyRule getPolicyRuleModel(List<String> apiGroups = [], List<String> verbs = [], List<String> resources = []) {
        return new V1PolicyRule().apiGroups(apiGroups).resources(resources).verbs(verbs)
    }

    static V1Role getRoleModel(String name, List<V1PolicyRule> rules = []) {
        return new V1Role().kind('Role').metadata(getObjectMetaModel(name)).rules(rules)
    }

    static RbacV1Subject getSubjectModel(String kind, String name, String namespace) {
        return new RbacV1Subject().kind(kind).name(name).namespace(namespace)
    }

    static V1RoleRef getRoleRefModel(String name) {
        return new V1RoleRef().apiGroup('rbac.authorization.k8s.io').kind('Role').name(name)
    }

    static V1RoleBinding getRoleBindingModel(String name, V1RoleRef roleRef, List<RbacV1Subject> subjects) {
        return new V1RoleBinding().metadata(getObjectMetaModel(name)).roleRef(roleRef).subjects(subjects)
    }

    static V1ConfigMap getConfigMapModel(String name, Map<String, String> data, Map<String, String> labels = [:], Map<String, String> annotations = [:]) {
        return new V1ConfigMap().kind('ConfigMap').metadata(getObjectMetaModel(name, labels, annotations)).data(data)
    }

    static V1Secret getSecretModel(String name, Map<String, byte[]> data, Map<String, String> labels = [:]) {
        return new V1Secret().kind('Secret').metadata(getObjectMetaModel(name, labels)).data(data)
    }

    static V1ServicePort getServicePortModel(String name, Integer port, String targetPort) {
        return new V1ServicePort().name(name).port(port).targetPort(new IntOrString(targetPort))
    }

    static V1ServicePort getServicePortModel(Integer port, Integer targetPort) {
        return new V1ServicePort().port(port).targetPort(new IntOrString(targetPort))
    }

    static V1ServicePort getServicePortModel(String name, Integer port) {
        return new V1ServicePort().name(name).port(port)
    }

    static V1ServicePort getServicePortModel(Integer port) {
        return new V1ServicePort().port(port)
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

    static V1Endpoints getEndpointsModel(String name, Map<String, String> labels = [:], Map<String, String> annotations = [:]) {
        return new V1Endpoints().kind('Endpoints').metadata(getObjectMetaModel(name, labels, annotations))
    }

    static V1ClusterRole getClusterRoleModel(String name, List<V1PolicyRule> rules) {
        return new V1ClusterRole().kind('ClusterRole').metadata(getObjectMetaModel(name)).rules(rules)
    }
}
