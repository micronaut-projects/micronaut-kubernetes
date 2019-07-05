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

package io.micronaut.kubernetes.client.v1.configmaps;

import java.util.Collections;
import java.util.List;

/**
 * The ConfigMap API resource holds key-value pairs of configuration data that can be consumed in pods or used to store
 * configuration data for system components such as controllers.
 *
 * @author Álvaro Sánchez-Mariscal
 * @since 1.0.0
 */
public class ConfigMapList {

    private List<ConfigMap> items;

    /**
     * @return The items
     */
    public List<ConfigMap> getItems() {
        if (items == null) {
            return Collections.emptyList();
        }
        return items;
    }

    /**
     * @param items The items
     */
    public void setItems(List<ConfigMap> items) {
        this.items = items;
    }
}
