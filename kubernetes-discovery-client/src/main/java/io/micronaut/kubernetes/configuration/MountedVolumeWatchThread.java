/*
 * Copyright 2017-2020 original authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.micronaut.kubernetes.configuration;

import edu.umd.cs.findbugs.annotations.NonNull;
import io.micronaut.context.LifeCycle;
import io.micronaut.context.annotation.Parallel;
import io.micronaut.context.annotation.Requires;
import io.micronaut.context.event.ApplicationEventPublisher;
import io.micronaut.core.util.StringUtils;
import io.micronaut.kubernetes.client.v1.KubernetesConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Singleton;
import java.io.IOException;
import java.nio.file.ClosedWatchServiceException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.time.Duration;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import static java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY;

/**
 * A watch service that send an event every time a modify event took place in the mounted volumes directory.
 *
 *
 * @author Stephane Paulus
 * @since 1.0.0
 */
@Requires(property = KubernetesConfiguration.PREFIX + ".watch", notEquals = StringUtils.FALSE, defaultValue = StringUtils.TRUE)
@Parallel
@Singleton
public class MountedVolumeWatchThread implements LifeCycle<MountedVolumeWatchThread> {

    private static final Logger LOG = LoggerFactory.getLogger(MountedVolumeWatchThread.class);
    private final KubernetesConfiguration configuration;
    private final AtomicBoolean active = new AtomicBoolean(true);
    private final ApplicationEventPublisher eventPublisher;
    private final WatchService watchService;
    private final Collection<WatchKey> secretWatchKeys = new ConcurrentLinkedQueue<>();
    private final Collection<WatchKey> configMapWatchKeys = new ConcurrentLinkedQueue<>();

    /**
     * Default constructor.
     *
     * @param eventPublisher The event publisher
     * @param configuration  the configuration
     */
    protected MountedVolumeWatchThread(
            ApplicationEventPublisher eventPublisher,
            KubernetesConfiguration configuration) throws IOException {
        this.eventPublisher = eventPublisher;
        this.configuration = configuration;
        this.watchService = FileSystems.getDefault().newWatchService();
    }

    @Override
    public boolean isRunning() {
        return active.get();
    }

    @Override
    @PostConstruct
    public MountedVolumeWatchThread start() {
        try {
            final List<Path> configMapPaths = configuration.getConfigMaps().getPaths().stream().map(Paths::get).collect(Collectors.toList());
            if (!configMapPaths.isEmpty()) {
                for (Path path : configMapPaths) {
                    if (path.toFile().exists()) {
                        addMountedVolumeWatchDirectory(path, true);
                    }
                }
            }
            final List<Path> secretPaths = configuration.getSecrets().getPaths().stream().map(Paths::get).collect(Collectors.toList());
            if (!secretPaths.isEmpty()) {
                for (Path path : secretPaths) {
                    if (path.toFile().exists()) {
                        addMountedVolumeWatchDirectory(path, false);
                    }
                }
            }

            if (!(secretWatchKeys.isEmpty() && configMapWatchKeys.isEmpty())) {
                new Thread(() -> {
                    while (active.get()) {
                        try {
                            WatchKey watchKey = watchService.poll(Duration.ofMillis(300).toMillis(), TimeUnit.MILLISECONDS);
                            if (watchKey != null && (secretWatchKeys.contains(watchKey) || configMapWatchKeys.contains(watchKey))) {
                                List<WatchEvent<?>> watchEvents = watchKey.pollEvents();
                                for (WatchEvent<?> watchEvent : watchEvents) {
                                    WatchEvent.Kind<?> kind = watchEvent.kind();
                                    if (kind == StandardWatchEventKinds.OVERFLOW) {
                                        if (LOG.isWarnEnabled()) {
                                            LOG.warn("WatchService Overflow occurred");
                                        }
                                    } else {
                                        final Object context = watchEvent.context();
                                        if (context instanceof Path) {
                                            Path dir = (Path) watchKey.watchable();
                                            Path fullPath = dir.resolve(context.toString());
                                            if (LOG.isDebugEnabled()) {
                                                LOG.debug("File at path {} changed. Firing change event: {}", context, kind);
                                            }
                                            eventPublisher.publishEvent(new MountedVolumeChangedEvent(
                                                    fullPath,
                                                    configMapWatchKeys.contains(watchKey),
                                                    kind
                                            ));
                                        }
                                    }
                                }
                                watchKey.reset();
                            }
                        } catch (InterruptedException | ClosedWatchServiceException e) {
                            // ignore
                        }
                    }
                }, "micronaut-mounted-volume-watch-thread").start();
            }
        } catch (IOException e) {
            if (LOG.isErrorEnabled()) {
                LOG.error("Error starting file watch service: " + e.getMessage(), e);
            }
        }
        return this;
    }

    @Override
    public MountedVolumeWatchThread stop() {
        active.set(false);
        closeWatchService();
        return this;
    }

    @Override
    @PreDestroy
    public void close() {
        stop();
    }

    /**
     * @return The watch service used.
     */
    public @NonNull
    WatchService getWatchService() {
        return watchService;
    }

    /**
     * Closes the watch service.
     */
    protected void closeWatchService() {
        try {
            getWatchService().close();
        } catch (IOException e) {
            if (LOG.isErrorEnabled()) {
                LOG.error("Error stopping file watch service: " + e.getMessage(), e);
            }
        }
    }

    private void addMountedVolumeWatchDirectory(Path p, boolean isConfigmap) throws IOException {
        WatchKey key = p.register(watchService, ENTRY_MODIFY);
        if (isConfigmap) {
            configMapWatchKeys.add(key);
        } else {
            secretWatchKeys.add(key);
        }
    }
}
