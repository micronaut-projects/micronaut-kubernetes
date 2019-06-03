package io.micronaut.kubernetes.configuration;

import io.micronaut.context.annotation.BootstrapContextCompatible;
import io.micronaut.context.annotation.Requires;
import io.micronaut.context.env.*;
import io.micronaut.context.env.yaml.YamlPropertySourceLoader;
import io.micronaut.core.util.StringUtils;
import io.micronaut.discovery.config.ConfigurationClient;
import io.micronaut.jackson.env.JsonPropertySourceLoader;
import io.micronaut.kubernetes.client.v1.KubernetesClient;
import io.micronaut.kubernetes.client.v1.KubernetesConfiguration;
import io.micronaut.kubernetes.client.v1.configmaps.ConfigMap;
import io.micronaut.kubernetes.client.v1.configmaps.ConfigMapList;
import io.reactivex.Flowable;
import org.reactivestreams.Publisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Singleton;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
    private final List<PropertySourceReader> propertySourceReaders;

    /**
     * @param client An HTTP Client to query the Kubernetes API.
     * @param configuration The configuration properties
     */
    public KubernetesConfigurationClient(KubernetesClient client, KubernetesConfiguration configuration) {
        LOG.debug("Initializing {}", getClass().getName());
        this.client = client;
        this.configuration = configuration;
        this.propertySourceReaders = Arrays.asList(new YamlPropertySourceLoader(), new JsonPropertySourceLoader(), new PropertiesPropertySourceLoader());
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
                .flatMap(this::entryToPropertySource)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    private Stream<? extends PropertySource> entryToPropertySource(Map.Entry<String, String> entry) {
        String extension = getExtension(entry.getKey()).orElse("yml");
        return this.propertySourceReaders.stream()
                .filter(reader -> reader.getExtensions().contains(extension))
                .map(reader -> reader.read(entry.getKey(), entry.getValue().getBytes()))
                .map(map -> PropertySource.of(entry.getKey(), map));
    }

    private Optional<String> getExtension(String filename) {
        return Optional.of(filename)
                .filter(f -> f.contains("."))
                .map(f -> f.substring(filename.lastIndexOf(".") + 1));
    }

}
