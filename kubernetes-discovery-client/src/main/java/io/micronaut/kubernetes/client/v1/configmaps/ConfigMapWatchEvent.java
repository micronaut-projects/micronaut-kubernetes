package io.micronaut.kubernetes.client.v1.configmaps;

/**
 * TODO: javadoc
 *
 * @author Álvaro Sánchez-Mariscal
 * @since 1.0.0
 */
public class ConfigMapWatchEvent {

    private EventType type;
    private ConfigMap object;

    public ConfigMapWatchEvent() {
    }

    public EventType getType() {
        return type;
    }

    public void setType(EventType type) {
        this.type = type;
    }

    public ConfigMap getObject() {
        return object;
    }

    public void setObject(ConfigMap object) {
        this.object = object;
    }

    public enum EventType {
        ADDED, MODIFIED, DELETED, ERROR
    }

    @Override
    public String toString() {
        return "ConfigMapWatchEvent{" +
                "type=" + type +
                ", object=" + object +
                '}';
    }
}
