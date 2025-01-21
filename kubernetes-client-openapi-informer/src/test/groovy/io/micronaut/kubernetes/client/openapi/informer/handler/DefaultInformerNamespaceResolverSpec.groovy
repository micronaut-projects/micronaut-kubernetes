package io.micronaut.kubernetes.client.openapi.informer.handler

import io.micronaut.context.ApplicationContext
import io.micronaut.context.annotation.Context
import io.micronaut.context.annotation.Requires
import io.micronaut.context.env.Environment
import io.micronaut.core.annotation.AnnotationValue
import io.micronaut.inject.BeanDefinition
import io.micronaut.kubernetes.client.openapi.model.V1Secret
import spock.lang.Specification

import java.util.function.Supplier

class DefaultInformerNamespaceResolverSpec extends Specification {

    def 'test all namespaces'() {
        given:
        ApplicationContext context = ApplicationContext.run([
                'kubernetes.client.informer.enabled': false,
                'spec.name': 'handler1'
        ])
        BeanDefinition beanDefinition = context.getBeanDefinition(EventHandler1.class)
        AnnotationValue<Informer> annotationValue = beanDefinition.getAnnotation(Informer.class)
        DefaultInformerNamespaceResolver resolver = context.getBean(DefaultInformerNamespaceResolver.class)

        when:
        Set<String> namespaces = resolver.resolveInformerNamespaces(annotationValue)

        then:
        namespaces != null
        namespaces.size() == 0

        cleanup:
        context.close()
    }

    def 'test namespace annotation field'() {
        given:
        ApplicationContext context = ApplicationContext.run([
                'kubernetes.client.informer.enabled': false,
                'spec.name': 'handler2'
        ])
        BeanDefinition beanDefinition = context.getBeanDefinition(EventHandler2.class)
        AnnotationValue<Informer> annotationValue = beanDefinition.getAnnotation(Informer.class)
        DefaultInformerNamespaceResolver resolver = context.getBean(DefaultInformerNamespaceResolver.class)

        when:
        Set<String> namespaces = resolver.resolveInformerNamespaces(annotationValue)

        then:
        namespaces != null
        namespaces.size() == 1
        namespaces.contains('namespace1')

        cleanup:
        context.close()
    }

    def 'test namespaces annotation field'() {
        given:
        ApplicationContext context = ApplicationContext.run([
                'kubernetes.client.informer.enabled': false,
                'spec.name': 'handler3'
        ])
        BeanDefinition beanDefinition = context.getBeanDefinition(EventHandler3.class)
        AnnotationValue<Informer> annotationValue = beanDefinition.getAnnotation(Informer.class)
        DefaultInformerNamespaceResolver resolver = context.getBean(DefaultInformerNamespaceResolver.class)

        when:
        Set<String> namespaces = resolver.resolveInformerNamespaces(annotationValue)

        then:
        namespaces != null
        namespaces.size() == 2
        namespaces.contains('namespace2')
        namespaces.contains('namespace3')

        cleanup:
        context.close()
    }

    def 'test namespace supplier annotation field'() {
        given:
        ApplicationContext context = ApplicationContext.run([
                'kubernetes.client.informer.enabled': false,
                'spec.name': 'handler4'
        ])
        BeanDefinition beanDefinition = context.getBeanDefinition(EventHandler4.class)
        AnnotationValue<Informer> annotationValue = beanDefinition.getAnnotation(Informer.class)
        DefaultInformerNamespaceResolver resolver = context.getBean(DefaultInformerNamespaceResolver.class)

        when:
        Set<String> namespaces = resolver.resolveInformerNamespaces(annotationValue)

        then:
        namespaces != null
        namespaces.size() == 1
        namespaces.contains('namespace4')

        cleanup:
        context.close()
    }

    def 'test all namespace annotation fields'() {
        given:
        ApplicationContext context = ApplicationContext.run([
                'kubernetes.client.informer.enabled': false,
                'spec.name': 'handler5'
        ])
        BeanDefinition beanDefinition = context.getBeanDefinition(EventHandler5.class)
        AnnotationValue<Informer> annotationValue = beanDefinition.getAnnotation(Informer.class)
        DefaultInformerNamespaceResolver resolver = context.getBean(DefaultInformerNamespaceResolver.class)

        when:
        Set<String> namespaces = resolver.resolveInformerNamespaces(annotationValue)

        then:
        namespaces != null
        namespaces.size() == 4
        namespaces.contains('namespace1')
        namespaces.contains('namespace2')
        namespaces.contains('namespace3')
        namespaces.contains('namespace4')

        cleanup:
        context.close()
    }

