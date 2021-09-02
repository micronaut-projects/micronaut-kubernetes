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
package io.micronaut.kubernetes.client.informer;

import io.kubernetes.client.informer.ResourceEventHandler;
import io.kubernetes.client.openapi.models.V1ConfigMap;
import io.kubernetes.client.openapi.models.V1ConfigMapList;
import io.micronaut.context.annotation.Requires;
import jakarta.inject.Singleton;
import org.apache.commons.compress.utils.Lists;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

@Requires(property = "spec.name", value = "ConfigMapInformerSpec")
//tag::handler[]
@Singleton
@Informer(apiType = V1ConfigMap.class, apiListType = V1ConfigMapList.class) // <1>
public class ConfigMapInformer implements ResourceEventHandler<V1ConfigMap> { // <2>

    //end::handler[]
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

    //tag::handler[]
    @Override
    public void onAdd(V1ConfigMap obj) {
        //end::handler[]
        LOG.info("ADDED CONFIG MAP: {}", obj);
        added.add(obj);
        //tag::handler[]
    }

    @Override
    public void onUpdate(V1ConfigMap oldObj, V1ConfigMap newObj) {
        //end::handler[]
        LOG.info("UPDATED CONFIG MAP: {}", newObj);
        updated.add(newObj);
        //tag::handler[]
    }

    @Override
    public void onDelete(V1ConfigMap obj, boolean deletedFinalStateUnknown) {
        //end::handler[]
        LOG.info("DELETED CONFIG MAP: {}", obj);
        deleted.add(obj);
        //tag::handler[]
    }
}
//end::handler[]
