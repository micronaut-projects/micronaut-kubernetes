package io.micronaut.docs.informer;

import io.kubernetes.client.informer.ResourceEventHandler;
import io.kubernetes.client.openapi.models.V1ConfigMap;
import io.kubernetes.client.openapi.models.V1ConfigMapList;
import io.micronaut.context.annotation.Requires;
import io.micronaut.kubernetes.client.informer.Informer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

@Requires(property = "spec.name", value = "ConfigMapInformerSpec")
//tag::handler[]
@Informer(apiType = V1ConfigMap.class, apiListType = V1ConfigMapList.class) // <1>
public class ConfigMapInformer implements ResourceEventHandler<V1ConfigMap> { // <2>

    //end::handler[]
    public static final Logger LOG = LoggerFactory.getLogger(ConfigMapInformer.class);

    private final List<V1ConfigMap> added = new ArrayList<>();
    private final List<V1ConfigMap> updated = new ArrayList<>();
    private final List<V1ConfigMap> deleted = new ArrayList<>();

    public List<V1ConfigMap> getAdded() {
        return added;
    }

    public List<V1ConfigMap> getUpdated() {
        return updated;
    }

    public List<V1ConfigMap> getDeleted() {
        return deleted;
    }

    //tag::handler[]
    @Override
    public void onAdd(V1ConfigMap obj) {
        //end::handler[]
        LOG.info("ADDED CONFIG MAP: {}", obj);
        added.add(obj);
        //tag::handler[]
    }

    @Override
    public void onUpdate(V1ConfigMap oldObj, V1ConfigMap newObj) {
        //end::handler[]
        LOG.info("UPDATED CONFIG MAP: {}", newObj);
        updated.add(newObj);
        //tag::handler[]
    }

    @Override
    public void onDelete(V1ConfigMap obj, boolean deletedFinalStateUnknown) {
        //end::handler[]
        LOG.info("DELETED CONFIG MAP: {}", obj);
        deleted.add(obj);
        //tag::handler[]
    }
}
//end::handler[]
