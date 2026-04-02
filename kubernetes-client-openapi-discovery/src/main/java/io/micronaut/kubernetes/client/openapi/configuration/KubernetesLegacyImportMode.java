package io.micronaut.kubernetes.client.openapi.configuration;

import io.micronaut.core.annotation.Internal;
import jakarta.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Internal
@Singleton
public final class KubernetesLegacyImportMode {

    private static final Logger LOG = LoggerFactory.getLogger(KubernetesLegacyImportMode.class);

    private static final String DEPRECATION_MESSAGE = "Legacy Kubernetes bootstrap configuration loading is deprecated and will be removed in a future release. Migrate to explicit micronaut.config.import entries for the remaining active legacy types.";

    private final Set<LegacyType> explicitImports = ConcurrentHashMap.newKeySet();
    private volatile boolean deprecationWarningLogged;

    public enum LegacyType {
        CONFIG_MAP("ConfigMap", "kubernetes-configmap"),
        SECRET("Secret", "kubernetes-secret");

        private final String displayName;
        private final String provider;

        LegacyType(String displayName, String provider) {
            this.displayName = displayName;
            this.provider = provider;
        }

        public String getDisplayName() {
            return displayName;
        }

        public String getProvider() {
            return provider;
        }
    }

    public void registerExplicitImport(LegacyType legacyType) {
        explicitImports.add(legacyType);
    }

    public boolean isLegacyBootstrapEnabled(LegacyType legacyType) {
        return !explicitImports.contains(legacyType);
    }

    public void logLegacyBootstrapDeprecationIfNeeded(boolean legacyBootstrapActive) {
        if (legacyBootstrapActive && !deprecationWarningLogged) {
            synchronized (this) {
                if (!deprecationWarningLogged) {
                    LOG.warn(DEPRECATION_MESSAGE);
                    deprecationWarningLogged = true;
                }
            }
        }
    }
}
