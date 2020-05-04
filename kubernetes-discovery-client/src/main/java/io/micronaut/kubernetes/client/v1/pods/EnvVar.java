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

package io.micronaut.kubernetes.client.v1.pods;

import io.micronaut.core.annotation.Introspected;

/**
 * Environment variable to set in a container.
 *
 * @author <Miguel Ferreira>
 */
@Introspected
public class EnvVar {
    private String name;
    private String value;

    /**
     * @return Name of the environment variable.
     */
    public String getName() {
        return name;
    }

    /**
     * @param name Name of the environment variable.
     */
    public void setName(final String name) {
        this.name = name;
    }

    /**
     * @return Value of the environment variable.
     */
    public String getValue() {
        return value;
    }

    /**
     * @param value Value of the environment variable.
     */
    public void setValue(final String value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return "EnvVar{" +
                "name='" + name + '\'' +
                ", value='" + value + '\'' +
                '}';
    }
}
