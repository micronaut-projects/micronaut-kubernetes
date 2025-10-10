/*
 * Copyright 2017-2025 original authors
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
package io.micronaut.kubernetes.client.openapi.tasks;

import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;
import org.yaml.snakeyaml.representer.Representer;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Utility class for openapi tasks.
 */
public class TaskUtils {

    private static final int CODE_POINT_LIMIT = 15728640;

    /**
     * Creates and configures yaml.
     *
     * @return a {@link Yaml} instance
     */
    static Yaml createYaml() {
        LoaderOptions loaderOptions = new LoaderOptions();
        loaderOptions.setCodePointLimit(CODE_POINT_LIMIT);
        DumperOptions dumperOptions = new DumperOptions();
        dumperOptions.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        return new Yaml(new SafeConstructor(loaderOptions), new Representer(dumperOptions), dumperOptions);
    }

    /**
     * Creates a new spec file from given content.
     *
     * @param yaml    the yaml
     * @param content the content
     * @param file    the new file
     * @throws IOException exception thrown when a new file cannot be created
     */
    static void createSpecFile(Yaml yaml, Map<String, Object> content, File file) throws IOException {
        try (FileWriter fileWriter = new FileWriter(file)) {
            yaml.dump(content, fileWriter);
        }
    }

    /**
     * Creates a new type mappings file.
     *
     * @param typeMappings the type mappings
     * @param file         the new file
     * @throws IOException exception thrown when a new file cannot be created
     */
    static void createTypeMappingsFile(Map<String, String> typeMappings, File file) throws IOException {
        String typeMappingsStr = typeMappings.entrySet()
            .stream()
            .map(entry -> "\"" + entry.getKey() + "\": \"" + entry.getValue() + "\"")
            .collect(Collectors.joining(", "));
        try (FileWriter fileWriter = new FileWriter(file)) {
            fileWriter.write("[" + typeMappingsStr + "]");
        }
    }
}
