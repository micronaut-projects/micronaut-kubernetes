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
 * VolumeMount describes a mounting of a Volume within a container.
 *
 * @author <Miguel Ferreira>
 */
@Introspected
public class VolumeMount {
    private String name;
    private String mountPath;

    /**
     * @return This must match the Name of a Volume.
     */
    public String getName() {
        return name;
    }

    /**
     * @param name This must match the Name of a Volume.
     */
    public void setName(final String name) {
        this.name = name;
    }

    /**
     * @return Path within the container at which the volume should be mounted. Must not contain ':'.
     */
    public String getMountPath() {
        return mountPath;
    }

    /**
     * @param mountPath Path within the container at which the volume should be mounted. Must not contain ':'.
     */
    public void setMountPath(final String mountPath) {
        this.mountPath = mountPath;
    }

    @Override
    public String toString() {
        return "VolumeMount{" +
                "name='" + name + '\'' +
                ", mountPath='" + mountPath + '\'' +
                '}';
    }
}
