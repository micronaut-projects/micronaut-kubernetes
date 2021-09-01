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

import io.kubernetes.client.informer.ResourceEventHandler;
import io.kubernetes.client.openapi.models.V1ConfigMap;
import io.kubernetes.client.openapi.models.V1ConfigMapList;
import io.kubernetes.client.openapi.models.V1Namespace;
import io.kubernetes.client.openapi.models.V1NamespaceList;
import io.micronaut.context.annotation.Requires;
import jakarta.inject.Singleton;
import org.apache.commons.compress.utils.Lists;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

//tag::listener[]
@Requires(property = "spec.name", value = "NamespaceInformerSpec")
@Singleton
@Informer(apiType = V1Namespace.class, apiListType = V1NamespaceList.class, resourcePlural = "namespaces", namespace = Informer.ALL_NAMESPACES) // <1>
public class NamespaceInformer implements ResourceEventHandler<V1Namespace> { // <2>

    //end::listener[]
    public static final Logger LOG = LoggerFactory.getLogger(NamespaceInformer.class);

    private final List<V1Namespace> added = Lists.newArrayList();
    private final List<V1Namespace> updated = Lists.newArrayList();
    private final List<V1Namespace> deleted = Lists.newArrayList();

    public List<V1Namespace> getAdded() {
        return added;
    }

    public List<V1Namespace> getUpdated() {
        return updated;
    }

    public List<V1Namespace> getDeleted() {
        return deleted;
    }

    //tag::listener[]
    @Override
    public void onAdd(V1Namespace obj) {
        //end::listener[]
        LOG.info("ADDED NAMESPACE: {}", obj);
        added.add(obj);
        //tag::listener[]
    }


    @Override
    public void onUpdate(V1Namespace oldObj, V1Namespace newObj) {
        //end::listener[]
        LOG.info("UPDATED NAMESPACE: {}", newObj);
        updated.add(newObj);
        //tag::listener[]
    }

    @Override
    public void onDelete(V1Namespace obj, boolean deletedFinalStateUnknown) {
        //end::listener[]
        LOG.info("DELETED NAMESPACE: {}", obj);
        deleted.add(obj);
        //tag::listener[]
    }
}
//end::listener[]