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
package io.micronaut.kubernetes.client;

import io.micronaut.context.annotation.BootstrapContextCompatible;
import io.micronaut.context.annotation.Value;
import io.micronaut.core.annotation.Nullable;
import jakarta.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * Default implemention of {@link NamespaceResolver}. The resolution of namespace is evaluated in this order:
 * <ol>
 *     <li>Value configured by `kubernetes.client.namespace`.</li>
 *     <li>If the application is running inside a pod then the namespace is read from file `/var/run/secrets/kubernetes.io/serviceaccount/namespace`.</li>
 *     <li>Namespace is set do `default`.</li>
 * </ol>
 *
 * @author Pavol Gressa
 * @since 3.1
 */
@Singleton
@BootstrapContextCompatible
public class DefaultNamespaceResolver implements NamespaceResolver {

    public static final String DEFAULT_NAMESPACE = "default";
    public static final String NAMESPACE_PATH = "/var/run/secrets/kubernetes.io/serviceaccount/namespace";
    private static final Logger LOG = LoggerFactory.getLogger(DefaultNamespaceResolver.class);
    private final String namespace;

    public DefaultNamespaceResolver(@Nullable @Value("${kubernetes.client.namespace}") String namespace) {
        String resolvedNamespace = namespace;
        if (resolvedNamespace == null) {
            try {
                if (LOG.isTraceEnabled()) {
                    LOG.trace("Namespace has not been set. Reading it from file [{}]", NAMESPACE_PATH);
                }
                resolvedNamespace = new String(Files.readAllBytes(Paths.get(NAMESPACE_PATH)), StandardCharsets.UTF_8);
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Namespace: [{}]", resolvedNamespace);
                }
            } catch (IOException ioe) {
                LOG.warn("An error has occurred when reading the file: [{}]. Kubernetes namespace will be set to: {}", NAMESPACE_PATH, DEFAULT_NAMESPACE);
                resolvedNamespace = DEFAULT_NAMESPACE;
            }
        }
        this.namespace = resolvedNamespace;
    }

    @Override
    public String resolveNamespace() {
        return this.namespace;
    }
}
