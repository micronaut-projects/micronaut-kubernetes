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

import io.kubernetes.client.common.KubernetesObject;
import io.kubernetes.client.informer.ResourceEventHandler;
import io.kubernetes.client.informer.SharedInformerFactory;
import io.micronaut.aop.ConstructorInterceptor;
import io.micronaut.aop.ConstructorInvocationContext;
import io.micronaut.aop.InterceptorBean;
import io.micronaut.aop.InvocationContext;
import io.micronaut.core.annotation.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Type;
import java.util.Arrays;

/**
 * @author Pavol Gressa
 * @since 2.5
 */
@InterceptorBean(Informer.class)
public class InformerConstructorInterceptor implements ConstructorInterceptor<ResourceEventHandler<? extends KubernetesObject>> {

    private static final Logger LOG = LoggerFactory.getLogger(InformerConstructorInterceptor.class);

    private final SharedInformerFactory sharedInformerFactory;

    public InformerConstructorInterceptor(SharedInformerFactory sharedInformerFactory) {
        this.sharedInformerFactory = sharedInformerFactory;
    }

    @Override
    public @NonNull ResourceEventHandler<? extends KubernetesObject> intercept(@NonNull ConstructorInvocationContext<ResourceEventHandler<? extends KubernetesObject>> context) {
        LOG.info(">>>>>>   ConstructorInvocationContext " + Arrays.toString(context.getDeclaringType().getGenericInterfaces()));

        final Type[] genericInterfaces = context.getDeclaringType().getGenericInterfaces();
//        genericInterfaces[0].getTypeName()
//
//        SharedIndexInformer<V1Node> nodeInformer =
//                factory.sharedIndexInformerFor(
//                        (CallGeneratorParams params) -> {
//                            return coreV1Api.listNodeCall(
//                                    null,
//                                    null,
//                                    null,
//                                    null,
//                                    null,
//                                    null,
//                                    params.resourceVersion,
//                                    null,
//                                    params.timeoutSeconds,
//                                    params.watch,
//                                    null);
//                        },
//                        V1Node.class,
//                        V1NodeList.class);
//
//

        return context.proceed();
    }
}
