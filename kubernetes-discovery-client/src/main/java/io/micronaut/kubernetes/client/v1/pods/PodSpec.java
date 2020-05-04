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

import java.util.List;

import io.micronaut.core.annotation.Introspected;
import io.micronaut.kubernetes.client.v1.LocalObjectReference;

/**
 * Specification of the desired behavior of the pod.
 *
 * @author Miguel Ferreira
 * @since 1.0.5
 */
@Introspected
public class PodSpec {

    private String restartPolicy;
    private String serviceAccountName;
    private List<Container> containers;
    private List<Container> initContainers;
    private List<Volume> volumes;
    private List<LocalObjectReference> imagePullSecrets;

    /**
     * @return Restart policy for all containers within the pod. One of Always, OnFailure, Never. Default to Always.
     */
    public String getRestartPolicy() {
        return restartPolicy;
    }

    /**
     * @param restartPolicy Restart policy for all containers within the pod. One of Always, OnFailure, Never. Default to Always.
     */
    public void setRestartPolicy(final String restartPolicy) {
        this.restartPolicy = restartPolicy;
    }

    /**
     * @return ServiceAccountName is the name of the ServiceAccount to use to run this pod.
     */
    public String getServiceAccountName() {
        return serviceAccountName;
    }

    /**
     * @param serviceAccountName ServiceAccountName is the name of the ServiceAccount to use to run this pod.
     */
    public void setServiceAccountName(final String serviceAccountName) {
        this.serviceAccountName = serviceAccountName;
    }

    /**
     * @return List of containers belonging to the pod.
     */
    public List<Container> getContainers() {
        return containers;
    }

    /**
     * @param containers List of containers belonging to the pod.
     */
    public void setContainers(final List<Container> containers) {
        this.containers = containers;
    }

    /**
     * @return List of initialization containers belonging to the pod.
     */
    public List<Container> getInitContainers() {
        return initContainers;
    }

    /**
     * @param initContainers List of initialization containers belonging to the pod.
     */
    public void setInitContainers(final List<Container> initContainers) {
        this.initContainers = initContainers;
    }

    /**
     * @return List of volumes that can be mounted by containers belonging to the pod.
     */
    public List<Volume> getVolumes() {
        return volumes;
    }

    /**
     * @param volumes List of volumes that can be mounted by containers belonging to the pod.
     */
    public void setVolumes(final List<Volume> volumes) {
        this.volumes = volumes;
    }

    /**
     * @return ImagePullSecrets is an optional list of references to secrets in the same namespace to use for pulling any of the images used by this PodSpec.
     */
    public List<LocalObjectReference> getImagePullSecrets() {
        return imagePullSecrets;
    }

    /**
     * @param imagePullSecrets ImagePullSecrets is an optional list of references to secrets in the same namespace to use for pulling any of the images used by this PodSpec.
     */
    public void setImagePullSecrets(final List<LocalObjectReference> imagePullSecrets) {
        this.imagePullSecrets = imagePullSecrets;
    }

    @Override
    public String toString() {
        return "PodSpec{" +
                "restartPolicy='" + restartPolicy + '\'' +
                ", serviceAccountName='" + serviceAccountName + '\'' +
                ", containers=" + containers +
                ", initContainers=" + initContainers +
                ", volumes=" + volumes +
                ", imagePullSecrets=" + imagePullSecrets +
                '}';
    }
}
