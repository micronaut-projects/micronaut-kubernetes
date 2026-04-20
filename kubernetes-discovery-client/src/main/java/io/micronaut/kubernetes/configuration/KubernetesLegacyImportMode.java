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
package io.micronaut.kubernetes.configuration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Tracks use of legacy Kubernetes bootstrap imports and emits a deprecation warning when needed.
 */
final class KubernetesLegacyImportMode {

    private static final Logger LOG = LoggerFactory.getLogger(KubernetesLegacyImportMode.class);

    private static final String DEPRECATION_MESSAGE = "Legacy Kubernetes bootstrap configuration loading is deprecated " +
        "and will be removed in a future release. Please migrate to explicit micronaut.config.import entries.";

    private static final AtomicBoolean CONFIG_MAP_IMPORT_ENABLED = new AtomicBoolean(false);
    private static final AtomicBoolean SECRET_IMPORT_ENABLED = new AtomicBoolean(false);
    private static final AtomicBoolean DEPRECATION_WARNING_LOGGED = new AtomicBoolean(false);

    /**
     * Marks ConfigMap import support as active for the current application run.
     */
    static void registerConfigMapImport() {
        CONFIG_MAP_IMPORT_ENABLED.set(true);
    }

    /**
     * Marks Secret import support as active for the current application run.
     */
    static void registerSecretImport() {
        SECRET_IMPORT_ENABLED.set(true);
    }

    /**
     * @return Whether ConfigMap import support is active for the current application run
     */
    static boolean isConfigMapImportEnabled() {
        return CONFIG_MAP_IMPORT_ENABLED.get();
    }

    /**
     * @return Whether Secret import support is active for the current application run
     */
    static boolean isSecretImportEnabled() {
        return SECRET_IMPORT_ENABLED.get();
    }

    /**
     * Logs the legacy bootstrap deprecation warning once when legacy bootstrap mode is active.
     *
     * @param legacyBootstrapActive Whether legacy bootstrap configuration loading is active
     */
    static void logLegacyBootstrapDeprecationIfNeeded(boolean legacyBootstrapActive) {
        if (legacyBootstrapActive && !DEPRECATION_WARNING_LOGGED.getAndSet(true)) {
            LOG.warn(DEPRECATION_MESSAGE);
        }
    }
}
