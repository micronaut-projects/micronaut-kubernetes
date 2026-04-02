package io.micronaut.kubernetes.client.openapi.configuration;

import io.micronaut.core.annotation.Internal;
import io.micronaut.kubernetes.discovery.client.core.imports.AbstractKubernetesImport;

import java.util.Map;

/**
 * Typed Secret import declaration for the OpenAPI Kubernetes discovery client.
 *
 * @param namespace The resolved namespace
 * @param name The exact Secret name, or {@code null} when importing by labels
 * @param labels The label selector map, or {@code null} when importing by exact name
 * @param optional Whether the import is optional
 * @since 8.0.0
 */
@Internal
record KubernetesSecretImport(String namespace,
                              String name,
                              Map<String, String> labels,
                              boolean optional) implements AbstractKubernetesImport {
}
