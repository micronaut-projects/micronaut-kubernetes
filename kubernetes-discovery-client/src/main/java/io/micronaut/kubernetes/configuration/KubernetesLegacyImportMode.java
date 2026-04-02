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