    def 'test resolve automatically namespace'() {
        given:
        ApplicationContext context = ApplicationContext.run([
                'kubernetes.client.informer.enabled': false,
                'spec.name': 'handler6',
                'kubernetes.client.namespace': 'namespace5'
        ], Environment.KUBERNETES)
        BeanDefinition beanDefinition = context.getBeanDefinition(EventHandler6.class)
        AnnotationValue<Informer> annotationValue = beanDefinition.getAnnotation(Informer.class)
        DefaultInformerNamespaceResolver resolver = context.getBean(DefaultInformerNamespaceResolver.class)

        when:
        Set<String> namespaces = resolver.resolveInformerNamespaces(annotationValue)

        then:
        namespaces != null
        namespaces.size() == 1
        namespaces.contains('namespace5')

        cleanup:
        context.close()
    }

    def 'test resolve automatically namespace failed'() {
        given:
        ApplicationContext context = ApplicationContext.run([
                'kubernetes.client.informer.enabled': false,
                'spec.name': 'handler7'
        ])
        BeanDefinition beanDefinition = context.getBeanDefinition(EventHandler7.class)
        AnnotationValue<Informer> annotationValue = beanDefinition.getAnnotation(Informer.class)
        DefaultInformerNamespaceResolver resolver = context.getBean(DefaultInformerNamespaceResolver.class)

        when:
        resolver.resolveInformerNamespaces(annotationValue)

        then:
        def error = thrown(IllegalStateException)
        error.message == "The @Informer's namespace value is set to " + Informer.RESOLVE_AUTOMATICALLY +
                " but namespace resolver not found"

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
    @Informer(apiType = V1Secret.class, namespace = Informer.ALL_NAMESPACES)
    @Requires(property = 'spec.name', value = 'handler1')
    private static final class EventHandler1 extends BaseResourceEventHandler {}

    @Context
    @Informer(apiType = V1Secret.class, namespace = 'namespace1')
    @Requires(property = 'spec.name', value = 'handler2')
    private static final class EventHandler2 extends BaseResourceEventHandler {}

    @Context
    @Informer(apiType = V1Secret.class, namespaces = ['namespace2', 'namespace3'])
    @Requires(property = 'spec.name', value = 'handler3')
    private static final class EventHandler3 extends BaseResourceEventHandler {}

    @Context
    @Informer(apiType = V1Secret.class, namespacesSupplier = SimpleNamespacesSupplier1.class)
    @Requires(property = 'spec.name', value = 'handler4')
    private static final class EventHandler4 extends BaseResourceEventHandler {}

    @Context
    @Requires(property = 'spec.name', value = 'handler4')
    private static final class SimpleNamespacesSupplier1 implements Supplier<String[]> {
        @Override
        String[] get() {
            return new String[] {'namespace4'}
        }
    }

    @Context
    @Informer(apiType = V1Secret.class, namespace = 'namespace1', namespaces = ['namespace2', 'namespace3'], namespacesSupplier = SimpleNamespacesSupplier2.class)
    @Requires(property = 'spec.name', value = 'handler5')
    private static final class EventHandler5 extends BaseResourceEventHandler {}

    @Context
    @Requires(property = 'spec.name', value = 'handler5')
    private static final class SimpleNamespacesSupplier2 implements Supplier<String[]> {
        @Override
        String[] get() {
            return new String[] {'namespace4'}
        }
    }

    @Context
    @Informer(apiType = V1Secret.class)
    @Requires(property = 'spec.name', value = 'handler6')
    private static final class EventHandler6 extends BaseResourceEventHandler {}

    @Context
    @Informer(apiType = V1Secret.class)
    @Requires(property = 'spec.name', value = 'handler7')
    private static final class EventHandler7 extends BaseResourceEventHandler {}
}
