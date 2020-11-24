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
package io.micronaut.kubernetes.client.v1.services;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.micronaut.core.annotation.Introspected;
import io.micronaut.kubernetes.client.v1.InetAddressDeserializer;
import io.micronaut.kubernetes.client.v1.Port;

import java.net.InetAddress;
import java.util.List;

/**
 * @see <a href="https://git.k8s.io/community/contributors/devel/api-conventions.md#spec-and-status">Spec and Status</a>
 *
 * @see <a href="https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.19/#servicespec-v1-core">Service Spec v1</a>
 *
 * @author Álvaro Sánchez-Mariscal
 * @since 1.0.0
 */
@Introspected
public class ServiceSpec {

    private List<Port> ports;
    private InetAddress clusterIp;
    private String type;
    private String externalName;

    /**
     *
     * @return The list of ports that are exposed by this service.
     */
    public List<Port> getPorts() {
        return ports;
    }

    /**
     *
     * @param ports The list of ports that are exposed by this service
     */
    public void setPorts(List<Port> ports) {
        this.ports = ports;
    }

    /**
     *
     * @return The IP address of the service; usually assigned randomly by the master.
     */
    @JsonProperty("clusterIP")
    @JsonDeserialize(using = InetAddressDeserializer.class)
    public InetAddress getClusterIp() {
        return clusterIp;
    }

    /**
     *
     * @param clusterIp The IP address of the service.
     */
    @JsonProperty("clusterIP")
    public void setClusterIp(InetAddress clusterIp) {
        this.clusterIp = clusterIp;
    }

    /**
     *
     * @return The type of service; as of 1.19 the valid options are ExternalName, ClusterIP, NodePort, and LoadBalancer.
     */
    public String getType() {
        return type;
    }

    /**
     *
     * @param type The type of service.
     */
    public void setType(String type) {
        this.type = type;
    }

    /**
     *
     * @return The external name of service.
     */
    public String getExternalName() {
        return externalName;
    }

    /**
     *
     * @param externalName The external name of service.
     */
    public void setExternalName(String externalName) {
        this.externalName = externalName;
    }
}
