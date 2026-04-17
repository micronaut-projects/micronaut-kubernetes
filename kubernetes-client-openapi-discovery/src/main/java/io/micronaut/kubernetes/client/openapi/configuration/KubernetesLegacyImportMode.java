package io.micronaut.kubernetes.client.openapi.configuration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicBoolean;

final class KubernetesLegacyImportMode {

    private static final Logger LOG = LoggerFactory.getLogger(KubernetesLegacyImportMode.class);

    private static final String DEPRECATION_MESSAGE = "Legacy Kubernetes bootstrap configuration loading is deprecated " +
        "and will be removed in a future release. Please migrate to explicit micronaut.config.import entries.";

    private static final AtomicBoolean CONFIG_MAP_IMPORT_ENABLED = new AtomicBoolean(false);
    private static final AtomicBoolean SECRET_IMPORT_ENABLED = new AtomicBoolean(false);
    private static final AtomicBoolean DEPRECATION_WARNING_LOGGED = new AtomicBoolean(false);

    static void registerConfigMapImport() {
        CONFIG_MAP_IMPORT_ENABLED.set(true);
    }

    static void registerSecretImport() {
        SECRET_IMPORT_ENABLED.set(true);
    }

    static boolean isConfigMapImportEnabled() {
        return CONFIG_MAP_IMPORT_ENABLED.get();
    }

    static boolean isSecretImportEnabled() {
        return SECRET_IMPORT_ENABLED.get();
    }

    static void logLegacyBootstrapDeprecationIfNeeded(boolean legacyBootstrapActive) {
        if (legacyBootstrapActive && !DEPRECATION_WARNING_LOGGED.getAndSet(true)) {
            LOG.warn(DEPRECATION_MESSAGE);
        }
    }
}
