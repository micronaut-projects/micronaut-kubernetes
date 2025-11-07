package io.micronaut.kubernetes.client.openapi

import io.micronaut.context.ApplicationContext
import io.micronaut.context.env.Environment
import io.micronaut.kubernetes.client.openapi.api.CoreV1Api
import io.micronaut.kubernetes.client.openapi.model.V1ConfigMap
import io.micronaut.kubernetes.client.openapi.model.V1Namespace
import io.micronaut.kubernetes.client.openapi.response.DeleteResponse
import io.micronaut.kubernetes.openapi.test.K3sContainerSpec
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import static io.micronaut.kubernetes.openapi.test.KubernetesModels.getConfigMapModel
import static io.micronaut.kubernetes.openapi.test.KubernetesModels.getNamespaceModel
import static io.micronaut.kubernetes.openapi.test.KubernetesOperations.createConfigMap
import static io.micronaut.kubernetes.openapi.test.KubernetesOperations.createNamespace
import static io.micronaut.kubernetes.openapi.test.KubernetesOperations.deleteConfigMap
import static io.micronaut.kubernetes.openapi.test.KubernetesOperations.deleteNamespace

class DeleteObjectSpec extends K3sContainerSpec {

    private static final Logger LOG = LoggerFactory.getLogger(DeleteObjectSpec)

    private static final String NAMESPACE_NAME = 'micronaut-test-namespace'
    private static final String CONFIG_MAP_NAME = 'micronaut-test-config-map'

    @Override
    Logger getLogger() {
        return LOG
    }

    @Override
    def setupKubernetes(ApplicationContext context) {
        CoreV1Api api = context.getBean(CoreV1Api.class)
        createNamespace(api, getNamespaceModel(NAMESPACE_NAME))
        createConfigMap(api, NAMESPACE_NAME, getConfigMapModel(CONFIG_MAP_NAME, ['test.properties': 'testKey=testValue']))
    }

    def 'delete kubernetes objects'() {
        given:
        ApplicationContext context = ApplicationContext.run([
                "kubernetes.client.kube-config-path"   : "file:" + kubeConfigFile.toString(),
                "kubernetes.client.namespace"          : NAMESPACE_NAME
        ], Environment.KUBERNETES)
        CoreV1Api api = context.getBean(CoreV1Api.class)

        when:
        DeleteResponse<V1ConfigMap> configMapResponse = deleteConfigMap(api, NAMESPACE_NAME, CONFIG_MAP_NAME)
        DeleteResponse<V1Namespace> namespaceResponse = deleteNamespace(api, NAMESPACE_NAME)

        then:
        configMapResponse.object() == null
        configMapResponse.status().apiVersion == 'v1'
        configMapResponse.status().details.kind == 'configmaps'
        configMapResponse.status().details.name == CONFIG_MAP_NAME

        namespaceResponse.object().apiVersion == 'v1'
        namespaceResponse.object().metadata.name == NAMESPACE_NAME
        namespaceResponse.status() == null
    }
}
