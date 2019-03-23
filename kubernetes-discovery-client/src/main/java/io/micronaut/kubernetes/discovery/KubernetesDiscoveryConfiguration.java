package io.micronaut.kubernetes.discovery;

import io.micronaut.context.annotation.BootstrapContextCompatible;
import io.micronaut.context.annotation.ConfigurationProperties;
import io.micronaut.discovery.DiscoveryConfiguration;
import io.micronaut.kubernetes.client.v1.KubernetesConfiguration;

/**
 * Configuration class for the discovery client of Kubernetes.
 *
 * @author Álvaro Sánchez-Mariscal
 * @since 1.0.0
 */
@ConfigurationProperties(KubernetesDiscoveryConfiguration.PREFIX)
@BootstrapContextCompatible
public class KubernetesDiscoveryConfiguration extends DiscoveryConfiguration {

    public static final String PREFIX = KubernetesConfiguration.PREFIX + "." + DiscoveryConfiguration.PREFIX;

    private KubernetesConfiguration kubernetesConfiguration;

    /**
     * @param kubernetesConfiguration Kubernetes configuration
     */
    public KubernetesDiscoveryConfiguration(KubernetesConfiguration kubernetesConfiguration) {
        this.kubernetesConfiguration = kubernetesConfiguration;
    }

    /**
     * @return The Kubernetes configuration
     */
    public KubernetesConfiguration getKubernetesConfiguration() {
        return kubernetesConfiguration;
    }

}
