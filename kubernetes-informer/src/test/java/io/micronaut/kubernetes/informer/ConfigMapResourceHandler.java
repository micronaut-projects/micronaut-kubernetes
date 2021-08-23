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
import jakarta.inject.Singleton;
import org.apache.commons.compress.utils.Lists;

import java.util.List;

@Singleton
public class ConfigMapResourceHandler implements ResourceEventHandler<V1ConfigMap> {

    private final List<V1ConfigMap> added = Lists.newArrayList();
    private final List<V1ConfigMap> updated = Lists.newArrayList();
    private final List<V1ConfigMap> deleted = Lists.newArrayList();

    @Override
    public void onAdd(V1ConfigMap obj) {
        added.add(obj);
    }

    @Override
    public void onUpdate(V1ConfigMap oldObj, V1ConfigMap newObj) {
        updated.add(newObj);
    }

    @Override
    public void onDelete(V1ConfigMap obj, boolean deletedFinalStateUnknown) {
        deleted.add(obj);
    }

    public List<V1ConfigMap> getAdded() {
        return added;
    }

    public List<V1ConfigMap> getUpdated() {
        return updated;
    }

    public List<V1ConfigMap> getDeleted() {
        return deleted;
    }
}
