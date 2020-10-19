/*
 * Copyright 2017-2020 original authors
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

import groovy.util.logging.Slf4j
import io.fabric8.kubernetes.api.model.*
import io.fabric8.kubernetes.api.model.apps.Deployment
import io.fabric8.kubernetes.api.model.apps.DoneableDeployment
import io.fabric8.kubernetes.api.model.rbac.*
import io.fabric8.kubernetes.client.DefaultKubernetesClient
import io.fabric8.kubernetes.client.KubernetesClient
import io.fabric8.kubernetes.client.LocalPortForward
import io.fabric8.kubernetes.client.dsl.RollableScalableResource
import io.micronaut.core.util.StringUtils

import java.util.concurrent.TimeUnit

/**
 * @author Pavol Gressa
 * @since 2.2
 */
@Slf4j
class KubernetesOperations implements Closeable {

    private KubernetesClient client = new DefaultKubernetesClient()
    private List<LocalPortForward> portForwardList = new ArrayList<>()

    Namespace createNamespace(String name) {
        log.debug("Creating namespace ${name}")
        return client.namespaces().create(
                new NamespaceBuilder()
                        .withNewMetadata()
                        .withName(name)
                        .endMetadata()
                        .build())
    }

    Namespace getNamespace(String name) {
        return client.namespaces().withName(name).get()
    }

    boolean deleteNamespace(String namespace) {
        log.debug("Deleting namespace ${namespace}")
        return client.namespaces()
                .delete(getNamespace(namespace))
    }

    Role createRole(String name,
                    String namespace,
                    List<String> verbs = ["get", "list", "watch"],
                    List<String> resources = ["services", "endpoints", "configmaps", "secrets", "pods"]) {
        PolicyRule policyRule = new PolicyRuleBuilder()
                .withApiGroups("")
                .withResources(resources)
                .withVerbs(verbs)
                .build()
        log.debug("Creating Role ${name} ${policyRule}")
        return client.rbac().roles().create(
                new RoleBuilder()
                        .withNewMetadata()
                        .withName(name)
                        .withNamespace(namespace)
                        .endMetadata()
                        .addToRules(policyRule)
                        .build())
    }

    RoleBinding createRoleBinding(String name, String namespace,
                                  String roleRefName, String accountName = "default") {

        Subject ks = new SubjectBuilder()
                .withKind("ServiceAccount")
                .withName(accountName)
                .withNamespace(namespace)
                .build();

        RoleRef roleRef = new RoleRefBuilder()
                .withName(roleRefName)
                .withApiGroup("rbac.authorization.k8s.io")
                .withKind("Role")
                .build();

        RoleBinding rb = new RoleBindingBuilder()
                .withNewMetadata()
                .withName(name)
                .withNamespace(namespace)
                .endMetadata()
                .withRoleRef(roleRef)
                .withSubjects(Collections.singletonList(ks))
                .build();

        return client.rbac().roleBindings().create(rb)
    }

    ConfigMap getConfigMap(String name, String namespace) {
        return client.configMaps().inNamespace(namespace).withName(name).get()
    }

    ConfigMap createConfigMap(String name, String namespace,
                              Map data = [foo: 'bar'], Map<String, String> labels = [:]) {
        def cm = new ConfigMapBuilder()
                .withNewMetadata()
                .withName(name)
                .withNamespace(namespace)
                .withLabels(labels)
                .endMetadata()
                .withData(data)
                .build()
        log.debug("Creating ${cm}")
        return client.configMaps().create(cm)
    }

    ConfigMap createConfigMapFromFile(String name, String namespace,
                                      URL path, Map<String, String> labels = [:]) {
        ConfigMap cm = new ConfigMapBuilder()
                .withNewMetadata()
                .withName(name)
                .withNamespace(namespace)
                .withLabels(labels)
                .endMetadata()
                .addToData(new File(path.toURI().toString()).name, path.text)
                .build()
        log.debug("Creating ${cm}")
        return client.configMaps().create(cm)
    }

