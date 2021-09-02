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
import io.kubernetes.client.openapi.models.V1ClusterRole;
import io.kubernetes.client.openapi.models.V1ClusterRoleList;
import io.micronaut.context.annotation.Requires;
import jakarta.inject.Singleton;
import org.apache.commons.compress.utils.Lists;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

@Requires(property = "spec.name", value = "ClusterRoleInformerSpec")
//tag::handler[]
@Singleton
@Informer(apiType = V1ClusterRole.class, apiListType = V1ClusterRoleList.class)
public class ClusterRoleInformer implements ResourceEventHandler<V1ClusterRole> {

    //end::handler[]
    public static final Logger LOG = LoggerFactory.getLogger(ClusterRoleInformer.class);

    private final List<V1ClusterRole> added = Lists.newArrayList();
    private final List<V1ClusterRole> updated = Lists.newArrayList();
    private final List<V1ClusterRole> deleted = Lists.newArrayList();

    public List<V1ClusterRole> getAdded() {
        return added;
    }

    public List<V1ClusterRole> getUpdated() {
        return updated;
    }

    public List<V1ClusterRole> getDeleted() {
        return deleted;
    }

    //tag::handler[]
    @Override
    public void onAdd(V1ClusterRole obj) {
        //end::handler[]
        LOG.info("ADDED V1ClusterRole: {}", obj);
        added.add(obj);
        //tag::handler[]
    }

    @Override
    public void onUpdate(V1ClusterRole oldObj, V1ClusterRole newObj) {
        //end::handler[]
        LOG.info("UPDATED V1ClusterRole: {}", newObj);
        updated.add(newObj);
        //tag::handler[]
    }

    @Override
    public void onDelete(V1ClusterRole obj, boolean deletedFinalStateUnknown) {
        //end::handler[]
        LOG.info("DELETED V1ClusterRole: {}", obj);
        deleted.add(obj);
        //tag::handler[]
    }
}
//end::handler[]
