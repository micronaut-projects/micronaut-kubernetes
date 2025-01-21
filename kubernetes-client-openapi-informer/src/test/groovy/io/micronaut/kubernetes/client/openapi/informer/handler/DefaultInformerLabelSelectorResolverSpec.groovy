package io.micronaut.kubernetes.client.openapi.informer.handler

import io.micronaut.context.ApplicationContext
import io.micronaut.context.annotation.Context
import io.micronaut.context.annotation.Requires
import io.micronaut.core.annotation.AnnotationValue
import io.micronaut.inject.BeanDefinition
import io.micronaut.kubernetes.client.openapi.model.V1Secret
import spock.lang.Specification

import java.util.function.Supplier

class DefaultInformerLabelSelectorResolverSpec extends Specification {

    def 'test label selector annotation field'() {
        given:
        ApplicationContext context = ApplicationContext.run([
                'kubernetes.client.informer.enabled': false,
                'spec.name': 'handler1'
        ])
        BeanDefinition beanDefinition = context.getBeanDefinition(EventHandler1.class)
        AnnotationValue<Informer> annotationValue = beanDefinition.getAnnotation(Informer.class)
        DefaultInformerLabelSelectorResolver resolver = context.getBean(DefaultInformerLabelSelectorResolver.class)

        when:
        String label = resolver.resolveInformerLabels(annotationValue)

        then:
        label == 'selector1'

        cleanup:
        context.close()
    }

    def 'test label selector supplier annotation field'() {
        given:
        ApplicationContext context = ApplicationContext.run([
                'kubernetes.client.informer.enabled': false,
                'spec.name': 'handler2'
        ])
        BeanDefinition beanDefinition = context.getBeanDefinition(EventHandler2.class)
        AnnotationValue<Informer> annotationValue = beanDefinition.getAnnotation(Informer.class)
        DefaultInformerLabelSelectorResolver resolver = context.getBean(DefaultInformerLabelSelectorResolver.class)

        when:
        String label = resolver.resolveInformerLabels(annotationValue)

        then:
        label == 'selector2'

        cleanup:
        context.close()
    }

    def 'test all label selector annotation fields'() {
        given:
        ApplicationContext context = ApplicationContext.run([
                'kubernetes.client.informer.enabled': false,
                'spec.name': 'handler3'
        ])
        BeanDefinition beanDefinition = context.getBeanDefinition(EventHandler3.class)
        AnnotationValue<Informer> annotationValue = beanDefinition.getAnnotation(Informer.class)
        DefaultInformerLabelSelectorResolver resolver = context.getBean(DefaultInformerLabelSelectorResolver.class)

        when:
        String label = resolver.resolveInformerLabels(annotationValue)

        then:
        label == 'selector3,selector4'

        cleanup:
        context.close()
    }

    private static class BaseResourceEventHandler implements ResourceEventHandler<V1Secret> {
        @Override
        void onAdd(V1Secret obj) {}

        @Override
        void onUpdate(V1Secret oldObj, V1Secret newObj) {}

        @Override
        void onDelete(V1Secret obj, boolean deletedFinalStateUnknown) {}
    }

    @Context
    @Informer(apiType = V1Secret.class, labelSelector = 'selector1')
    @Requires(property = 'spec.name', value = 'handler1')
    private static final class EventHandler1 extends BaseResourceEventHandler {}

    @Context
    @Informer(apiType = V1Secret.class, labelSelectorSupplier = SimpleLabelSelectorSupplier1.class)
    @Requires(property = 'spec.name', value = 'handler2')
    private static final class EventHandler2 extends BaseResourceEventHandler {}

    @Context
    @Requires(property = 'spec.name', value = 'handler2')
    private static final class SimpleLabelSelectorSupplier1 implements Supplier<String> {
        @Override
        String get() {
            return "selector2"
        }
    }

    @Context
    @Informer(apiType = V1Secret.class, labelSelector = 'selector3', labelSelectorSupplier = SimpleLabelSelectorSupplier2.class)
    @Requires(property = 'spec.name', value = 'handler3')
    private static final class EventHandler3 extends BaseResourceEventHandler {}

    @Context
    @Requires(property = 'spec.name', value = 'handler3')
    private static final class SimpleLabelSelectorSupplier2 implements Supplier<String> {
        @Override
        String get() {
            return "selector4"
        }
    }
}
