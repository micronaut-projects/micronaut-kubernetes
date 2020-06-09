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
 * Represents an empty directory for a pod. Empty directory volumes support ownership management and SELinux relabeling.
 *
 * @author <Miguel Ferreira>
 */
@Introspected
public class EmptyDirVolumeSource {

    private String medium = "";

    /**
     * @return What type of storage medium should back this directory. The default is "" which means to use the node's default medium. Must be an empty string (default) or Memory.
     */
    public String getMedium() {
        return medium;
    }

    /**
     * @param medium What type of storage medium should back this directory. The default is "" which means to use the node's default medium. Must be an empty string (default) or Memory.
     */
    public void setMedium(final String medium) {
        this.medium = medium;
    }

    @Override
    public String toString() {
        return "EmptyDirVolumeSource{" +
                "medium='" + medium + '\'' +
                '}';
    }
}
