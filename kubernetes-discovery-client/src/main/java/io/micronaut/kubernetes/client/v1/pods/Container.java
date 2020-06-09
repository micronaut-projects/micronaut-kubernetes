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

import java.util.List;

import io.micronaut.core.annotation.Introspected;

/**
 * Represents a container.
 *
 * @see <a href="https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.18/#container-v1-core">Container v1 core</a>.
 *
 * @author <Miguel Ferreira>
 */
@Introspected
public class Container {

    private String name;
    private List<String> args;
    private List<String> command;
    private List<EnvVar> env;
    private List<EnvFromSource> envFrom;
    private String image;
    private String imagePullPolicy;
    private List<VolumeMount> volumeMounts;

    /**
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * @param name the name
     */
    public void setName(final String name) {
        this.name = name;
    }

    /**
     * @return Arguments to the entrypoint.
     */
    public List<String> getArgs() {
        return args;
    }

    /**
     * @param args Arguments to the entrypoint.
     */
    public void setArgs(final List<String> args) {
        this.args = args;
    }

    /**
     * @return Entrypoint array. The commands that are executed.
     */
    public List<String> getCommand() {
        return command;
    }

    /**
     * @param command Entrypoint array. The commands that are executed.
     */
    public void setCommand(final List<String> command) {
        this.command = command;
    }

    /**
     * @return List of environment variables to set in the container.
     */
    public List<EnvVar> getEnv() {
        return env;
    }

    /**
     * @param env List of environment variables to set in the container.
     */
    public void setEnv(final List<EnvVar> env) {
        this.env = env;
    }

    /**
     * @return List of sources to populate environment variables in the container.
     */
    public List<EnvFromSource> getEnvFrom() {
        return envFrom;
    }

    /**
     * @param envFrom List of sources to populate environment variables in the container.
     */
    public void setEnvFrom(final List<EnvFromSource> envFrom) {
        this.envFrom = envFrom;
    }

    /**
     * @return Docker image name, including tag
     */
    public String getImage() {
        return image;
    }

    /**
     * @param image Docker image name, including tag
     */
    public void setImage(final String image) {
        this.image = image;
    }

    /**
     * @return Image pull policy. One of Always, Never, IfNotPresent. Defaults to Always if :latest tag is specified, or IfNotPresent otherwise
     */
    public String getImagePullPolicy() {
        return imagePullPolicy;
    }

    /**
     * @param imagePullPolicy Image pull policy. One of Always, Never, IfNotPresent. Defaults to Always if :latest tag is specified, or IfNotPresent otherwise
     */
    public void setImagePullPolicy(final String imagePullPolicy) {
        this.imagePullPolicy = imagePullPolicy;
    }

    /**
     * @return Pod volumes to mount into the container's filesystem
     */
    public List<VolumeMount> getVolumeMounts() {
        return volumeMounts;
    }

    /**
     * @param volumeMounts Pod volumes to mount into the container's filesystem
     */
    public void setVolumeMounts(final List<VolumeMount> volumeMounts) {
        this.volumeMounts = volumeMounts;
    }

    @Override
    public String toString() {
        return "Container{" +
                "name='" + name + '\'' +
                ", args=" + args +
                ", command=" + command +
                ", env=" + env +
                ", envFrom=" + envFrom +
                ", image='" + image + '\'' +
                ", imagePullPolicy='" + imagePullPolicy + '\'' +
                ", volumeMounts=" + volumeMounts +
                '}';
    }
}
