/*
 * Copyright 2017-2026 original authors
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
package io.micronaut.kubernetes.client.openapi.configuration.imports;

import io.micronaut.context.env.PropertySource;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

final class PropertySourceCache {

    private static final Map<ImportDeclaration, PropertySource> PROPERTY_SOURCES = new ConcurrentHashMap<>();

    static void add(ImportDeclaration importDeclaration, PropertySource propertySource) {
        PROPERTY_SOURCES.put(importDeclaration, propertySource);
    }

    static PropertySource get(ImportDeclaration importDeclaration) {
        return PROPERTY_SOURCES.get(importDeclaration);
    }

    static void remove(ImportDeclaration importDeclaration) {
        PROPERTY_SOURCES.remove(importDeclaration);
    }

    static Map<ImportDeclaration, PropertySource> get() {
        return PROPERTY_SOURCES;
    }
}
