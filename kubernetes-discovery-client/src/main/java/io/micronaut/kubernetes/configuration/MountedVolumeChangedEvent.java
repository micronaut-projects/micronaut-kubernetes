/*
 * Copyright 2017-2021 original authors
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
import io.micronaut.context.event.ApplicationEvent;
import io.micronaut.core.util.ArgumentUtils;
import io.micronaut.scheduling.io.watch.event.WatchEventType;

import javax.annotation.concurrent.Immutable;
import java.nio.file.Path;
import java.nio.file.WatchEvent;

/**
 * <p>An {@link MountedVolumeChangedEvent} for handling mounted volume changes.</p>
 *
 * @author Stephane Paulus
 * @since 1.0.0
 */
@Immutable
public class MountedVolumeChangedEvent extends ApplicationEvent {
    private final Path path;
    private final boolean isConfigMap;
    private final WatchEventType eventType;

    /**
     * Constructs a new file changed event.
     *
     * @param path        The path
     * @param isConfigMap The isConfigMap
     * @param eventType   The event type
     */
    public MountedVolumeChangedEvent(@NonNull Path path, @NonNull boolean isConfigMap, @NonNull WatchEventType eventType) {
        super(path);
        ArgumentUtils.requireNonNull("path", path);
        ArgumentUtils.requireNonNull("isConfigMap", isConfigMap);
        ArgumentUtils.requireNonNull("eventType", eventType);
        this.path = path;
        this.isConfigMap = isConfigMap;
        this.eventType = eventType;
    }

    /**
     * Constructs a new file changed event.
     *
     * @param path        The path
     * @param isConfigMap The isConfigMap
     * @param eventType   The event type
     */
    public MountedVolumeChangedEvent(@NonNull Path path, @NonNull boolean isConfigMap, @NonNull WatchEvent.Kind eventType) {
        this(path, isConfigMap, WatchEventType.of(eventType));
    }

    @Override
    public @NonNull
    Path getSource() {
        return (Path) super.getSource();
    }

    /**
     * The path to the file / directory that changed.
     *
     * @return The path
     */
    public @NonNull
    Path getPath() {
        return path;
    }

    /**
     * If the file that is changed is a configmap.
     *
     * @return The path
     */
    public boolean isConfigMap() {
        return isConfigMap;
    }

    /**
     * The watch event type.
     *
     * @return The event type
     */
    public @NonNull
    WatchEventType getEventType() {
        return eventType;
    }
}

