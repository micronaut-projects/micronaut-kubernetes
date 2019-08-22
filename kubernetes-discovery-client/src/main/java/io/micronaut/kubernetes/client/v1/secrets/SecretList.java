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

package io.micronaut.kubernetes.client.v1.secrets;

import io.micronaut.core.annotation.Introspected;

import java.util.Collections;
import java.util.List;

/**
 * Represents a response of type <code>SecretList</code> from the Kubernetes API.
 *
 * @author Álvaro Sánchez-Mariscal
 * @since 1.0.0
 */
@Introspected
public class SecretList {

    private List<Secret> items;

    /**
     * @return The items
     */
    public List<Secret> getItems() {
        if (items == null) {
            return Collections.emptyList();
        }
        return items;
    }

    /**
     * @param items The items
     */
    public void setItems(List<Secret> items) {
        this.items = items;
    }

}
