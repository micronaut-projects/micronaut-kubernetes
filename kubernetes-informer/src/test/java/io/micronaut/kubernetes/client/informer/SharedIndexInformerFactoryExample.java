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


import io.kubernetes.client.informer.SharedIndexInformer;
import io.kubernetes.client.openapi.models.V1ConfigMap;
import io.kubernetes.client.openapi.models.V1ConfigMapList;

public class SharedIndexInformerFactoryExample {

    private DefaultSharedIndexInformerFactory factory;

    public SharedIndexInformerFactoryExample(DefaultSharedIndexInformerFactory factory) {
        this.factory = factory;
    }

    public SharedIndexInformer<V1ConfigMap> createInformer() {
        //tag::create[]
        SharedIndexInformer<V1ConfigMap> sharedIndexInformer = factory.sharedIndexInformerFor(
                V1ConfigMap.class, // <1>
                V1ConfigMapList.class, // <2>
                "configmaps", // <3>
                "",  // <4>
                "default",  // <5>
                null,
                null
        );
        //end::create[]
        return sharedIndexInformer;
    }

    public SharedIndexInformer<V1ConfigMap> getInformer(String namespace) {
        //tag::get[]
        SharedIndexInformer<V1ConfigMap> sharedIndexInformer = factory.getExistingSharedIndexInformer(
                "default", // <1>
                V1ConfigMap.class); // <2>
        //end::get[]
        return sharedIndexInformer;
    }


}
