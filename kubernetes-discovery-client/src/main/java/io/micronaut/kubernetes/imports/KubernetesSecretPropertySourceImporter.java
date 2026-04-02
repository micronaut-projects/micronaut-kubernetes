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
import io.micronaut.core.annotation.Internal;
import io.micronaut.core.convert.value.ConvertibleValues;
import io.micronaut.core.util.ConnectionString;
import io.micronaut.kubernetes.KubernetesConfiguration;
import io.micronaut.kubernetes.discovery.client.core.imports.AbstractKubernetesPropertySourceImporter;
import io.micronaut.kubernetes.discovery.client.core.imports.KubernetesImportSupport;
import org.jspecify.annotations.Nullable;

import java.util.Map;
import java.util.Optional;

/**
 * Imports Kubernetes Secrets for the classic Kubernetes discovery client.
 *
 * @since 8.0.0
 */
@Internal
public final class KubernetesSecretPropertySourceImporter extends AbstractKubernetesPropertySourceImporter<KubernetesSecretImport, KubernetesSecretImportSupport> {

    static final String PROVIDER = "kubernetes-secret";

    @Override
    public String getProvider() {
        return PROVIDER;
    }

    @Override
    protected Class<KubernetesSecretImportSupport> getSupportType() {
        return KubernetesSecretImportSupport.class;
    }

    @Override
    protected Map<String, Object> contextProperties(ImportContext<KubernetesSecretImport> context) {
        return KubernetesImportSupport.authenticationProperties(context.connectionString(), ConvertibleValues.empty(), KubernetesConfiguration.PREFIX);
    }

    @Override
    protected KubernetesSecretImport createDeclaration(KubernetesImportSupport.Declaration declaration) {
        return new KubernetesSecretImport(declaration.namespace(), declaration.name(), declaration.labels(), declaration.optional());
    }

    @Override
    protected KubernetesSecretImport resolveDeclaration(KubernetesSecretImportSupport support,
                                                        KubernetesSecretImport declaration,
                                                        @Nullable ConnectionString connectionString,
                                                        ConvertibleValues<Object> values) {
        return support.resolve(declaration, connectionString, values);
    }

    @Override
    protected Optional<PropertySource> importPropertySource(KubernetesSecretImportSupport support,
                                                            ImportContext<KubernetesSecretImport> context) {
        return support.importPropertySource(context);
    }
}
