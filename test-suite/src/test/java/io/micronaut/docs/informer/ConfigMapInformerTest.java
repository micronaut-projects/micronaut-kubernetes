package io.micronaut.docs.informer;

import io.kubernetes.client.openapi.models.V1ConfigMap;
import io.kubernetes.client.openapi.models.V1ObjectMeta;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ConfigMapInformerTest {

    private static V1ConfigMap configMap(String name) {
        return new V1ConfigMap().metadata(new V1ObjectMeta().name(name).namespace("default"));
    }

    @Test
    void testTheHandlerRecordsTheEvents() {
        ConfigMapInformer informer = new ConfigMapInformer();

        informer.onAdd(configMap("added"));
        informer.onUpdate(configMap("added"), configMap("updated"));
        informer.onDelete(configMap("deleted"), false);

        assertEquals(List.of("added"), informer.getAdded().stream().map(cm -> cm.getMetadata().getName()).toList());
        assertEquals(List.of("updated"), informer.getUpdated().stream().map(cm -> cm.getMetadata().getName()).toList());
        assertEquals(List.of("deleted"), informer.getDeleted().stream().map(cm -> cm.getMetadata().getName()).toList());
    }
}
