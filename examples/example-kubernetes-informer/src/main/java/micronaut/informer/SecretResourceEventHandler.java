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
import io.micronaut.kubernetes.client.informer.Informer;
import jakarta.inject.Singleton;

import java.util.HashMap;
import java.util.Map;

@Singleton
@Informer(apiType = V1Secret.class, apiListType = V1SecretList.class)
public class SecretResourceEventHandler implements ResourceEventHandler<V1Secret> {

    private Map<String, V1Secret> v1SecretMap = new HashMap<>();

    @Override
    public void onAdd(V1Secret obj) {
        v1SecretMap.put(obj.getMetadata().getName(), obj);
    }

    @Override
    public void onUpdate(V1Secret oldObj, V1Secret newObj) {
        v1SecretMap.put(newObj.getMetadata().getName(), newObj);
    }

    @Override
    public void onDelete(V1Secret obj, boolean deletedFinalStateUnknown) {
        v1SecretMap.remove(obj.getMetadata().getName());
    }

    public Map<String, V1Secret> getV1SecretMap() {
        return v1SecretMap;
    }
}
