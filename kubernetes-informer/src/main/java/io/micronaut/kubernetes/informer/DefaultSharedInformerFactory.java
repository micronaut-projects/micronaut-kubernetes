/*
 * Copyright 2021 original authors
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
package io.micronaut.kubernetes.informer;

import io.kubernetes.client.common.KubernetesListObject;
import io.kubernetes.client.common.KubernetesObject;
import io.kubernetes.client.informer.SharedInformerFactory;
import io.kubernetes.client.openapi.ApiClient;
import io.micronaut.context.ApplicationContext;
import io.micronaut.context.BeanResolutionContext;
import io.micronaut.context.annotation.BootstrapContextCompatible;
import io.micronaut.inject.BeanDefinition;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.List;
import java.util.concurrent.ExecutorService;

/**
 * @author Pavol Gressa
 * @since 2.5
 */
@Singleton
@BootstrapContextCompatible
public class DefaultSharedInformerFactory extends SharedInformerFactory {

    private final List<InformerResourceEventHandler<? extends KubernetesObject, ? extends KubernetesListObject>> resourceEventHandlerList;
    private ApplicationContext applicationContext;


    public DefaultSharedInformerFactory(ApiClient client,
                                        @Named("io") ExecutorService threadPool,
                                        List<InformerResourceEventHandler<? extends KubernetesObject, ? extends KubernetesListObject>> resourceEventHandlerList,
                                        ApplicationContext applicationContext) {
        super(client, threadPool);
        this.resourceEventHandlerList = resourceEventHandlerList;
        this.applicationContext = applicationContext;
        registerInformers();
    }

    private void registerInformers() {
        resourceEventHandlerList.forEach(resourceEventHandler -> {
            BeanDefinition<?> definition = applicationContext.getBeanDefinition(resourceEventHandler.getClass());
            Type[] genericInterfaces = definition.getBeanType().getGenericInterfaces();
            if (genericInterfaces.length > 0) {
                Type type = genericInterfaces[0];
                if (type instanceof ParameterizedType) {
                    ParameterizedType pt = (ParameterizedType) type;
                    Type[] typeArguments = pt.getActualTypeArguments();
                    if (typeArguments[0] instanceof KubernetesObject &&
                            typeArguments[1] instanceof KubernetesListObject) {
                        KubernetesObject kubernetesObject = (KubernetesObject) typeArguments[0];
                        KubernetesListObject kubernetesListObject = (KubernetesListObject) typeArguments[1];
                    }
                }
            }
        });

    }
}
