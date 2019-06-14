package io.micronaut.kubernetes.discovery;

import io.micronaut.context.annotation.BootstrapContextCompatible;
import io.micronaut.context.annotation.Requires;
import io.micronaut.context.env.Environment;
import io.micronaut.discovery.ServiceInstance;
import io.micronaut.discovery.ServiceInstanceList;
import io.micronaut.kubernetes.client.v1.KubernetesClient;

import javax.inject.Singleton;
import java.net.URI;
import java.util.Collections;
import java.util.List;

import static io.micronaut.kubernetes.client.v1.KubernetesClient.SERVICE_ID;
import static io.micronaut.kubernetes.discovery.KubernetesDiscoveryClient.KUBERNETES_URI;

/**
 * A {@link io.micronaut.discovery.ServiceInstanceList} implementation for Kubernetes
 *
 * @author Álvaro Sánchez-Mariscal
 * @since 1.0.0
 */
@Singleton
@Requires(env = Environment.KUBERNETES)
@BootstrapContextCompatible
public class KubernetesServiceInstanceList implements ServiceInstanceList {

    @Override
    public String getID() {
        return KubernetesClient.SERVICE_ID;
    }

    @Override
    public List<ServiceInstance> getInstances() {
        return Collections.singletonList(ServiceInstance.of(SERVICE_ID, URI.create(KUBERNETES_URI)));
    }


}
