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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Tracks watched import declarations by selector so watcher events can invalidate matching imports.
 */
final class ImportDeclarationWatchIndex {

    private static final Map<SelectorKey, ImportDeclaration> CONFIG_MAP_INDEX = new ConcurrentHashMap<>();
    private static final Map<SelectorKey, ImportDeclaration> SECRET_INDEX = new ConcurrentHashMap<>();

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
        CONFIG_MAP_INDEX.put(new SelectorKey.NameKey(name), importDeclaration);
        CONFIG_MAP_WATCHER_ENABLED.set(true);
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
        CONFIG_MAP_WATCHER_ENABLED.set(true);
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
        SECRET_WATCHER_ENABLED.set(true);
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
        SECRET_WATCHER_ENABLED.set(true);
    }

    /**
     * Removes watched ConfigMap declarations matching the changed object.
     *
     * @param objectName   The changed ConfigMap name
     * @param objectLabels The changed ConfigMap labels
     * @return The removed declarations
     */
    @NonNull
    static List<ImportDeclaration> removeConfigMapDeclarations(@NonNull String objectName,
                                                               @NonNull Map<String, String> objectLabels) {
        return removeImportDeclarations(CONFIG_MAP_INDEX, objectName, objectLabels);
    }

    /**
     * Removes watched Secret declarations matching the changed object.
     *
     * @param objectName   The changed Secret name
     * @param objectLabels The changed Secret labels
     * @return The removed declarations
     */
    @NonNull
    static List<ImportDeclaration> removeSecretDeclarations(@NonNull String objectName,
                                                            @NonNull Map<String, String> objectLabels) {
        return removeImportDeclarations(SECRET_INDEX, objectName, objectLabels);
    }

    @NonNull
    private static List<ImportDeclaration> removeImportDeclarations(@NonNull Map<SelectorKey, ImportDeclaration> index,
                                                                    @NonNull String objectName,
                                                                    @NonNull Map<String, String> objectLabels) {
        List<ImportDeclaration> removed = new ArrayList<>();
            index.entrySet().removeIf(e -> {
            if (e.getKey() instanceof SelectorKey.NameKey(String name)
                && Objects.equals(objectName, name)) {
                removed.add(e.getValue());
                return true;
            }

            if (e.getKey() instanceof SelectorKey.LabelsKey(Map<String, String> labels)
                && CollectionUtils.isNotEmpty(objectLabels)
                && objectLabels.entrySet().containsAll(labels.entrySet())) {
                removed.add(e.getValue());
                return true;
            }
            return false;
        });
        return removed;
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
