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
package io.micronaut.kubernetes.client.v1.services;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.micronaut.kubernetes.client.v1.Port;

import java.net.InetAddress;
import java.util.List;

public class ServiceSpec {

    private List<Port> ports;
    private InetAddress clusterIp;

    public List<Port> getPorts() {
        return ports;
    }

    public void setPorts(List<Port> ports) {
        this.ports = ports;
    }

    @JsonProperty("clusterIP")
    public InetAddress getClusterIp() {
        return clusterIp;
    }

    @JsonProperty("clusterIP")
    public void setClusterIp(InetAddress clusterIp) {
        this.clusterIp = clusterIp;
    }
}
