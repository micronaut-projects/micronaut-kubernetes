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
package io.micronaut.kubernetes.client.v1.endpoints;

import io.micronaut.core.annotation.Introspected;
import io.micronaut.kubernetes.client.v1.Address;
import io.micronaut.kubernetes.client.v1.Port;

import java.util.List;

/**
 * A group of addresses with a common set of ports.
 *
 * @author Álvaro Sánchez-Mariscal
 * @since 1.0.0
 */
@Introspected
public class EndpointsSubset {

    private List<Address> addresses;
    private List<Port> ports;

    /**
     *
     * @return IP addresses which offer the related ports that are marked as ready.
     */
    public List<Address> getAddresses() {
        return addresses;
    }

    /**
     *
     * @param addresses IP addresses
     */
    public void setAddresses(List<Address> addresses) {
        this.addresses = addresses;
    }

    /**
     *
     * @return Port numbers available on the related IP addresses.
     */
    public List<Port> getPorts() {
        return ports;
    }

    /**
     *
     * @param ports Port numbers available.
     */
    public void setPorts(List<Port> ports) {
        this.ports = ports;
    }

    @Override
    public String toString() {
        return "EndpointsSubset{" +
                "addresses=" + addresses +
                ", ports=" + ports +
                '}';
    }
}
