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
 * A source of environment variables for a container backed by a Secret.
 *
 * @author <Miguel Ferreira>
 */
@Introspected
public class SecretEnvSource {
    private String name;
    private boolean optional;

    /**
     * @return Name of the referent.
     */
    public String getName() {
        return name;
    }

    /**
     * @param name Name of the referent.
     */
    public void setName(final String name) {
        this.name = name;
    }

    /**
     * @return Specifies whether the Secret must be defined
     */
    public boolean isOptional() {
        return optional;
    }

    /**
     * @param optional Whether the Secret must be defined
     */
    public void setOptional(final boolean optional) {
        this.optional = optional;
    }

    @Override
    public String toString() {
        return "SecretEnvSource{" +
                "name='" + name + '\'' +
                ", optional=" + optional +
                '}';
    }
}