    boolean deleteConfigMap(String name, String namespace) {
        log.debug("Deleting config map ${namespace}/${name}")
        return client.configMaps()
                .inNamespace(namespace)
                .withName(name)
                .delete()
    }

    String modifyConfigMap(String name, String namespace, Map data = [foo: 'baz']) {
        return client.configMaps().inNamespace(namespace).withName(name).createOrReplace(
                new ConfigMapBuilder().
                        withNewMetadata()
                        .withName(name)
                        .withNamespace(namespace)
                        .and()
                        .withData(data).build()
        )
    }

    ConfigMapList listConfigMaps(String namespace){
        return client.configMaps().inNamespace(namespace).list()
    }

    List<Pod> getPods(String namespace) {
        return client.pods().inNamespace(namespace).list().items
    }

    Secret createSecret(String name, String namespace, Map<String, String> literals, Map<String, String> labels = [:]) {
        Secret secret = new SecretBuilder()
                .withNewMetadata()
                .withName(name)
                .withNamespace(namespace)
                .withLabels(labels)
                .endMetadata()
                .withData(literals).build()
        log.debug("Creating ${secret}")
        return client.secrets().create(secret)
    }

    Secret getSecret(String name, String namespace) {
        return client.secrets().inNamespace(namespace).withName(name).get()
    }

    Deployment createDeploymentFromFile(URL pathToManifest, String name = null, String namespace = null) {
        RollableScalableResource<Deployment, DoneableDeployment> deployment = client.apps().deployments().load(pathToManifest)
        if (StringUtils.isNotEmpty(name)) {
            deployment.get().metadata.name = name
        }

        if (StringUtils.isNotEmpty(namespace)) {
            deployment.get().metadata.namespace = namespace
        }

        log.debug("Creating deployment ${deployment.get()}")
        client.apps().deployments().create(deployment.get())

        log.debug("Waiting 60s until ready")
        return deployment.waitUntilCondition(
                d -> d.status.availableReplicas == d.spec.replicas,
                60, TimeUnit.SECONDS)
    }

    Deployment getDeployment(String name, String namespace) {
        return client.apps().deployments().inNamespace(namespace).withName(name).get()
    }

    Service createService(String name, String namespace, ServiceSpec serviceSpec, Map<String, String> labels = [:]){
        Service service = new ServiceBuilder()
                .withNewMetadata()
                .withName(name)
                .withNamespace(namespace)
                .withLabels(labels)
                .and()
                .withSpec(serviceSpec)
                .build()
        log.debug("Creating service ${service}")
        return client.services().create(service)
    }

    Service getService(String name, String namespace){
        return client.services().inNamespace(namespace).withName(name).get()
    }

    ServiceList listServices(String namespace){
        return client.services().inNamespace(namespace).list()
    }

    Endpoints getEndpoints(String name, String namespace){
        return client.endpoints().inNamespace(namespace).withName(name).get()
    }

    EndpointsList listEndpoints(String namespace){
        return client.endpoints().inNamespace(namespace).list()
    }

    LocalPortForward portForwardService(String name, String namespace, int sourcePort, int targetPort){
        Service service = getService(name, namespace)
        service.spec.ports.stream()
                .filter(s -> s.port == sourcePort)
                .findFirst()
                .orElseThrow(() ->
                        new IllegalArgumentException("Service ${namespace}/${name} doesn't contain port ${sourcePort}"))

        LocalPortForward lpf = client
                .services()
                .inNamespace(namespace)
                .withName(name)
                .portForward(sourcePort, targetPort)
        portForwardList.add(lpf)

        if(!lpf.isAlive()){
            throw new IllegalArgumentException("Failed to port forward service ${namespace}/${name} " +
                    "port ${sourcePort} -> ${targetPort}")
        }
        log.debug("Forwarding service ${namespace}/${name} port ${sourcePort} to ${targetPort}")
        return lpf
    }

    @Override
    void close() throws IOException {
        portForwardList.forEach(it -> it.close())
        client.close()
    }
}
