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
package io.micronaut.kubernetes.client.v1;

import io.micronaut.core.annotation.Introspected;

/**
 * @see <a href="https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.13/#serviceport-v1-core">Service Port v1 core</a>.
 *
 * @author Álvaro Sánchez-Mariscal
 * @since 1.0.0
 */
@Introspected
public class Port {

    private String name;
    private String protocol;
    private int port;
    private String targetPort;
    private int nodePort = -1;

    /**
     * @return The name of this port within the service
     */
    public String getName() {
        return name;
    }

    /**
     * @param name The name of this port within the service
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * @return The IP protocol for this port.
     */
    public String getProtocol() {
        return protocol;
    }

    /**
     * @return Number or name of the port to access on the pods targeted by the service.
     */
    public String getTargetPort() {
        return this.targetPort;
    }

    /**
     * @param protocol The IP protocol for this port.
     */
    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }

    /**
     * @param nodePort The port on each node on which this service is exposed when type=NodePort or LoadBalancer.
     */
    public void setNodePort(int nodePort) {
        this.nodePort = nodePort;
    }

    /**
     * @return The port that will be exposed by this service.
     */
    public int getPort() {
        return port;
    }

    /**
     * @param port The port that will be exposed by this service.
     */
    public void setPort(int port) {
        this.port = port;
    }

    /**
     * @param targetPort Number or name of the port to access on the pods targeted by the service.
     */
    public void setTargetPort(String targetPort) {
        this.targetPort = targetPort;
    }

    /**
     * @return The port on each node on which this service is exposed when type=NodePort or LoadBalancer.
     */
    public int getNodePort() {
        return nodePort;
    }

    @Override
    public String toString() {
        return "Port{" +
                "name='" + name + '\'' +
                ", protocol='" + protocol + '\'' +
                ", port=" + port +
                ", targetPort=" + targetPort +
                ", nodePort=" + nodePort +
                '}';
    }

    /**
     * Attempts to guess whether this port should be connected to using SSL. By default, port numbers ending in 443
     * or port named "https" are considered secure
     *
     * @return Whether the port is considered secure
     */
    public boolean isSecure() {
        String port = String.valueOf(this.port);
        return port.endsWith("443") || "https".equals(this.name);
    }
}
