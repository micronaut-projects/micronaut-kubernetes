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
package io.micronaut.kubernetes.client.v1.pods;

import io.micronaut.core.annotation.Introspected;

/**
 * A source of environment variables for a container backed by a ConfigMap.
 *
 * @author <Miguel Ferreira>
 */
@Introspected
public class ConfigMapEnvSource {
    private String name;
    private boolean optional;

    /**
     * @return the name of the ConfigMap
     */
    public String getName() {
        return name;
    }

    /**
     * @param name the name of the ConfigMap
     */
    public void setName(final String name) {
        this.name = name;
    }

    /**
     * @return Whether the ConfigMap must be defined
     */
    public boolean isOptional() {
        return optional;
    }

    /**
     * @param optional Specifies whether the ConfigMap must be defined
     */
    public void setOptional(final boolean optional) {
        this.optional = optional;
    }

    @Override
    public String toString() {
        return "ConfigMapEnvSource{" +
                "name='" + name + '\'' +
                ", optional=" + optional +
                '}';
    }
}
