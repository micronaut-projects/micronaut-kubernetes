package io.micronaut.kubernetes.discovery;

import io.micronaut.context.annotation.BootstrapContextCompatible;
import io.micronaut.context.annotation.Requires;
import io.micronaut.context.env.Environment;
import io.micronaut.discovery.client.DiscoveryClientConfiguration;
import io.micronaut.discovery.client.DiscoveryServerInstanceList;
import io.micronaut.kubernetes.client.v1.KubernetesClient;
import io.micronaut.runtime.ApplicationConfiguration;

import javax.inject.Singleton;

/**
 * A {@link io.micronaut.discovery.ServiceInstanceList} implementation for Kubernetes.
 *
 * @author Álvaro Sánchez-Mariscal
 * @since 1.0.0
 */
@Singleton
@Requires(env = Environment.KUBERNETES)
@BootstrapContextCompatible
public class KubernetesServiceInstanceList extends DiscoveryServerInstanceList {

    /**
     * @param configuration         The discovery client configuration
     * @param instanceConfiguration The instance configuration
     */
    public KubernetesServiceInstanceList(DiscoveryClientConfiguration configuration, ApplicationConfiguration.InstanceConfiguration instanceConfiguration) {
        super(configuration, instanceConfiguration);
    }

    @Override
    public String getID() {
        return KubernetesClient.SERVICE_ID;
    }

}
