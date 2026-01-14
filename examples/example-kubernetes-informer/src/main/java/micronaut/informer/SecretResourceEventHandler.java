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
package micronaut.informer;

import io.kubernetes.client.informer.ResourceEventHandler;
import io.kubernetes.client.openapi.models.V1Secret;
import io.kubernetes.client.openapi.models.V1SecretList;
import io.micronaut.context.annotation.Context;
import io.micronaut.kubernetes.client.informer.Informer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Context
@Informer(apiType = V1Secret.class, apiListType = V1SecretList.class)
public class SecretResourceEventHandler implements ResourceEventHandler<V1Secret> {

    private static final Logger LOG = LoggerFactory.getLogger(SecretResourceEventHandler.class);

    @Override
    public void onAdd(V1Secret obj) {
        LOG.info("{} secret added!", obj.getMetadata().getName());
    }

    @Override
    public void onUpdate(V1Secret oldObj, V1Secret newObj) {
        LOG.info("{} secret updated!", oldObj.getMetadata().getName());
    }

    @Override
    public void onDelete(V1Secret obj, boolean deletedFinalStateUnknown) {
        LOG.info("{} secret deleted!", obj.getMetadata().getName());
    }
}
