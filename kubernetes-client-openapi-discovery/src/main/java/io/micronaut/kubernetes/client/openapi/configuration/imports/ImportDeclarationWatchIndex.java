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
package io.micronaut.kubernetes.client.openapi.configuration.imports;

import io.micronaut.core.util.CollectionUtils;
import org.jspecify.annotations.NonNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

final class ImportDeclarationWatchIndex {

    private static final Map<SelectorKey, ImportDeclaration> CONFIG_MAP_INDEX = new ConcurrentHashMap<>();
    private static final Map<SelectorKey, ImportDeclaration> SECRET_INDEX = new ConcurrentHashMap<>();

    private static final AtomicBoolean CONFIG_MAP_WATCHER_ENABLED = new AtomicBoolean(false);
    private static final AtomicBoolean SECRET_WATCHER_ENABLED = new AtomicBoolean(false);

    static void addConfigMapNameDeclaration(@NonNull String name,
                                            @NonNull ImportDeclaration importDeclaration) {
        CONFIG_MAP_INDEX.put(new SelectorKey.NameKey(name), importDeclaration);
        CONFIG_MAP_WATCHER_ENABLED.set(true);
    }

    static void addConfigMapLabelsDeclaration(@NonNull Map<String, String> labels,
                                              @NonNull ImportDeclaration importDeclaration) {
        CONFIG_MAP_INDEX.put(new SelectorKey.LabelsKey(labels), importDeclaration);
        CONFIG_MAP_WATCHER_ENABLED.set(true);
    }

    static void addSecretNameDeclaration(@NonNull String name,
                                         @NonNull ImportDeclaration importDeclaration) {
        SECRET_INDEX.put(new SelectorKey.NameKey(name), importDeclaration);
        SECRET_WATCHER_ENABLED.set(true);
    }

    static void addSecretLabelsDeclaration(@NonNull Map<String, String> labels,
                                           @NonNull ImportDeclaration importDeclaration) {
        SECRET_INDEX.put(new SelectorKey.LabelsKey(labels), importDeclaration);
        SECRET_WATCHER_ENABLED.set(true);
    }

    @NonNull
    static List<ImportDeclaration> removeConfigMapDeclarations(@NonNull String objectName,
                                                               @NonNull Map<String, String> objectLabels) {
        return removeImportDeclarations(CONFIG_MAP_INDEX, objectName, objectLabels);
    }

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

    static boolean isConfigMapWatcherEnabled() {
        return CONFIG_MAP_WATCHER_ENABLED.get();
    }

    static boolean isSecretWatcherEnabled() {
        return SECRET_WATCHER_ENABLED.get();
    }

    sealed interface SelectorKey permits SelectorKey.NameKey, SelectorKey.LabelsKey {

        record NameKey(@NonNull String name) implements SelectorKey {
            public NameKey {
                Objects.requireNonNull(name);
            }
        }

        record LabelsKey(@NonNull Map<String, String> labels) implements SelectorKey {
            public LabelsKey {
                Objects.requireNonNull(labels);
                labels = Collections.unmodifiableMap(labels);
            }
        }
    }
}
