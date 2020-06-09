/*
 * Copyright 2017-2020 original authors
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
package io.micronaut.kubernetes.client.v1.configmaps;

import io.micronaut.core.annotation.Introspected;
import io.micronaut.kubernetes.client.v1.KubernetesObject;

import java.util.HashMap;
import java.util.Map;

/**
 * A resource that holds key-value pairs of configuration data.
 *
 * @author Álvaro Sánchez-Mariscal
 * @since 1.0.0
 */
@Introspected
public class ConfigMap extends KubernetesObject {

    private Map<String, String> data = new HashMap<>();

    /**
     * @return A Map where the key is the file name, and the value is a string with all the properties
     */
    public Map<String, String> getData() {
        return data;
    }

    /**
     * @param data A Map where the key is the file name, and the value is a string with all the properties
     */
    public void setData(Map<String, String> data) {
        this.data = data;
    }

    @Override
    public String toString() {
        return "ConfigMap{" +
                "metadata=" + getMetadata() +
                ", data=" + data +
                '}';
    }
}
