package io.micronaut.kubernetes.client.v1.secrets;

import io.micronaut.kubernetes.client.v1.Metadata;

import java.util.HashMap;
import java.util.Map;

/**
 * TODO: javadoc
 *
 * @author Álvaro Sánchez-Mariscal
 * @since 1.0.0
 */
public class Secret {

    private Metadata metadata;
    private Map<String, String> data = new HashMap<>();
    private String type;

    /**
     * @return Standard object's metadata. More info: https://git.k8s.io/community/contributors/devel/api-conventions.md#metadata
     */
    public Metadata getMetadata() {
        return metadata;
    }

    /**
     * @param metadata Standard object's metadata. More info: https://git.k8s.io/community/contributors/devel/api-conventions.md#metadata
     */
    public void setMetadata(Metadata metadata) {
        this.metadata = metadata;
    }

    /**
     * @return Data contains the secret data. Each key must consist of alphanumeric characters, '-', '_' or '.'. The
     * serialized form of the secret data is a base64 encoded string, representing the arbitrary (possibly non-string)
     * data value here. Described in https://tools.ietf.org/html/rfc4648#section-4
     */
    public Map<String, String> getData() {
        return data;
    }

    /**
     * @param data Data contains the secret data. Each key must consist of alphanumeric characters, '-', '_' or '.'. The
     * serialized form of the secret data is a base64 encoded string, representing the arbitrary (possibly non-string)
     * data value here. Described in https://tools.ietf.org/html/rfc4648#section-4
     */
    public void setData(Map<String, String> data) {
        this.data = data;
    }

    /**
     * @return The secret type. Only "Opaque" supported
     */
    public String getType() {
        return type;
    }

    /**
     * @param type The secret type. Only "Opaque" supported
     */
    public void setType(String type) {
        this.type = type;
    }

    @Override
    public String toString() {
        return "Secret{" +
                "metadata=" + metadata +
                ", type='" + type + '\'' +
                '}';
    }
}
