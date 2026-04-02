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
package io.micronaut.kubernetes.imports;

import io.micronaut.context.env.PropertySource;
import io.micronaut.context.env.PropertySourceImporter;
import io.micronaut.core.annotation.Internal;
import io.micronaut.core.convert.value.ConvertibleValues;
import io.micronaut.kubernetes.KubernetesConfiguration;
import io.micronaut.kubernetes.discovery.client.core.imports.AbstractKubernetesPropertySourceImporter;
import io.micronaut.kubernetes.discovery.client.core.imports.KubernetesImportSupport;
import io.micronaut.core.util.ConnectionString;
import org.jspecify.annotations.Nullable;

import java.util.Optional;

/**
 * Imports Kubernetes ConfigMaps for the classic Kubernetes discovery client.
 *
 * <p>This importer is created by Micronaut through {@link java.util.ServiceLoader}, so it must not rely on bean
 * construction or dependency injection. It lazily creates a child {@link ApplicationContext} for each configuration
 * loading cycle and closes it via {@link #close()} when the importer lifecycle ends.</p>
 *
 * @since 8.0.0
 */
@Internal
public final class KubernetesConfigMapPropertySourceImporter extends AbstractKubernetesPropertySourceImporter<KubernetesConfigMapImport, KubernetesConfigMapImportSupport> {

    static final String PROVIDER = "kubernetes-configmap";

    @Override
    public String getProvider() {
        return PROVIDER;
    }

    @Override
    protected Class<KubernetesConfigMapImportSupport> getSupportType() {
        return KubernetesConfigMapImportSupport.class;
    }


    @Override
    protected java.util.Map<String, Object> contextProperties(ImportContext<KubernetesConfigMapImport> context) {
        return KubernetesImportSupport.authenticationProperties(context.connectionString(), ConvertibleValues.empty(), KubernetesConfiguration.PREFIX);
    }

    @Override
    protected KubernetesConfigMapImport createDeclaration(KubernetesImportSupport.Declaration declaration) {
        return new KubernetesConfigMapImport(declaration.namespace(), declaration.name(), declaration.labels(), declaration.optional());
    }

    @Override
    protected KubernetesConfigMapImport resolveDeclaration(KubernetesConfigMapImportSupport support,
                                                           KubernetesConfigMapImport declaration,
                                                           @Nullable ConnectionString connectionString,
                                                           ConvertibleValues<Object> values) {
        return support.resolve(declaration, connectionString, values);
    }

    @Override
    protected Optional<PropertySource> importPropertySource(KubernetesConfigMapImportSupport support,
                                                            ImportContext<KubernetesConfigMapImport> context) {
        return support.importPropertySource(context);
    }
}
