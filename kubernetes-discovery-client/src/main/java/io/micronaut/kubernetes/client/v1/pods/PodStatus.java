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

/**
 * Pod status information.
 *
 * @author Álvaro Sánchez-Mariscal
 * @since 1.0.0
 */
public class PodStatus {

    private String phase;
    private String hostIP;
    private String podIP;
    private List<ContainerStatus> containerStatuses;

    /**
     * @return Execution phase (eg: Running)
     */
    public String getPhase() {
        return phase;
    }

    /**
     * @param phase Execution phase (eg: Running)
     */
    public void setPhase(String phase) {
        this.phase = phase;
    }

    /**
     * @return Host IP address
     */
    public String getHostIP() {
        return hostIP;
    }

    /**
     * @param hostIP Host IP address
     */
    public void setHostIP(String hostIP) {
        this.hostIP = hostIP;
    }

    /**
     * @return Pod IP address
     */
    public String getPodIP() {
        return podIP;
    }

    /**
     * @param podIP Pod IP address
     */
    public void setPodIP(String podIP) {
        this.podIP = podIP;
    }

    /**
     * @return Status of all the containers running in this Pod
     */
    public List<ContainerStatus> getContainerStatuses() {
        return containerStatuses;
    }

    /**
     * @param containerStatuses Status of all the containers running in this Pod
     */
    public void setContainerStatuses(List<ContainerStatus> containerStatuses) {
        this.containerStatuses = containerStatuses;
    }

    @Override
    public String toString() {
        return "PodStatus{" +
                "phase='" + phase + '\'' +
                ", hostIP='" + hostIP + '\'' +
                ", podIP='" + podIP + '\'' +
                ", containerStatuses=" + containerStatuses +
                '}';
    }
}
