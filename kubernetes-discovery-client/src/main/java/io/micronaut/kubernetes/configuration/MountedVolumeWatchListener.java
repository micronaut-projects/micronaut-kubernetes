package io.micronaut.kubernetes.configuration;

import io.micronaut.context.ApplicationContext;
import io.micronaut.context.annotation.Requires;
import io.micronaut.context.env.Environment;
import io.micronaut.context.env.PropertySource;
import io.micronaut.context.event.ApplicationEventListener;
import io.micronaut.kubernetes.client.v1.KubernetesConfiguration;
import io.micronaut.runtime.context.scope.refresh.RefreshEvent;
import io.micronaut.scheduling.io.watch.event.WatchEventType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Singleton;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.WatchEvent;
import java.util.HashMap;
import java.util.Map;

import static io.micronaut.kubernetes.util.KubernetesUtils.configMapAsPropertySource;

/**
 *
 */
@Requires(beans = ApplicationContext.class)
@Requires(beans = Environment.class)
@Requires(property = KubernetesConfiguration.PREFIX + ".enabled", notEquals = "false")
@Singleton
public class MountedVolumeWatchListener implements ApplicationEventListener<MountedVolumeChangedEvent> {

    private static final Logger LOG = LoggerFactory.getLogger(MountedVolumeWatchListener.class);
    private final ApplicationContext context;
    private final KubernetesConfiguration kubernetesConfiguration;
    private Environment environment;

    /**
     * Default constructor.
     *
     * @param environment             The environment
     * @param context                 The ApplicationContext
     * @param kubernetesConfiguration The kubernetesConfiguration
     */
    public MountedVolumeWatchListener(Environment environment, ApplicationContext context, KubernetesConfiguration kubernetesConfiguration) {
        this.environment = environment;
        this.context = context;
        this.kubernetesConfiguration = kubernetesConfiguration;
    }

    @Override
    public void onApplicationEvent(MountedVolumeChangedEvent event) {
        processConfigMapEvent(event);
    }

    @Override
    public boolean supports(MountedVolumeChangedEvent event) {
        return true;
    }

    private void processConfigMapEvent(MountedVolumeChangedEvent event) {
        if (event.isConfigMap()) {
            Path filePath = event.getPath();
            try {
                if (!Files.isDirectory(filePath)) {
                    String key = filePath.getFileName().toString();
                    String value = new String(Files.readAllBytes(filePath));
                    final Map<String, String> objectObjectHashMap = new HashMap();
                    objectObjectHashMap.put(key, value);
                    WatchEventType kind = event.getEventType();
                    switch (event.getEventType()) {
                        case CREATE:
                            processConfigMapAdded(key, objectObjectHashMap);
                        case MODIFY:
                            processConfigMapModified(key, objectObjectHashMap);
                        case DELETE:
                            processConfigMapDeleted(key, objectObjectHashMap);
                        default:
                            processConfigMapErrored(kind);
                    }

                }
            } catch (IOException e) {
                LOG.warn("Exception occurred when reading file from path: {}", filePath);
                LOG.warn(e.getMessage(), e);
            }
        }
    }

    private void processConfigMapAdded(String key, Map<String, String> data) {
        PropertySource propertySource = null;
        if (data != null) {
            propertySource = configMapAsPropertySource(key, data);
        }
        this.environment.addPropertySource(propertySource);
        KubernetesConfigurationClient.addPropertySourceToCache(propertySource);
        this.environment = environment.refresh();
        context.publishEvent(new RefreshEvent());
    }

    private void processConfigMapModified(String key, Map<String, String> data) {
        PropertySource propertySource = null;
        if (data != null) {
            propertySource = configMapAsPropertySource(key, data);
        }

        this.environment.removePropertySource(propertySource);
        this.environment.addPropertySource(propertySource);
        KubernetesConfigurationClient.removePropertySourceFromCache(propertySource.getName());
        KubernetesConfigurationClient.addPropertySourceToCache(propertySource);
        this.environment = environment.refresh();
        context.publishEvent(new RefreshEvent());
    }

    private void processConfigMapDeleted(String key, Map<String, String> data) {
        PropertySource propertySource = null;
        if (data != null) {
            propertySource = configMapAsPropertySource(key, data);
        }
        this.environment.removePropertySource(propertySource);
        KubernetesConfigurationClient.removePropertySourceFromCache(propertySource.getName());
        this.environment = environment.refresh();
        context.publishEvent(new RefreshEvent());
    }

    private void processConfigMapErrored(WatchEventType event) {
        LOG.error("Kubernetes API returned an error for a ConfigMap watch event: {}", event.toString());
    }

    private void processSecretModified(PropertySource propertySource) {
        this.environment.removePropertySource(propertySource);
        this.environment.addPropertySource(propertySource);
        KubernetesConfigurationClient.removePropertySourceFromCache(propertySource.getName());
        KubernetesConfigurationClient.addPropertySourceToCache(propertySource);
        this.environment = environment.refresh();
        context.publishEvent(new RefreshEvent());
    }

    private void processSecretErrored(WatchEvent<?> event) {
        LOG.error("Kubernetes API returned an error for a ConfigMap watch event: {}", event.toString());
    }
}
