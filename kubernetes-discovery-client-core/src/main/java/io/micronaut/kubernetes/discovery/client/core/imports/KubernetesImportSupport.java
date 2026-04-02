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
package io.micronaut.kubernetes.discovery.client.core.imports;

import io.micronaut.context.exceptions.ConfigurationException;
import io.micronaut.core.annotation.Internal;
import io.micronaut.core.convert.value.ConvertibleValues;
import io.micronaut.core.util.ConnectionString;
import io.micronaut.core.util.StringUtils;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Shared parsing and validation helpers for Kubernetes config import declarations.
 *
 * @since 8.0.0
 */
@Internal
public final class KubernetesImportSupport {

    /**
     * Structured import key for the namespace override.
     */
    public static final String NAMESPACE = "namespace";

    /**
     * Structured import key for label selectors.
     */
    public static final String LABELS = "labels";

    private KubernetesImportSupport() {
    }

    /**
     * Resolve a declaration from a connection string.
     */
    public static Declaration declaration(ConnectionString connectionString,
                                          String defaultNamespace,
                                          String provider) {
        String name = normalize(connectionString.getPath());
        Map<String, String> labels = parseLabels(connectionString.getOptions().get(LABELS), provider);
        validateSelectors(name, labels, provider);
        String namespace = resolveNamespace(connectionString.getOptions().get(NAMESPACE), defaultNamespace, provider);
        return new Declaration(namespace, name, labels, connectionString.isOptional());
    }

    /**
     * Resolve a declaration from map-based import values.
     */
    public static Declaration declaration(ConvertibleValues<Object> values,
                                          String defaultNamespace,
                                          String provider) {
        String name = normalize(values.get("path", String.class).orElse(null));
        Map<String, String> labels = parseLabels(values.get(LABELS, String.class).orElse(null), provider);
        validateSelectors(name, labels, provider);
        String namespace = resolveNamespace(values.get(NAMESPACE, String.class).orElse(null), defaultNamespace, provider);
        boolean optional = values.get("optional", Boolean.class).orElse(false);
        return new Declaration(namespace, name, labels, optional);
    }

    /**
     * Resolve auth-related child context properties from either scalar or map-based import syntax.
     *
     * @param connectionString The connection string, if present
     * @param values Structured values, if present
     * @param prefix The configuration prefix to populate
     * @return Context properties to apply when building a child application context
     */
    public static Map<String, Object> authenticationProperties(ConnectionString connectionString,
                                                               ConvertibleValues<Object> values,
                                                               String prefix) {
        Map<String, Object> properties = new LinkedHashMap<>();
        copy(values, properties, "master-url", prefix + ".master-url");
        copy(values, properties, "namespace", prefix + ".namespace");
        copy(values, properties, "service-account-token-path", prefix + ".service-account-token-path");
        copy(values, properties, "service-account-token", prefix + ".service-account-token");
        copy(values, properties, "ca-path", prefix + ".ca-path");
        copy(values, properties, "ca-crt-data", prefix + ".ca-crt-data");
        copy(values, properties, "client-certificate-path", prefix + ".client-certificate-path");
        copy(values, properties, "client-key-path", prefix + ".client-key-path");
        copy(values, properties, "client-certificate-data", prefix + ".client-certificate-data");
        copy(values, properties, "client-key-data", prefix + ".client-key-data");
        copy(values, properties, "username", prefix + ".username");
        copy(values, properties, "password", prefix + ".password");
        copy(values, properties, "token", prefix + ".token");
        copy(values, properties, "insecure-skip-tls-verify", prefix + ".insecure-skip-tls-verify");
        copy(values, properties, "kube-config-path", prefix + ".kube-config-path");
        if (connectionString != null) {
            connectionString.getOptions().forEach((key, value) -> copy(value, key, properties, prefix));
        }
        return Map.copyOf(properties);
    }

    /**
     * Shared parsed declaration fields.
     */
    @Internal
    public record Declaration(String namespace, String name, Map<String, String> labels, boolean optional) {
    }

    private static void copy(ConvertibleValues<Object> values,
                             Map<String, Object> target,
                             String sourceKey,
                             String targetKey) {
        values.get(sourceKey, Object.class).ifPresent(value -> target.put(targetKey, value));
    }

    private static void copy(String value,
                             String sourceKey,
                             Map<String, Object> target,
                             String prefix) {
        if (value != null && AUTH_OPTION_KEYS.containsKey(sourceKey)) {
            target.put(prefix + AUTH_OPTION_KEYS.get(sourceKey), value);
        }
    }

    private static final Map<String, String> AUTH_OPTION_KEYS = Map.ofEntries(
        Map.entry("master-url", ".master-url"),
        Map.entry("namespace", ".namespace"),
        Map.entry("service-account-token-path", ".service-account-token-path"),
        Map.entry("service-account-token", ".service-account-token"),
        Map.entry("ca-path", ".ca-path"),
        Map.entry("ca-crt-data", ".ca-crt-data"),
        Map.entry("client-certificate-path", ".client-certificate-path"),
        Map.entry("client-key-path", ".client-key-path"),
        Map.entry("client-certificate-data", ".client-certificate-data"),
        Map.entry("client-key-data", ".client-key-data"),
        Map.entry("username", ".username"),
        Map.entry("password", ".password"),
        Map.entry("token", ".token"),
        Map.entry("insecure-skip-tls-verify", ".insecure-skip-tls-verify"),
        Map.entry("kube-config-path", ".kube-config-path")
    );

    private static void validateSelectors(String name, Map<String, String> labels, String provider) {
        boolean hasName = StringUtils.isNotEmpty(name);
        boolean hasLabels = labels != null && !labels.isEmpty();
        if (hasName == hasLabels) {
            throw new ConfigurationException("Config import provider [" + provider + "] requires exactly one selector: resource name path or ['labels']");
        }
    }

    private static String resolveNamespace(String namespace, String defaultNamespace, String provider) {
        String resolved = normalize(namespace);
        if (resolved != null) {
            return resolved;
        }
        resolved = normalize(defaultNamespace);
        if (resolved != null) {
            return resolved;
        }
        throw new ConfigurationException("Config import provider [" + provider + "] requires a Kubernetes namespace");
    }

    private static Map<String, String> parseLabels(String labelsValue, String provider) {
        String normalized = normalize(labelsValue);
        if (normalized == null) {
            return null;
        }
        Map<String, String> labels = new LinkedHashMap<>();
        for (String token : normalized.split(",")) {
            String entry = token.trim();
            if (entry.isEmpty()) {
                continue;
            }
            int equalsIndex = entry.indexOf('=');
            if (equalsIndex <= 0 || equalsIndex == entry.length() - 1) {
                throw new ConfigurationException("Config import provider [" + provider + "] requires labels in the form key=value");
            }
            String key = entry.substring(0, equalsIndex).trim();
            String value = entry.substring(equalsIndex + 1).trim();
            if (key.isEmpty() || value.isEmpty()) {
                throw new ConfigurationException("Config import provider [" + provider + "] requires labels in the form key=value");
            }
            labels.put(key, value);
        }
        if (labels.isEmpty()) {
            throw new ConfigurationException("Config import provider [" + provider + "] requires a non-blank ['labels']");
        }
        return Map.copyOf(labels);
    }

    private static String normalize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
