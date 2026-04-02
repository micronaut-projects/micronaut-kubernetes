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

import io.micronaut.context.ApplicationContext;
import io.micronaut.context.env.PropertySource;
import io.micronaut.context.env.PropertySourceImporter;
import io.micronaut.core.annotation.Internal;
import io.micronaut.core.convert.value.ConvertibleValues;
import io.micronaut.core.io.ResourceLoader;
import io.micronaut.core.util.ConnectionString;
import org.jspecify.annotations.Nullable;

import java.util.Map;
import java.util.Optional;

/**
 * Base service-loader-safe importer implementation for Kubernetes import providers.
 *
 * <p>{@link PropertySourceImporter} instances are created through Java service loading rather than dependency
 * injection. Subclasses therefore expose a zero-argument constructor and delegate runtime bean lookups to a child
 * {@link ApplicationContext} created lazily for a single configuration loading cycle. The child context is closed from
 * {@link #close()} when Micronaut finishes using the importer instance.</p>
 *
 * @param <D> The typed import declaration
 * @param <S> The support type resolved from the child context
 * @since 8.0.0
 */
@Internal
public abstract class AbstractKubernetesPropertySourceImporter<D extends AbstractKubernetesImport, S> implements PropertySourceImporter<D> {

    private ApplicationContext applicationContext;
    private S support;

    @Override
    public final D newImportDeclaration(ConnectionString connectionString) {
        return createDeclaration(KubernetesImportSupport.declaration(connectionString, null, getProvider()));
    }

    @Override
    public final D newImportDeclaration(ConvertibleValues<Object> values) {
        return createDeclaration(KubernetesImportSupport.declaration(values, null, getProvider()));
    }

    @Override
    public final Optional<PropertySource> importPropertySource(ImportContext<D> context) {
        S resolvedSupport = getSupport(context);
        D resolvedDeclaration = resolveDeclaration(resolvedSupport, context.importDeclaration(), context.connectionString(), ConvertibleValues.empty());
        return importPropertySource(resolvedSupport, new DelegatingImportContext<>(context, resolvedDeclaration));
    }

    @Override
    public final void close() {
        if (applicationContext != null) {
            applicationContext.close();
            applicationContext = null;
            support = null;
        }
    }

    /**
     * @return The support bean type to resolve from the child application context.
     */
    protected abstract Class<S> getSupportType();

    /**
     * @param context The import context
     * @return The child context properties required to bootstrap the module-specific Kubernetes client beans.
     */
    protected abstract Map<String, Object> contextProperties(ImportContext<D> context);

    /**
     * @param declaration The parsed declaration fields from the import syntax
     * @return The module-specific typed declaration instance.
     */
    protected abstract D createDeclaration(KubernetesImportSupport.Declaration declaration);

    /**
     * Resolve a declaration against runtime configuration available from the child context.
     *
     * @param support The resolved support bean
     * @param declaration The initially parsed declaration
     * @param connectionString The original connection string, if present
     * @param values The original map-based values, if present
     * @return The fully resolved declaration used for import execution
     */
    protected abstract D resolveDeclaration(S support,
                                            D declaration,
                                            @Nullable ConnectionString connectionString,
                                            ConvertibleValues<Object> values);

    /**
     * Execute the import using the fully resolved support bean and declaration.
     *
     * @param support The resolved support bean
     * @param context The import context containing the resolved declaration
     * @return The imported property source, if one was found
     */
    protected abstract Optional<PropertySource> importPropertySource(S support,
                                                                     ImportContext<D> context);

    private S getSupport(ImportContext<D> context) {
        if (support == null) {
            applicationContext = ApplicationContext.builder()
                .properties(contextProperties(context))
                .environments(context.environment().getActiveNames().toArray(String[]::new))
                .start();
            support = applicationContext.getBean(getSupportType());
        }
        return support;
    }

    private record DelegatingImportContext<D>(ImportContext<D> delegate,
                                              D importDeclaration) implements ImportContext<D> {
        @Override
        public io.micronaut.context.env.Environment environment() {
            return delegate.environment();
        }

        @Override
        public ConnectionString connectionString() {
            return delegate.connectionString();
        }

        @Override
        public io.micronaut.context.env.PropertySource.Origin parentOrigin() {
            return delegate.parentOrigin();
        }

        @Override
        public Optional<PropertySource> importPropertySource(ResourceLoader resourceLoader,
                                                             String resourcePath,
                                                             String sourceName,
                                                             io.micronaut.context.env.PropertySource.Origin origin) {
            return delegate.importPropertySource(resourceLoader, resourcePath, sourceName, origin);
        }

        @Override
        public Optional<PropertySource> importPropertySource(String content,
                                                             String sourceName,
                                                             String extension,
                                                             io.micronaut.context.env.PropertySource.Origin origin) {
            return delegate.importPropertySource(content, sourceName, extension, origin);
        }

        @Override
        public Optional<PropertySource> importClasspathPropertySource(String resourcePath,
                                                                      String sourceName,
                                                                      io.micronaut.context.env.PropertySource.Origin origin,
                                                                      boolean allowMultiple) {
            return delegate.importClasspathPropertySource(resourcePath, sourceName, origin, allowMultiple);
        }
    }
}
