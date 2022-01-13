/*
 * Copyright 2017-2021 original authors
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
package micronaut.operator;

import io.kubernetes.client.extended.controller.reconciler.Request;
import io.kubernetes.client.extended.controller.reconciler.Result;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.openapi.models.V1ConfigMap;
import io.kubernetes.client.openapi.models.V1ConfigMapList;
import io.micronaut.context.annotation.Requires;
import io.micronaut.context.env.Environment;
import io.micronaut.core.annotation.NonNull;
import io.micronaut.kubernetes.client.informer.Informer;
import io.micronaut.kubernetes.client.operator.Operator;
import io.micronaut.kubernetes.client.operator.OperatorResourceLister;
import io.micronaut.kubernetes.client.operator.ResourceReconciler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Requires(env = Environment.KUBERNETES)
@Operator(informer = @Informer(apiType = V1ConfigMap.class, apiListType = V1ConfigMapList.class))
public class ConfigMapReconciler implements ResourceReconciler<V1ConfigMap> {

    private static final Logger LOG = LoggerFactory.getLogger(ConfigMapReconciler.class);

    private final CoreV1Api coreV1Api;

    public ConfigMapReconciler(CoreV1Api coreV1Api) {
        this.coreV1Api = coreV1Api;
    }

    @Override
    @NonNull
    public Result reconcile(@NonNull Request request, @NonNull OperatorResourceLister<V1ConfigMap> lister) {
        LOG.info("Reconciling " + request);
        Optional<V1ConfigMap> v1ConfigMapOptional = lister.get(request);
        if (v1ConfigMapOptional.isPresent()) {
            V1ConfigMap v1ConfigMap = v1ConfigMapOptional.get();

            if (v1ConfigMap.getMetadata().getAnnotations() == null) {
                v1ConfigMap.getMetadata().setAnnotations(new HashMap<>());
            }

            Map<String, String> annotations = v1ConfigMap.getMetadata().getAnnotations();

            if (!annotations.containsKey("io.micronaut.operator")) {
                annotations.put("io.micronaut.operator", "processed");
                String name = v1ConfigMap.getMetadata().getName();
                String namespace = v1ConfigMap.getMetadata().getNamespace();
                try {
                    coreV1Api.replaceNamespacedConfigMap(name, namespace, v1ConfigMap, null, null, null);
                } catch (ApiException e) {
                    LOG.error("Failed to update config map", e);
                    return new Result(true, Duration.ofSeconds(2));
                }
            }
        }
        return new Result(false);
    }
}
