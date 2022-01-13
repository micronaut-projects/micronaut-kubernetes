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
package io.micronaut.kubernetes.client.operator;
//tag::reconciler[]
import io.kubernetes.client.extended.controller.reconciler.Request;
import io.kubernetes.client.extended.controller.reconciler.Result;
import io.kubernetes.client.openapi.models.V1ConfigMap;
import io.kubernetes.client.openapi.models.V1ConfigMapList;
import io.micronaut.core.annotation.NonNull;
import io.micronaut.kubernetes.client.informer.Informer;
//end::reconciler[]
import io.micronaut.context.annotation.Requires;
import io.micronaut.kubernetes.client.operator.event.LeaseAcquiredEvent;
import io.micronaut.runtime.event.annotation.EventListener;

import java.util.ArrayList;
import java.util.List;
//tag::reconciler[]
import java.util.Optional;
//end::reconciler[]
import java.util.concurrent.atomic.AtomicBoolean;

@Requires(property = "spec.name", value = "ConfigMapResourceReconcilerSpec")
//tag::reconciler[]

@Operator(informer = @Informer(apiType = V1ConfigMap.class, apiListType = V1ConfigMapList.class)) // <1>
public class ConfigMapResourceReconciler implements ResourceReconciler<V1ConfigMap> { // <2>

    //end::reconciler[]
    List<String> requestList = new ArrayList<>();
    AtomicBoolean leaseAcquired = new AtomicBoolean(false);

    //tag::reconciler[]
    @Override
    @NonNull
    public Result reconcile(@NonNull Request request, @NonNull OperatorResourceLister<V1ConfigMap> lister) { // <3>
        Optional<V1ConfigMap> resource = lister.get(request); // <4>
        // .. reconcile  <5>
        //end::reconciler[]
        resource.ifPresent(v1ConfigMap -> {
                    requestList.add(v1ConfigMap.getMetadata().getName());
                }
        );
        //tag::reconciler[]
        return new Result(false); // <6>
    }
    //end::reconciler[]

    @EventListener
    public void onBecomingLeader(LeaseAcquiredEvent leaseAcquiredEvent) {
        leaseAcquired.set(true);
    }
//tag::reconciler[]
}
//end::reconciler[]
