package io.micronaut.kubernetes.client.openapi

import io.micronaut.context.ApplicationContext
import io.micronaut.context.env.Environment
import io.micronaut.kubernetes.client.openapi.api.CoreV1Api
import io.micronaut.kubernetes.client.openapi.model.V1PodList
import io.micronaut.kubernetes.openapi.test.K3sContainerSpec
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import static io.micronaut.kubernetes.openapi.test.KubernetesOperations.listPodForAllNamespaces

class KubernetesClientCertAuthSpec extends K3sContainerSpec {

    private static final Logger LOG = LoggerFactory.getLogger(KubernetesClientCertAuthSpec)

    @Override
    Logger getLogger() {
        return LOG
    }

    def 'list pods when client certificate authentication is used'() {
        given:
        ApplicationContext context = ApplicationContext.run([
                "kubernetes.client.kube-config-path"   : "file:" + kubeConfigFile.toString()
        ], Environment.KUBERNETES)
        CoreV1Api api = context.getBean(CoreV1Api.class)

        when:
        V1PodList response = listPodForAllNamespaces(api)

        then:
        response.getItems() != null
        response.getItems().size() == 3
    }
}
