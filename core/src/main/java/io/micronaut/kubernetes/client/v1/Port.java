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

package io.micronaut.kubernetes.client.v1;


/**
 * @see <a href="https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.13/#serviceport-v1-core">Service Port v1 core</a>.
 *
 * @author Álvaro Sánchez-Mariscal
 * @since 1.0.0
 */
public class Port {

    private String protocol;
    private int port;
    private int targetPort = -1;
    private int nodePort = -1;

    /**
     *
     * @return The IP protocol for this port.
     */
    public String getProtocol() {
        return protocol;
    }

    /**
     *
     * @return Number or name of the port to access on the pods targeted by the service.
     */
    public int getTargetPort() {
        return this.targetPort;
    }

    /**
     *
     * @param protocol The IP protocol for this port.
     */
    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }

    /**
     *
     * @param nodePort The port on each node on which this service is exposed when type=NodePort or LoadBalancer.
     */
    public void setNodePort(int nodePort) {
        this.nodePort = nodePort;
    }

    /**
     *
     * @return The port that will be exposed by this service.
     */
    public int getPort() {
        return port;
    }

    /**
     *
     * @param port The port that will be exposed by this service.
     */
    public void setPort(int port) {
        this.port = port;
    }

    /**
     *
     * @param targetPort Number or name of the port to access on the pods targeted by the service.
     */
    public void setTargetPort(int targetPort) {
        this.targetPort = targetPort;
    }

    /**
     *
     * @return The port on each node on which this service is exposed when type=NodePort or LoadBalancer.
     */
    public int getNodePort() {
        return nodePort;
    }

    @Override
    public String toString() {
        return "Port{" +
                "protocol='" + protocol + '\'' +
                ", port=" + port +
                ", targetPort=" + targetPort +
                ", nodePort=" + nodePort +
                '}';
    }
}
