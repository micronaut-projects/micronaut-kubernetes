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
import io.micronaut.context.annotation.Requires;
import jakarta.inject.Singleton;
import org.apache.commons.compress.utils.Lists;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

//tag::listener[]
@Requires(property = "spec.name", value = "ConfigMapInformerSpec")
@Singleton
@Informer(apiType = V1ConfigMap.class, apiListType = V1ConfigMapList.class, resourcePlural = "configmaps") // <1>
public class ConfigMapInformer implements ResourceEventHandler<V1ConfigMap> { // <2>

    //end::listener[]
    public static final Logger LOG = LoggerFactory.getLogger(ConfigMapInformer.class);

    private final List<V1ConfigMap> added = Lists.newArrayList();
    private final List<V1ConfigMap> updated = Lists.newArrayList();
    private final List<V1ConfigMap> deleted = Lists.newArrayList();

    public List<V1ConfigMap> getAdded() {
        return added;
    }

    public List<V1ConfigMap> getUpdated() {
        return updated;
    }

    public List<V1ConfigMap> getDeleted() {
        return deleted;
    }

    //tag::listener[]
    @Override
    public void onAdd(V1ConfigMap obj) {
        //end::listener[]
        LOG.info("ADDED CONFIG MAP: {}", obj);
        added.add(obj);
        //tag::listener[]
    }


    @Override
    public void onUpdate(V1ConfigMap oldObj, V1ConfigMap newObj) {
        //end::listener[]
        LOG.info("UPDATED CONFIG MAP: {}", newObj);
        updated.add(newObj);
        //tag::listener[]
    }

    @Override
    public void onDelete(V1ConfigMap obj, boolean deletedFinalStateUnknown) {
        //end::listener[]
        LOG.info("DELETED CONFIG MAP: {}", obj);
        deleted.add(obj);
        //tag::listener[]
    }
}
//end::listener[]