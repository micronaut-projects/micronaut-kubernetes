package io.micronaut.kubernetes.client.v1.configmaps;

import io.micronaut.kubernetes.client.v1.Metadata;

import java.util.HashMap;
import java.util.Map;

/**
 * A resource that holds key-value pairs of configuration data.
 *
 * @author Álvaro Sánchez-Mariscal
 * @since 1.0.0
 */
public class ConfigMap {

    private Metadata metadata;
    private Map<String, String> data = new HashMap<>();

    /**
     * @return The Metadata
     */
    public Metadata getMetadata() {
        return metadata;
    }

    /**
     * @param metadata The Metadata
     */
    public void setMetadata(Metadata metadata) {
        this.metadata = metadata;
    }

    /**
     * @return A Map where the key is the file name, and the value is a string with all the properties
     */
    public Map<String, String> getData() {
        return data;
    }

    /**
     * @param data A Map where the key is the file name, and the value is a string with all the properties
     */
    public void setData(Map<String, String> data) {
        this.data = data;
    }

    @Override
    public String toString() {
        return "ConfigMap{" +
                "metadata=" + metadata +
                ", data=" + data +
                '}';
    }
}
