/*
 * Copyright 2017-2026 original authors
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
package io.micronaut.kubernetes.configuration.imports;

import io.micronaut.core.util.CollectionUtils;
import org.jspecify.annotations.NonNull;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Tracks watched import declarations by selector so watcher events can invalidate matching imports.
 */
final class ImportDeclarationWatchIndex {

    private static final Map<SelectorKey, ImportDeclaration> CONFIG_MAP_INDEX = new ConcurrentHashMap<>();
    private static final Map<SelectorKey, ImportDeclaration> SECRET_INDEX = new ConcurrentHashMap<>();

    private static final Map<ImportDeclaration, AtomicInteger> REFRESH_COUNT = new ConcurrentHashMap<>();

    private static final AtomicBoolean CONFIG_MAP_WATCHER_ENABLED = new AtomicBoolean(false);
    private static final AtomicBoolean SECRET_WATCHER_ENABLED = new AtomicBoolean(false);

    /**
     * Registers a watched ConfigMap import that targets a specific resource name.
     *
     * @param name              The ConfigMap name
     * @param importDeclaration The watched import declaration
     */
    static void addConfigMapNameDeclaration(@NonNull String name,
                                            @NonNull ImportDeclaration importDeclaration) {
        CONFIG_MAP_INDEX.putIfAbsent(new SelectorKey.NameKey(name), importDeclaration);
        REFRESH_COUNT.putIfAbsent(importDeclaration, new AtomicInteger());
        CONFIG_MAP_WATCHER_ENABLED.compareAndSet(false, true);
    }

    /**
     * Registers a watched ConfigMap import that targets resources by labels.
     *
     * @param labels            The label selector
     * @param importDeclaration The watched import declaration
     */
    static void addConfigMapLabelsDeclaration(@NonNull Map<String, String> labels,
                                              @NonNull ImportDeclaration importDeclaration) {
        CONFIG_MAP_INDEX.put(new SelectorKey.LabelsKey(labels), importDeclaration);
        REFRESH_COUNT.putIfAbsent(importDeclaration, new AtomicInteger());
        CONFIG_MAP_WATCHER_ENABLED.compareAndSet(false, true);
    }

    /**
     * Registers a watched Secret import that targets a specific resource name.
     *
     * @param name              The Secret name
     * @param importDeclaration The watched import declaration
     */
    static void addSecretNameDeclaration(@NonNull String name,
                                         @NonNull ImportDeclaration importDeclaration) {
        SECRET_INDEX.put(new SelectorKey.NameKey(name), importDeclaration);
        REFRESH_COUNT.putIfAbsent(importDeclaration, new AtomicInteger());
        SECRET_WATCHER_ENABLED.compareAndSet(false, true);
    }

    /**
     * Registers a watched Secret import that targets resources by labels.
     *
     * @param labels            The label selector
     * @param importDeclaration The watched import declaration
     */
    static void addSecretLabelsDeclaration(@NonNull Map<String, String> labels,
                                           @NonNull ImportDeclaration importDeclaration) {
        SECRET_INDEX.put(new SelectorKey.LabelsKey(labels), importDeclaration);
        REFRESH_COUNT.putIfAbsent(importDeclaration, new AtomicInteger());
        SECRET_WATCHER_ENABLED.compareAndSet(false, true);
    }

    static AtomicInteger getRefreshCount(ImportDeclaration importDeclaration) {
        return REFRESH_COUNT.get(importDeclaration);
    }

    /**
     * Increments the refresh count when the given ConfigMap matches a watched import declaration.
     *
     * @param objectName The ConfigMap name
     * @param objectLabels The ConfigMap labels
     * @return {@code true} if the ConfigMap is watched and the refresh count was updated
     */
    static boolean updateRefreshCountIfConfigMapWatched(@NonNull String objectName,
                                                        @NonNull Map<String, String> objectLabels) {
        return updateRefreshCountIfWatched(CONFIG_MAP_INDEX, objectName, objectLabels);
    }

    /**
     * Increments the refresh count when the given Secret matches a watched import declaration.
     *
     * @param objectName The Secret name
     * @param objectLabels The Secret labels
     * @return {@code true} if the Secret is watched and the refresh count was updated
     */
    static boolean updateRefreshCountIfSecretWatched(@NonNull String objectName,
                                                     @NonNull Map<String, String> objectLabels) {
        return updateRefreshCountIfWatched(SECRET_INDEX, objectName, objectLabels);
    }

    private static boolean updateRefreshCountIfWatched(@NonNull Map<SelectorKey, ImportDeclaration> index,
                                                       @NonNull String objectName,
                                                       @NonNull Map<String, String> objectLabels) {
        return index.entrySet().stream().anyMatch(entry -> {
            SelectorKey key = entry.getKey();
            boolean matchesName = key instanceof SelectorKey.NameKey(String name)
                && Objects.equals(objectName, name);
            boolean matchesLabels = key instanceof SelectorKey.LabelsKey(Map<String, String> labels)
                && CollectionUtils.isNotEmpty(objectLabels)
                && objectLabels.entrySet().containsAll(labels.entrySet());
            if (matchesName || matchesLabels) {
                REFRESH_COUNT.get(entry.getValue()).incrementAndGet();
                return true;
            }
            return false;
        });
    }

    /**
     * @return Whether any ConfigMap watcher-backed imports have been registered
     */
    static boolean isConfigMapWatcherEnabled() {
        return CONFIG_MAP_WATCHER_ENABLED.get();
    }

    /**
     * @return Whether any Secret watcher-backed imports have been registered
     */
    static boolean isSecretWatcherEnabled() {
        return SECRET_WATCHER_ENABLED.get();
    }

    /**
     * Clears all tracked declarations and disables watcher activation flags.
     */
    static void reset() {
        CONFIG_MAP_INDEX.clear();
        SECRET_INDEX.clear();
        REFRESH_COUNT.clear();
        CONFIG_MAP_WATCHER_ENABLED.set(false);
        SECRET_WATCHER_ENABLED.set(false);
    }

    /**
     * Marker type for selector keys used in the watch index.
     */
    sealed interface SelectorKey permits SelectorKey.NameKey, SelectorKey.LabelsKey {

        /**
         * Selector key for exact-name imports.
         *
         * @param name The resource name
         */
        record NameKey(@NonNull String name) implements SelectorKey {
            public NameKey {
                Objects.requireNonNull(name);
            }
        }

        /**
         * Selector key for label-based imports.
         *
         * @param labels The required labels
         */
        record LabelsKey(@NonNull Map<String, String> labels) implements SelectorKey {
            public LabelsKey {
                Objects.requireNonNull(labels);
                labels = Collections.unmodifiableMap(labels);
            }
        }
    }
}
