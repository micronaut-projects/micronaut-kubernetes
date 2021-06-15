package io.micronaut.kubernetes.discovery

import io.fabric8.kubernetes.api.model.ContainerBuilder
import io.fabric8.kubernetes.api.model.PodBuilder
import io.fabric8.kubernetes.api.model.PodSpecBuilder
import io.micronaut.context.annotation.Property
import io.micronaut.context.env.Environment
import io.micronaut.kubernetes.test.KubernetesSpecification
import io.micronaut.kubernetes.test.TestUtils
import io.micronaut.test.extensions.spock.annotation.MicronautTest
import spock.lang.Requires

import javax.inject.Inject


@MicronautTest(environments = [Environment.KUBERNETES])
@Requires({ TestUtils.kubernetesApiAvailable() })
@Property(name = "kubernetes.client.namespace", value = "label-selector")
@Property(name = "spec.reuseNamespace", value = "false")
class DefaultLabelSelectorSpec extends KubernetesSpecification {

    @Inject
    LabelResolver labelResolver

    def setupFixture(String namespace) {
        createNamespaceSafe("label-selector")
        operations.getClient(namespace).pods().create(new PodBuilder()
                .withNewMetadata()
                .withName("busybox-with-labels")
                .withLabels(["label-1": "value-1", "label-2": null, "label-3": "", "label-4": "not-requested"])
                .endMetadata()
                .withSpec(new PodSpecBuilder()
                        .withRestartPolicy("Never")
                        .withContainers(new ContainerBuilder()
                                .withName("busy-box")
                                .withImage("busybox")
                                .withArgs("sleep 1h")
                                .build()
                        ).build()
                )
                .build()
        )
    }

    def "test it loads pod labels"() {
        when:
        def labels = labelResolver.resolvePodLabels("busybox-with-labels", ["label-1", "label-2", "label-3"])
                .blockingFirst()

        then: "with label-1 value"
        labels.containsKey("label-1")
        labels.get("label-1") == "value-1"

        and: "with label-2 null value evaluated as empty string"
        labels.containsKey("label-2")
        labels.get("label-2") == ""

        and: "with label-3 empty string value"
        labels.containsKey("label-3")
        labels.get("label-3") == ""

        and: "doesn't contain not requested valued labels"
        !labels.containsKey("label-4")
    }

    def "test it loads empty labels when pod not exists"() {
        when:
        def labels = labelResolver.resolvePodLabels("busybox-with-labels-X", ["label-1", "label-2", "label-3"])
                .blockingFirst()

        then:
        labels.isEmpty()
    }
}
