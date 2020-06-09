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
 * Volume for pod containers.
 *
 * @author <Miguel Ferreira>
 */
@Introspected
public class Volume {

    private String name;
    private SecretVolumeSource secret;
    private ConfigMapVolumeSource configMap;
    private EmptyDirVolumeSource emptyDir;

    /**
     * @return Volume's name.
     */
    public String getName() {
        return name;
    }

    /**
     * @param name Volume's name.
     */
    public void setName(final String name) {
        this.name = name;
    }

    /**
     * @return Secret that populates this volume.
     */
    public SecretVolumeSource getSecret() {
        return secret;
    }

    /**
     * @param secret Secret that should populate this volume.
     */
    public void setSecret(final SecretVolumeSource secret) {
        this.secret = secret;
    }

    /**
     * @return ConfigMap that populates this volume.
     */
    public ConfigMapVolumeSource getConfigMap() {
        return configMap;
    }

    /**
     * @param configMap ConfigMap that populates this volume.
     */
    public void setConfigMap(final ConfigMapVolumeSource configMap) {
        this.configMap = configMap;
    }

    /**
     * @return EmptyDir represents a temporary directory that shares a pod's lifetime.
     */
    public EmptyDirVolumeSource getEmptyDir() {
        return emptyDir;
    }

    /**
     * @param emptyDir EmptyDir represents a temporary directory that shares a pod's lifetime.
     */
    public void setEmptyDir(final EmptyDirVolumeSource emptyDir) {
        this.emptyDir = emptyDir;
    }

    @Override
    public String toString() {
        return "Volume{" +
                "name='" + name + '\'' +
                ", secret=" + secret +
                ", configMap=" + configMap +
                ", emptyDir=" + emptyDir +
                '}';
    }
}
