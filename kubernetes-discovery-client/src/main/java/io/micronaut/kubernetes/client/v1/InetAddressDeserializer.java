package io.micronaut.kubernetes.client.v1;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.FromStringDeserializer;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * TODO: javadoc
 *
 * @author Álvaro Sánchez-Mariscal
 * @since 1.0.0
 */
public class InetAddressDeserializer extends StdDeserializer<InetAddress> {

    public InetAddressDeserializer() {
        this(null);
    }


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
