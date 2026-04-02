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

import io.kubernetes.client.openapi.models.V1Secret;
import io.micronaut.context.exceptions.ConfigurationException;
import io.micronaut.context.env.PropertySource;
import io.micronaut.context.env.PropertySourceImporter;
import io.micronaut.core.annotation.Internal;
import io.micronaut.core.convert.value.ConvertibleValues;
import io.micronaut.core.util.ConnectionString;
import io.micronaut.kubernetes.KubernetesConfiguration;
import io.micronaut.kubernetes.client.reactor.CoreV1ApiReactorClient;
import io.micronaut.kubernetes.configuration.KubernetesConfigurationClient;
import io.micronaut.kubernetes.configuration.KubernetesLegacyImportMode;
import io.micronaut.kubernetes.configuration.KubernetesLegacyImportMode.LegacyType;
import io.micronaut.kubernetes.discovery.client.core.imports.KubernetesImportSupport;
import io.micronaut.kubernetes.util.KubernetesUtils;
import jakarta.inject.Singleton;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Optional;

@Internal
@Singleton
final class KubernetesSecretImportSupport {

    static final String NAMESPACE = "namespace";
    static final String LABELS = "labels";

    private final CoreV1ApiReactorClient client;
    private final KubernetesConfiguration configuration;
    private final KubernetesLegacyImportMode legacyImportMode;

    KubernetesSecretImportSupport(CoreV1ApiReactorClient client,
                                  KubernetesConfiguration configuration,
                                  KubernetesLegacyImportMode legacyImportMode) {
        this.client = client;
        this.configuration = configuration;
        this.legacyImportMode = legacyImportMode;
    }

    KubernetesSecretImport newImportDeclaration(ConnectionString connectionString) {
        return resolve(new KubernetesSecretImport(connectionString.getOptions().get(NAMESPACE),
            connectionString.getPath(),
            KubernetesImportSupport.declaration(connectionString, null, KubernetesSecretPropertySourceImporter.PROVIDER).labels(),
            connectionString.isOptional()), connectionString, ConvertibleValues.empty());
    }

    KubernetesSecretImport newImportDeclaration(ConvertibleValues<Object> values) {
        return resolve(new KubernetesSecretImport(values.get(NAMESPACE, String.class).orElse(null),
            values.get("path", String.class).orElse(null),
            KubernetesImportSupport.declaration(values, null, KubernetesSecretPropertySourceImporter.PROVIDER).labels(),
            values.get("optional", Boolean.class).orElse(false)), null, values);
    }

    KubernetesSecretImport resolve(KubernetesSecretImport declaration,
                                   ConnectionString connectionString,
                                   ConvertibleValues<Object> values) {
        KubernetesImportSupport.Declaration resolved = connectionString != null
            ? KubernetesImportSupport.declaration(connectionString, configuration.getNamespace(), KubernetesSecretPropertySourceImporter.PROVIDER)
            : KubernetesImportSupport.declaration(values, configuration.getNamespace(), KubernetesSecretPropertySourceImporter.PROVIDER);
        return new KubernetesSecretImport(resolved.namespace(), resolved.name(), resolved.labels(), declaration.optional());
    }

    Optional<PropertySource> importPropertySource(PropertySourceImporter.ImportContext<KubernetesSecretImport> context) {
        legacyImportMode.registerExplicitImport(LegacyType.SECRET);
        KubernetesSecretImport declaration = context.importDeclaration();
        if (declaration.isExactName()) {
            return importExactName(declaration);
        }
        return importLabels(declaration);
    }

    private Optional<PropertySource> importExactName(KubernetesSecretImport declaration) {
        V1Secret secret = Flux.from(client.listNamespacedSecret(declaration.namespace()).execute())
            .flatMapIterable(secretList -> secretList.getItems() == null ? java.util.List.<V1Secret>of() : secretList.getItems())
            .filter(candidate -> candidate.getMetadata() != null)
            .filter(candidate -> declaration.name().equals(candidate.getMetadata().getName()))
            .next()
            .block();
        if (secret == null) {
            return Optional.empty();
        }
        if (!KubernetesConfigurationClient.OPAQUE_SECRET_TYPE.equals(secret.getType())) {
            if (declaration.optional()) {
                return Optional.empty();
            }
            throw new ConfigurationException("Config import provider [" + KubernetesSecretPropertySourceImporter.PROVIDER + "] requires Kubernetes Secret [" + declaration.name() + "] in namespace [" + declaration.namespace() + "] to be of type [" + KubernetesConfigurationClient.OPAQUE_SECRET_TYPE + "]");
        }
        return Optional.of(KubernetesUtils.secretAsPropertySource(secret));
    }

    private Optional<PropertySource> importLabels(KubernetesSecretImport declaration) {
        String labelSelector = declaration.labels().entrySet().stream()
            .map(entry -> entry.getKey() + "=" + entry.getValue())
            .reduce((left, right) -> left + "," + right)
            .orElseThrow();
        return client.listNamespacedSecret(declaration.namespace())
            .labelSelector(labelSelector)
            .execute()
            .flatMapIterable(secretList -> secretList.getItems() == null ? java.util.List.<V1Secret>of() : secretList.getItems())
            .filter(secret -> KubernetesConfigurationClient.OPAQUE_SECRET_TYPE.equals(secret.getType()))
            .next()
            .map(KubernetesUtils::secretAsPropertySource)
            .switchIfEmpty(Mono.empty())
            .blockOptional();
    }

}
