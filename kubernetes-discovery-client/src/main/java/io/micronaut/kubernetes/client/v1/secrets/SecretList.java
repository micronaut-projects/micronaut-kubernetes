package io.micronaut.kubernetes.client.v1.secrets;

import java.util.Collections;
import java.util.List;

/**
 * Represents a response of type <code>SecretList</code> from the Kubernetes API.
 *
 * @author Álvaro Sánchez-Mariscal
 * @since 1.0.0
 */
public class SecretList {

    private List<Secret> items;

    /**
     * @return The items
     */
    public List<Secret> getItems() {
        if (items == null) {
            return Collections.emptyList();
        }
        return items;
    }

    /**
     * @param items The items
     */
    public void setItems(List<Secret> items) {
        this.items = items;
    }

}
