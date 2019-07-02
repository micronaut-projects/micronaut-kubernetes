package io.micronaut.kubernetes.client.v1.configmaps;

import java.util.Collections;
import java.util.List;

/**
 * The ConfigMap API resource holds key-value pairs of configuration data that can be consumed in pods or used to store
 * configuration data for system components such as controllers.
 *
 * @author Álvaro Sánchez-Mariscal
 * @since 1.0.0
 */
public class ConfigMapList {

    private List<ConfigMap> items;

    /**
     * @return The items
     */
    public List<ConfigMap> getItems() {
        if (items == null) {
            return Collections.emptyList();
        }
        return items;
    }

    /**
     * @param items The items
     */
    public void setItems(List<ConfigMap> items) {
        this.items = items;
    }
}
