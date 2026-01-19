package io.micronaut.kubernetes.client.openapi.informer

import io.micronaut.context.ApplicationContext
import io.micronaut.context.annotation.Context
import io.micronaut.context.annotation.Requires
import io.micronaut.kubernetes.client.openapi.api.CoreV1Api
import io.micronaut.kubernetes.client.openapi.api.ResourceV1Api
import io.micronaut.kubernetes.client.openapi.informer.handler.Informer
import io.micronaut.kubernetes.client.openapi.informer.handler.ResourceEventHandler
import io.micronaut.kubernetes.client.openapi.model.ResourceV1ResourceClaim
import io.micronaut.kubernetes.openapi.test.K3sContainerSpec
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import spock.util.concurrent.PollingConditions

import java.util.concurrent.ConcurrentHashMap

import static io.micronaut.kubernetes.openapi.test.KubernetesModels.getNamespaceModel
import static io.micronaut.kubernetes.openapi.test.KubernetesModels.getResourceClaimModel
import static io.micronaut.kubernetes.openapi.test.KubernetesOperations.createNamespace
import static io.micronaut.kubernetes.openapi.test.KubernetesOperations.createResourceClaim
import static io.micronaut.kubernetes.openapi.test.KubernetesOperations.deleteResourceClaim

class CustomTypeMappingSpec extends K3sContainerSpec {

    private static final Logger LOG = LoggerFactory.getLogger(CustomTypeMappingSpec.class)

    private static final NAMESPACE_NAME_1 = 'resource-claim-informer-ns-1'
    private static final RES_CLAIM_NAME_11 = 'test-11'
    private static final RES_CLAIM_NAME_12 = 'test-12'
    private static final NAMESPACE_NAME_2 = 'resource-claim-informer-ns-2'
    private static final RES_CLAIM_NAME_21 = 'test-21'

    @Override
    Logger getLogger() {
        return LOG
    }

    @Override
    def setupKubernetes(ApplicationContext context) {
        CoreV1Api api = context.getBean(CoreV1Api.class)
        createNamespace(api, getNamespaceModel(NAMESPACE_NAME_1))
        createNamespace(api, getNamespaceModel(NAMESPACE_NAME_2))

        ResourceV1Api resourceApi = context.getBean(ResourceV1Api.class)
        createResourceClaim(resourceApi, NAMESPACE_NAME_1, getResourceClaimModel(RES_CLAIM_NAME_11))
        createResourceClaim(resourceApi, NAMESPACE_NAME_2, getResourceClaimModel(RES_CLAIM_NAME_21))
    }

    def 'test resource claim informer'() {
        given:
        ApplicationContext context = ApplicationContext.run([
                'kubernetes.client.kube-config-path': "file:" + kubeConfigFile.toString(),
                'spec.name'                         : 'CustomTypeMappingSpec'
        ])
        ResourceV1Api resourceApi = context.getBean(ResourceV1Api.class)
        ResourceClaimEventHandler resourceClaimEventHandler = context.getBean(ResourceClaimEventHandler.class)

        when:
        createResourceClaim(resourceApi, NAMESPACE_NAME_1, getResourceClaimModel(RES_CLAIM_NAME_12))
        deleteResourceClaim(resourceApi, NAMESPACE_NAME_1, RES_CLAIM_NAME_11)
        def eventMessages = resourceClaimEventHandler.getEventMessages()

        PollingConditions conditions = new PollingConditions(timeout: 2)

        then:
        conditions.eventually {
            eventMessages.size() == 2
            eventMessages.get(RES_CLAIM_NAME_11).size() == 2
            eventMessages.get(RES_CLAIM_NAME_11).get(0) == 'Resource claim added'
            eventMessages.get(RES_CLAIM_NAME_11).get(1) == 'Resource claim deleted'
            eventMessages.get(RES_CLAIM_NAME_12).get(0) == 'Resource claim added'
        }

        cleanup:
        context.close()
    }

    @Context
    @Informer(apiType = ResourceV1ResourceClaim.class, namespace = NAMESPACE_NAME_1)
    @Requires(property = 'spec.name', value = 'CustomTypeMappingSpec')
    private static final class ResourceClaimEventHandler implements ResourceEventHandler<ResourceV1ResourceClaim> {

        private final Map<String, List<String>> eventMessages = new ConcurrentHashMap<>()

        Map<String, List<String>> getEventMessages() {
            return eventMessages
        }

        @Override
        void onAdd(ResourceV1ResourceClaim obj) {
            String name = obj.getMetadata().getName()
            eventMessages.computeIfAbsent(name, k -> new ArrayList<>()).add("Resource claim added")
        }

        @Override
        void onUpdate(ResourceV1ResourceClaim oldObj, ResourceV1ResourceClaim newObj) {
            String name = oldObj.getMetadata().getName()
            eventMessages.computeIfAbsent(name, k -> new ArrayList<>()).add("Resource claim updated")
        }

        @Override
        void onDelete(ResourceV1ResourceClaim obj, boolean deletedFinalStateUnknown) {
            String name = obj.getMetadata().getName()
            eventMessages.computeIfAbsent(name, k -> new ArrayList<>()).add("Resource claim deleted")
        }
    }
}
