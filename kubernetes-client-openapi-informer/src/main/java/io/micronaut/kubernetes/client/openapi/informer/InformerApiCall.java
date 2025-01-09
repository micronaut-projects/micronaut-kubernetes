package io.micronaut.kubernetes.client.openapi.informer;

import io.micronaut.core.type.Argument;
import io.micronaut.inject.ExecutableMethod;
import io.micronaut.kubernetes.client.openapi.common.KubernetesListObject;
import io.micronaut.kubernetes.client.openapi.common.KubernetesObject;
import io.micronaut.kubernetes.client.openapi.watcher.WatchEvent;
import reactor.core.publisher.Flux;

import java.util.HashMap;
import java.util.Map;

/**
 * Handles execution of {@code list} and {@code watch} api calls for given api type.
 *
 * @param <ApiType> api type which extends {@link KubernetesObject}
 */
class InformerApiCall<ApiType extends KubernetesObject> {

    private final ExecutableMethod<Object, KubernetesListObject> listExecMethod;
    private final Object listBean;
    private final ParamHolder listParamHolder;

    private final ExecutableMethod<Object, Flux<WatchEvent<ApiType>>> watchExecMethod;
    private final Object watchBean;
    private final ParamHolder watchParamHolder;

    private final String namespace;

    InformerApiCall(ExecutableMethod<Object, KubernetesListObject> listExecMethod,
                    Object listBean,
                    ExecutableMethod<Object, Flux<WatchEvent<ApiType>>> watchExecMethod,
                    Object watchBean,
                    String namespace) {
        this.listExecMethod = listExecMethod;
        this.listBean = listBean;
        listParamHolder = new ParamHolder(listExecMethod.getArguments());
        listParamHolder.setValue("namespace", namespace);
        listParamHolder.setValue("watch", false);

        this.watchExecMethod = watchExecMethod;
        this.watchBean = watchBean;
        watchParamHolder = new ParamHolder(watchExecMethod.getArguments());
        watchParamHolder.setValue("namespace", namespace);
        watchParamHolder.setValue("watch", true);

        this.namespace = namespace;
    }

    KubernetesListObject list(String resourceVersion) {
        listParamHolder.setValue("resourceVersion", resourceVersion);
        return listExecMethod.invoke(listBean, listParamHolder.values);
    }

    Flux<WatchEvent<ApiType>> watch(String resourceVersion, int timeoutSeconds) {
        watchParamHolder.setValue("resourceVersion", resourceVersion);
        watchParamHolder.setValue("timeoutSeconds", timeoutSeconds);
        return watchExecMethod.invoke(watchBean, watchParamHolder.values);
    }

    String getNamespace() {
        return namespace;
    }

    private static class ParamHolder {
        private final Map<String, Integer> position = new HashMap<>();
        private final Object[] values;

        private ParamHolder(Argument<?>[] arguments) {
            values = new Object[arguments.length];
            for (int i = 0; i < arguments.length; i++) {
                position.put(arguments[i].getName(), i);
            }
        }

        private void setValue(String paramName, Object paramValue) {
            values[position.get(paramName)] = paramValue;
        }
    }
}
