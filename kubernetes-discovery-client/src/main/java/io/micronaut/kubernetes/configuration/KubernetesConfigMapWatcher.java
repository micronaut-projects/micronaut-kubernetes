/*
 * Copyright 2017-2019 original authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.micronaut.kubernetes.configuration;

import io.micronaut.context.annotation.Requires;
import io.micronaut.context.env.Environment;
import io.micronaut.context.env.PropertySource;
import io.micronaut.context.event.ApplicationEventListener;
import io.micronaut.core.util.StringUtils;
import io.micronaut.discovery.config.ConfigurationClient;
import io.micronaut.discovery.event.ServiceStartedEvent;
import io.micronaut.kubernetes.client.v1.KubernetesClient;
import io.micronaut.kubernetes.client.v1.KubernetesConfiguration;
import io.micronaut.kubernetes.client.v1.configmaps.ConfigMapWatchEvent;
import io.reactivex.Flowable;
import io.reactivex.schedulers.Schedulers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Named;
import javax.inject.Singleton;
import java.util.concurrent.ExecutorService;

/**
 * Watches for ConfigMap changes and makes the appropriate changes to the {@link Environment} by adding or removing
 * {@link PropertySource}s.
 *
 * @author Álvaro Sánchez-Mariscal
 * @since 1.0.0
 */
@Singleton
@Requires(env = Environment.KUBERNETES)
@Requires(beans = KubernetesClient.class)
@Requires(property = ConfigurationClient.ENABLED, value = StringUtils.TRUE, defaultValue = StringUtils.FALSE)
public class KubernetesConfigMapWatcher implements ApplicationEventListener<ServiceStartedEvent> {

    private static final Logger LOG = LoggerFactory.getLogger(KubernetesConfigMapWatcher.class);

    private Environment environment;
    private final KubernetesClient client;
    private final KubernetesConfiguration configuration;
    private final ExecutorService executorService;

    /**
     * @param environment the {@link Environment}
     * @param client the {{@link KubernetesClient}}
     * @param configuration the {@link KubernetesConfiguration}
     * @param executorService the IO {@link ExecutorService} where the watch publisher will be scheduled on
     */
    public KubernetesConfigMapWatcher(Environment environment, KubernetesClient client, KubernetesConfiguration configuration, @Named("io") ExecutorService executorService) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("Initializing {}", getClass().getName());
        }

        this.environment = environment;
        this.client = client;
        this.configuration = configuration;
        this.executorService = executorService;
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    @Override
    public void onApplicationEvent(ServiceStartedEvent event) {
        int lastResourceVersion = computeLastResourceVersion();

        LOG.debug("Watching for ConfigMap events...");
        Flowable.fromPublisher(client.watchConfigMaps(configuration.getNamespace(), lastResourceVersion))
//        Flowable.fromPublisher(client.watchConfigMaps(configuration.getNamespace(), 0)) //To reproduce https://github.com/micronaut-projects/micronaut-core/issues/1864
                .doOnNext(e -> LOG.trace("Received ConfigMap watch event: {}", e))
                .doOnError(throwable -> LOG.error("Error while watching ConfigMap events", throwable))
                .subscribeOn(Schedulers.from(this.executorService))
                .subscribe(this::processEvent);
    }

    private int computeLastResourceVersion() {
        int lastResourceVersion = this.environment
                .getPropertySources()
                .stream()
                .filter(propertySource -> propertySource.getName().endsWith(KubernetesConfigurationClient.KUBERNETES_CONFIG_MAP_NAME_SUFFIX))
                .map(propertySource -> propertySource.get(KubernetesConfigurationClient.CONFIG_MAP_RESOURCE_VERSION))
                .map(o -> Integer.parseInt(o.toString()))
                .max(Integer::compareTo)
                .orElse(0);

        if (LOG.isDebugEnabled()) {
            LOG.debug("Latest resourceVersion is: {}", lastResourceVersion);
        }

        return lastResourceVersion;
    }

    private void processEvent(ConfigMapWatchEvent event) {
        PropertySource propertySource = null;
        if (event.getObject() != null) {
            propertySource = KubernetesConfigurationClient.configMapAsPropertySource(event.getObject());
        }
        switch (event.getType()) {
            case ADDED:
                processConfigMapAdded(propertySource);
                break;

            case MODIFIED:
                processConfigMapModified(propertySource);
                break;

            case DELETED:
                processConfigMapDeleted(propertySource);
                break;

            case ERROR:
            default:
                processConfigMapErrored(event);
        }

    }

    private void processConfigMapAdded(PropertySource propertySource) {
        this.environment.addPropertySource(propertySource);
    }

    private void processConfigMapModified(PropertySource propertySource) {
//        this.environment.removePropertySource(propertySource);
//        this.environment.addPropertySource(propertySource);
        this.environment = environment.refresh();
    }

    private void processConfigMapDeleted(PropertySource propertySource) {
//        this.environment.removePropertySource(propertySource);
        this.environment = environment.refresh();
    }

    private void processConfigMapErrored(ConfigMapWatchEvent event) {
        LOG.error("Kubernetes API returned an error for a ConfigMap watch event: {}", event.toString());
    }

}
