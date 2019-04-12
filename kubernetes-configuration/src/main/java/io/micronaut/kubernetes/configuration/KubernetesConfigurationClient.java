package io.micronaut.kubernetes.configuration;

import io.micronaut.context.annotation.BootstrapContextCompatible;
import io.micronaut.context.annotation.Requires;
import io.micronaut.context.env.Environment;
import io.micronaut.context.env.MapPropertySource;
import io.micronaut.context.env.PropertySource;
import io.micronaut.core.util.StringUtils;
import io.micronaut.discovery.config.ConfigurationClient;
import io.micronaut.kubernetes.client.v1.KubernetesClient;
import io.micronaut.kubernetes.client.v1.KubernetesConfiguration;
import io.micronaut.kubernetes.client.v1.configmaps.ConfigMap;
import io.micronaut.kubernetes.client.v1.configmaps.ConfigMapList;
import io.reactivex.Flowable;
import org.reactivestreams.Publisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Singleton;
import java.io.IOException;
import java.io.StringReader;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * A {@link ConfigurationClient} implementation that provides {@link PropertySource}s read from Kubernetes ConfigMap's.
 *
 * @author Álvaro Sánchez-Mariscal
 * @since 1.0.0
 */
@Singleton
@Requires(env = Environment.KUBERNETES)
@Requires(beans = KubernetesClient.class)
@Requires(property = ConfigurationClient.ENABLED, value = StringUtils.TRUE, defaultValue = StringUtils.FALSE)
@BootstrapContextCompatible
public class KubernetesConfigurationClient implements ConfigurationClient {

    private static final Logger LOG = LoggerFactory.getLogger(KubernetesConfigurationClient.class);

    private final KubernetesClient client;
    private final KubernetesConfiguration configuration;

    /**
     * @param client An HTTP Client to query the Kubernetes API.
     * @param configuration The configuration properties
     */
    public KubernetesConfigurationClient(KubernetesClient client, KubernetesConfiguration configuration) {
        this.client = client;
        this.configuration = configuration;
    }

    /**
     * Retrieves all of the {@link PropertySource} registrations for the given environment.
     *
     * @param environment The environment
     * @return A {@link Publisher} that emits zero or many {@link PropertySource} instances discovered for the given environment
     */
    @Override
    public Publisher<PropertySource> getPropertySources(Environment environment) {
        return Flowable.fromPublisher(client.listConfigMaps(configuration.getNamespace()))
                .doOnError(throwable -> LOG.error("Error while trying to list all Kubernetes ConfigMaps in the namespace [" + configuration.getNamespace() + "]", throwable))
                .doOnNext(configMapList -> LOG.debug("Found {} config maps", configMapList.getItems().size()))
                .flatMapIterable(ConfigMapList::getItems)
                .doOnNext(configMap -> LOG.debug("Adding config map with name {}", configMap.getMetadata().getName()))
                .flatMapIterable(this::asPropertySources);
    }

    /**
     * A description that describes this object.
     *
     * @return The description
     */
    @Override
    public String getDescription() {
        return KubernetesClient.SERVICE_ID;
    }

    private List<PropertySource> asPropertySources(ConfigMap configMap) {
        return configMap.getData().entrySet()
                .stream()
                .map(entryToPropertySource())
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    private Function<Map.Entry<String, String>, MapPropertySource> entryToPropertySource() {
        return entry -> {
            try {
                Properties properties = new Properties();
                properties.load(new StringReader(entry.getValue()));
                return new MapPropertySource(entry.getKey(), properties);
            } catch (IOException e) {
                return null;
            }
        };
    }
}
