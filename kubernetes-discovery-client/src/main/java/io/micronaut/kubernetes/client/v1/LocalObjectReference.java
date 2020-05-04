/*
 * Copyright 2017-2019 original authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.micronaut.kubernetes.client.v1;

import io.micronaut.core.annotation.Introspected;

/**
 * LocalObjectReference.
 *
 * @see <a href="https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.16/#localobjectreference-v1-core">LocalObjectReference v1 core</a>
 *
 * @author <Miguel Ferreira>
 */
@Introspected
public class LocalObjectReference {

    private String name;

    /**
     * @return the name of the object.
     */
    public String getName() {
        return name;
    }

    /**
     * @param name the name of the object.
     */
    public void setName(final String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return "LocalObjectReference{" +
                "name='" + name + '\'' +
                '}';
    }
}
