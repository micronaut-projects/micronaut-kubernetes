package io.micronaut.kubernetes.configuration;

import io.micronaut.context.ApplicationContext;
import io.micronaut.context.annotation.Requires;
import io.micronaut.context.env.Environment;
import io.micronaut.context.env.EnvironmentPropertySource;
import io.micronaut.context.env.PropertySource;
import io.micronaut.context.event.ApplicationEventListener;
import io.micronaut.kubernetes.client.v1.KubernetesConfiguration;
import io.micronaut.runtime.context.scope.Refreshable;
import io.micronaut.runtime.context.scope.refresh.RefreshEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Singleton;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static io.micronaut.kubernetes.util.KubernetesMountedVolumesUtils.configMapAsPropertySource;


/**
 * Implementation of {@link MountedVolumeChangedEvent}.
 *
 * @author Stephane Paulus
 * @since 1.0.0
 */
@Requires(beans = KubernetesConfiguration.class)
@Requires(beans = Environment.class)
@Singleton
public class MountedVolumeWatchListener implements ApplicationEventListener<MountedVolumeChangedEvent> {

    private static final Logger LOG = LoggerFactory.getLogger(MountedVolumeWatchListener.class);
    private final ApplicationContext context;
    private final KubernetesConfiguration configuration;
    private Environment environment;

    /**
     * Default constructor.
     *
     * @param environment The environment
     * @param context The applicationContext
     * @param configuration The kubernetesConfiguration
     */
    public MountedVolumeWatchListener(Environment environment, ApplicationContext context, KubernetesConfiguration configuration) {
        this.environment = environment;
        this.context = context;
        this.configuration = configuration;
    }

    @Override
    public void onApplicationEvent(MountedVolumeChangedEvent event) {
        if (event.isConfigMap()) {
            processConfigMapDeleted();
            processConfigMapEvent();
        } else {
            processSecretDeleted();
            processSecretEvent();
        }
    }

    @Override
    public boolean supports(MountedVolumeChangedEvent event) {
        return true;
    }

    private void processConfigMapEvent() {
        if (configuration.getConfigMaps().isEnabled()) {
            Collection<String> mountedVolumePaths = configuration.getConfigMaps().getPaths();
            if (!mountedVolumePaths.isEmpty()) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Reading configmap from the following mounted volumes: {}", mountedVolumePaths);
                }
                mountedVolumePaths.stream()
                        .map(Paths::get)
                        .forEach(path -> {
                            LOG.trace("Processing path: {}", path);
                            try (DirectoryStream<Path> stream = Files.newDirectoryStream(path)) {
                                for (Path file : stream) {
                                    if (!Files.isDirectory(file)) {
                                        String key = file.getFileName().toString();
                                        String value = new String(Files.readAllBytes(file));
                                        if (LOG.isTraceEnabled()) {
                                            LOG.trace("Processing key: {}", key);
                                        }
                                        final HashMap<String, String> objectObjectHashMap = new HashMap();
                                        objectObjectHashMap.put(key, value);
                                        final PropertySource propertySource = configMapAsPropertySource(key, objectObjectHashMap);
                                        this.environment.addPropertySource(propertySource);
                                        KubernetesMountedVolumeConfigurationClient.addPropertySourceToCache(propertySource);
                                        this.environment = environment.refresh();
                                        context.publishEvent(new RefreshEvent());
                                    }
                                }
                            } catch (IOException e) {
                                LOG.warn("Exception occurred when reading configmap from path: {}", path);
                                LOG.warn(e.getMessage(), e);
                            }
                        });
            }
        }
    }

    private void processSecretEvent() {
        if (configuration.getSecrets().isEnabled()) {
            Collection<String> mountedVolumePaths = configuration.getSecrets().getPaths();
            if (!mountedVolumePaths.isEmpty()) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Reading Secrets from the following mounted volumes: {}", mountedVolumePaths);
                }

                mountedVolumePaths.stream()
                        .map(Paths::get)
                        .forEach(path -> {
                            LOG.trace("Processing path: {}", path);
                            try (DirectoryStream<Path> stream = Files.newDirectoryStream(path)) {
                                Map<String, Object> propertySourceContents = new HashMap<>();
                                for (Path file : stream) {
                                    if (!Files.isDirectory(file)) {
                                        String key = file.getFileName().toString();
                                        String value = new String(Files.readAllBytes(file));
                                        if (LOG.isTraceEnabled()) {
                                            LOG.trace("Processing key: {}", key);
                                        }
                                        propertySourceContents.put(key, value);
                                    }
                                }
                                String propertySourceName = path.toString() + KubernetesMountedVolumeConfigurationClient.KUBERNETES_SECRET_NAME_SUFFIX;
                                int priority = EnvironmentPropertySource.POSITION + 150;
                                PropertySource propertySource = PropertySource.of(propertySourceName, propertySourceContents, priority);
                                this.environment.addPropertySource(propertySource);
                                KubernetesConfigurationClient.addPropertySourceToCache(propertySource);
                                this.environment = environment.refresh();
                                context.publishEvent(new RefreshEvent());
                            } catch (IOException e) {
                                LOG.warn("Exception occurred when reading secrets from path: {}", path);
                                LOG.warn(e.getMessage(), e);
                            }
                        });
            }
        }
    }

    private void processConfigMapDeleted() {
        final List<PropertySource> configMapPropertySources = this.environment.getPropertySources().stream().filter(propertySource -> propertySource.getName()
                .contains(KubernetesConfigurationClient.KUBERNETES_CONFIG_MAP_NAME_SUFFIX)).collect(Collectors.toList());
        configMapPropertySources.forEach(propertySource -> this.environment.removePropertySource(propertySource));
        configMapPropertySources.forEach(propertySource -> KubernetesMountedVolumeConfigurationClient.removePropertySourceFromCache(propertySource.getName()));
        this.environment = environment.refresh();
    }

    private void processSecretDeleted() {
        final List<PropertySource> configMapPropertySources = this.environment.getPropertySources().stream().filter(propertySource -> propertySource.getName()
                .contains(KubernetesMountedVolumeConfigurationClient.KUBERNETES_SECRET_NAME_SUFFIX)).collect(Collectors.toList());
        configMapPropertySources.forEach(propertySource -> this.environment.removePropertySource(propertySource));
        configMapPropertySources.forEach(propertySource -> KubernetesMountedVolumeConfigurationClient.removePropertySourceFromCache(propertySource.getName()));
        this.environment = environment.refresh();
    }
}
