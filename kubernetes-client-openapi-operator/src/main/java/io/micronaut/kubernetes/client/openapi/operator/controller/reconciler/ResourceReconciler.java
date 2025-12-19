/*
 * Copyright 2017-2025 original authors
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
package io.micronaut.kubernetes.client.openapi.operator.controller.reconciler;

import io.micronaut.kubernetes.client.openapi.common.KubernetesObject;
import io.micronaut.kubernetes.client.openapi.operator.Operator;
import io.micronaut.kubernetes.client.openapi.operator.OperatorResourceLister;
import org.jspecify.annotations.NonNull;

/**
 * The ResourceReconciler defines an interface for reconciling the resource. In general a reconciler works by comparing
 * the desired state of the resource against the actual cluster state, and then perform operations to make the actual
 * cluster state reflect the desired specified state.
 * <p>
 * The ResourceReconciler operations are required to be idempotent as the reconciliation loop is executed every time there
 * is a change of the resource.
 * <p>
 * This interface is meant to be used in combination with the {@link Operator} annotation where the subject of reconciliation
 * is specified:
 * <pre class="code">
 * &#064;Operator(informer = @Informer(apiType = V1ConfigMap.class)))
 * public class MyReconciler implements ResourceReconciler&lt;V1ConfigMap&gt; {
 *   &#064;Override
 *   public Result reconcile(&#064;NonNull Request request, &#064;NonNull OperatorResourceLister&lt;V1ConfigMap&gt; lister) {
 *      Optional&lt;V1ConfigMap&gt; resource = lister.get(request);
 *      // reconcile
 *      return new Result(false);
 *   }
 *}
 * </pre>
 *
 * @author Pavol Gressa
 *
 * @param <ApiType> kubernetes api type
 */
public interface ResourceReconciler<ApiType extends KubernetesObject> {

    /**
     * Reconcile the resource identified by the {@link Request}. This operation is required to be idempotent.
     * <p>
     * The {@link OperatorResourceLister} {@code lister} is used to retrieve resource from the local
     * {@link io.micronaut.kubernetes.client.openapi.informer.cache.Cache}.
     * <p>
     * The {@code lister} returns empty optional if the resource was deleted.
     * Always use <a href="https://kubernetes.io/docs/concepts/overview/working-with-objects/finalizers/">finalizers</a>
     * to properly reconcile on resource deletion.
     *
     * @param request the reconciliation request
     * @param lister the lister for given operator's reconciler
     * @return the reconciliation result
     */
    @NonNull Result reconcile(@NonNull Request request, @NonNull OperatorResourceLister<ApiType> lister);
}
