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

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * Jackson deserializer that handles when Kubernetes returns IP addresses of `None`.
 *
 * @author Álvaro Sánchez-Mariscal
 * @since 1.0.2
 */
@Introspected
public class InetAddressDeserializer extends StdDeserializer<InetAddress> {

    /**
     * Default constructor.
     */
    public InetAddressDeserializer() {
        this(null);
    }

    /**
     * @param vc The value class
     */
    public InetAddressDeserializer(Class<?> vc) {
        super(vc);
    }

    @Override
    public InetAddress deserialize(JsonParser p, DeserializationContext ctxt) throws IOException, JsonProcessingException {
        JsonNode node = p.getCodec().readTree(p);
        String clusterIP = node.asText();
        InetAddress inetAddress = null;
        try {
            inetAddress = InetAddress.getByName(clusterIP);
        } catch (UnknownHostException uhe) {
            //no-op
        }
        return inetAddress;
    }

}
