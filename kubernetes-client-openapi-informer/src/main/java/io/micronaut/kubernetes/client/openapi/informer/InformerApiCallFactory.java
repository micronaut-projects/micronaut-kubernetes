package io.micronaut.kubernetes.client.openapi.informer;

import io.micronaut.context.ApplicationContext;
import io.micronaut.core.annotation.Nullable;
import io.micronaut.core.type.Argument;
import io.micronaut.core.util.StringUtils;
import io.micronaut.inject.BeanDefinition;
import io.micronaut.inject.ExecutableMethod;
import io.micronaut.inject.qualifiers.Qualifiers;
import io.micronaut.kubernetes.client.openapi.annotation.KubernetesClientApi;
import io.micronaut.kubernetes.client.openapi.common.KubernetesObject;
import io.micronaut.kubernetes.client.openapi.watcher.annotation.KubernetesClientApiWatcher;
import jakarta.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * The factory which creates {@link InformerApiCall} instances using mappings between
 * kubernetes api types and {@link ExecutableMethod} instances which can be used for
 * execution of {@code list} and {@code watch} api calls.
 */
@SuppressWarnings("rawtypes")
@Singleton
class InformerApiCallFactory {
    private static final Logger LOG = LoggerFactory.getLogger(InformerApiCallFactory.class);

    private final ApplicationContext applicationContext;
    private final ExecMethodHolder listExecMethodHolder;
    private final ExecMethodHolder watchExecMethodHolder;

    InformerApiCallFactory(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
        listExecMethodHolder = createListExecMethodHolder();
        watchExecMethodHolder = createWatchExecMethodHolder();
    }

    /**
     * Creates an {@link InformerApiCall} instance which can handle execution of
     * {@code list} and {@code watch} api calls for given api type.
     *
     * @param apiTypeClass the api type
     * @param namespace    the namespace should be provided only if {@code list} and {@code watch}
     *                     api calls should be restricted to given namespace
     * @param <ApiType>    kubernetes api type
     * @return an instance of {@link InformerApiCall}
     */
    <ApiType extends KubernetesObject> InformerApiCall<ApiType> createInformerApiCall(Class<ApiType> apiTypeClass, @Nullable String namespace) {
        String apiTypeClassName = apiTypeClass.getName();
        boolean useNamespace;
        ExecutableMethod listExecMethod;
        if (StringUtils.isEmpty(namespace)) {
            useNamespace = false;
            listExecMethod = listExecMethodHolder.globalExecMethods.get(apiTypeClassName);
        } else {
            ExecutableMethod namespacedListExecMethod = listExecMethodHolder.namespaceExecMethods.get(apiTypeClassName);
            if (namespacedListExecMethod != null) {
                useNamespace = true;
                listExecMethod = namespacedListExecMethod;
            } else {
                LOG.warn("Usage of namespaced api calls not supported for type '{}', so fallback to global api calls", apiTypeClassName);
                useNamespace = false;
                listExecMethod = listExecMethodHolder.globalExecMethods.get(apiTypeClassName);
            }
        }
        if (listExecMethod == null) {
            throw new IllegalArgumentException(apiTypeClassName + " is not supported");
        }
        Class<?> listBeanType = listExecMethodHolder.beanTypes.get(apiTypeClassName);
        Object listBean = applicationContext.getBean(listBeanType);

        ExecutableMethod watchExecMethod = useNamespace
            ? watchExecMethodHolder.namespaceExecMethods.get(apiTypeClassName)
            : watchExecMethodHolder.globalExecMethods.get(apiTypeClassName);
        Class<?> watchBeanType = watchExecMethodHolder.beanTypes.get(apiTypeClassName);
        Object watchBean = applicationContext.getBean(watchBeanType);

        return new InformerApiCall<ApiType>(listExecMethod, listBean, watchExecMethod, watchBean, namespace);
    }

    /**
     * Creates a holder which contains mappings between kubernetes api types and {@link ExecutableMethod}
     * instances which can be used for execution of {@code list} api calls.
     *
     * @return exec method holder
     */
    private ExecMethodHolder createListExecMethodHolder() {
        ExecMethodHolder execMethodHolder = new ExecMethodHolder();
        Collection<BeanDefinition<?>> beanDefinitions = applicationContext.getBeanDefinitions(Qualifiers.byAnnotation(() -> KubernetesClientApi.class));
        beanDefinitions.forEach(beanDefinition -> beanDefinition.getExecutableMethods().forEach(execMethod -> {
            if (!hasParameter(execMethod, "watch")) {
                return;
            }
            String returnListTypeName = execMethod.getReturnType()
                .getType()
                .getName();
            if (returnListTypeName.endsWith("List")) {
                String returnTypeName = returnListTypeName.substring(0, returnListTypeName.indexOf("List"));
                execMethodHolder.addExecMethod(returnTypeName, execMethod, hasParameter(execMethod, "namespace"));
                execMethodHolder.beanTypes.put(returnTypeName, beanDefinition.getBeanType());
            }
        }));
        return execMethodHolder;
    }

    /**
     * Creates a holder which contains mappings between kubernetes api types and {@link ExecutableMethod}
     * instances which can be used for execution of {@code watch} api calls.
     *
     * @return exec method holder
     */
    private ExecMethodHolder createWatchExecMethodHolder() {
        ExecMethodHolder execMethodHolder = new ExecMethodHolder();
        Collection<BeanDefinition<?>> beanDefinitions = applicationContext.getBeanDefinitions(Qualifiers.byAnnotation(() -> KubernetesClientApiWatcher.class));
        beanDefinitions.forEach(beanDefinition -> beanDefinition.getExecutableMethods().forEach(execMethod -> {
            if (!Flux.class.getName().equals(execMethod.getReturnType().getType().getName())) {
                return;
            }
            String returnTypeName = execMethod.getReturnType()
                .getWrappedType()
                .getTypeParameters()[0]
                .getType()
                .getName();
            execMethodHolder.addExecMethod(returnTypeName, execMethod, hasParameter(execMethod, "namespace"));
            execMethodHolder.beanTypes.put(returnTypeName, beanDefinition.getBeanType());
        }));
        return execMethodHolder;
    }

    private boolean hasParameter(ExecutableMethod execMethod, String parameterName) {
        Optional<Argument> namespaceArgOpt = Arrays.stream(execMethod.getArguments())
            .filter(arg -> parameterName.equalsIgnoreCase(arg.getName()))
            .findFirst();
        return namespaceArgOpt.isPresent();
    }

    private static final class ExecMethodHolder {
        private final Map<String, Class<?>> beanTypes = new HashMap<>();
        private final Map<String, ExecutableMethod> globalExecMethods = new HashMap<>();
        private final Map<String, ExecutableMethod> namespaceExecMethods = new HashMap<>();

        private void addExecMethod(String typeName, ExecutableMethod execMethod, boolean namespace) {
            Map<String, ExecutableMethod> map = namespace ? namespaceExecMethods : globalExecMethods;
            if (map.containsKey(typeName)) {
                throw new IllegalStateException("The executable methods map already contains an executable method for given type, type: " +
                    typeName + ", existingMethod: " + map.get(typeName).getName() + ", newMethod: " + execMethod.getName());
            }
            map.put(typeName, execMethod);
        }
    }
}
