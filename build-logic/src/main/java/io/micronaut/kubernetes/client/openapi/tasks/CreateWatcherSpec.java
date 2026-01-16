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

import org.gradle.api.DefaultTask;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputFile;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.TaskAction;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * <p>The task creates a new openapi spec from the already downloaded spec.</p>
 * <br />
 * <p>The new spec contains only {@code get} operations which contains the {@code watch} parameter.
 * The task also modifies response schemas of the kept operations so each schema references a
 * list of items instead of list object (for example, list of {@code V1Namespace} instead of
 * {@code V1NamespaceList}), which enables creating of reactive responses (for example, {@code Flux<V1Namespace>})
 * which are required for streaming.
 * </p>
 * <br />
 * <p>
 * The task also creates a type mapping file which contains mapping between response types from
 * the new spec file and generic types (for example, {@code "v1.Namespace": "WatchEvent<V1Namespace>"})
 * which will replace the original ones when the open api generator is executed (for example,
 * {@code Flux<WatchEvent<V1Namespace>>}). The generic type is needed since all kubernetes server
 * streamed events have the same wrapper. Examples:
 * <pre>
 *     {"type":"ADDED","object":{"kind":"Namespace","apiVersion":"v1",...}}
 *     {"type":"MODIFIED","object":{"kind":"Secret","apiVersion":"v1",...}}
 *     {"type":"DELETED","object":{"kind":"Pod","apiVersion":"v1",...}}
 * </pre>
 * </p>
 */
@SuppressWarnings("unchecked")
public abstract class CreateWatcherSpec extends DefaultTask {

    private static final String LIST_ITEM_TYPE_MAPPER_CLASS_TEMPLATE = """
        package io.micronaut.kubernetes.client.openapi.watcher.mapper;

        import jakarta.inject.Singleton;
        import java.util.HashMap;
        import java.util.Map;

        @Singleton
        public class DefaultTypeNameMapper implements TypeNameMapper {
            @Override
            public Map<String, String> getMappings() {
                Map<String, String> mappings = new HashMap<>();
        %s
                return mappings;
            }
        }
        """;

    @InputFile
    public abstract RegularFileProperty getInputSpecFile();

    @Input
    public abstract Property<String> getModelPackageName();

    @OutputFile
    public abstract RegularFileProperty getWatcherSpecFile();

    @OutputFile
    public abstract RegularFileProperty getWatcherTypeMappingsFile();

    @OutputFile
    public abstract RegularFileProperty getDefaultTypeNameMapperFile();

    @TaskAction
    void createWatcherSpecFile() throws IOException {
        getLogger().info("Creating kubernetes client watcher openapi spec file");

        Map<String, Object> watcherSpecMap = new LinkedHashMap<>();
        Map<String, String> watcherTypeMappings = new LinkedHashMap<>();
        Map<String, String> listItemTypeMappings = new LinkedHashMap<>();

        Yaml yaml = TaskUtils.createYaml();

        FileReader fileReader = new FileReader(getInputSpecFile().getAsFile().get());
        Map<String, Object> inputSpecMap = yaml.load(fileReader);

        Map<String, Object> components = (Map<String, Object>) inputSpecMap.remove("components");

        inputSpecMap.forEach((specKey, specValue) -> {
            if ("paths".equals(specKey)) {
                Map<String, Object> newPaths = processPaths((Map<String, Object>) specValue, components, watcherTypeMappings, listItemTypeMappings);
                watcherSpecMap.put(specKey, newPaths);
            } else {
                watcherSpecMap.put(specKey, specValue);
            }
        });

        File specFile = getWatcherSpecFile().getAsFile().get();
        TaskUtils.createSpecFile(yaml, watcherSpecMap, specFile);
        getLogger().info("Created kubernetes client watcher openapi spec file: {}", specFile.getAbsolutePath());

        File typeMappingsFile = getWatcherTypeMappingsFile().getAsFile().get();
        TaskUtils.createTypeMappingsFile(watcherTypeMappings, typeMappingsFile);
        getLogger().info("Created kubernetes client watcher openapi type mappings file: {}", typeMappingsFile.getAbsolutePath());

        File defaultTypeNameMapperFile = getDefaultTypeNameMapperFile().getAsFile().get();
        createDefaultTypeNameMapperFile(listItemTypeMappings, defaultTypeNameMapperFile);
        getLogger().info("Created kubernetes client watcher openapi type name mapper java file: {}", defaultTypeNameMapperFile.getAbsolutePath());
    }

    private Map<String, Object> processPaths(Map<String, Object> paths,
                                             Map<String, Object> components,
                                             Map<String, String> watcherTypeMappings,
                                             Map<String, String> listItemTypeMappings) {
        Map<String, Object> newPaths = new LinkedHashMap<>(paths.size());
        paths.forEach((pathKey, pathValue) -> {
            Map<String, Object> operations = (Map<String, Object>) pathValue;
            Map<String, Object> operationData = operations == null ? null : getMapValue(operations, "get");
            if (operationData != null) {
                List<Map<String, Object>> parameters = (List<Map<String, Object>>) operationData.get("parameters");
                if (parameters != null) {
                    boolean watchParamFound = false;
                    for (Map<String, Object> parameterMap : parameters) {
                        if (parameterMap.get("name").equals("watch")) {
                            watchParamFound = true;
                            break;
                        }
                    }
                    if (watchParamFound) {
                        modifyResponseData(getMapValue(operationData, "responses"), components, watcherTypeMappings, listItemTypeMappings);
                        newPaths.put(pathKey,  Map.of("get", operationData));
                    }
                }
            }
        });
        return newPaths;
    }

    /**
     * Modifies response schemas so each reference a list of items instead of list object,
     * for example, list of V1Namespace instead V1NamespaceList.
     *
     * @param responses            map of all responses
     * @param components           map of all components
     * @param watcherTypeMappings  map of type mappings
     * @param listItemTypeMappings map of list types to item types which don't follow pattern where list type name is equal to item type name plus List suffix
     */
    private void modifyResponseData(Map<String, Object> responses,
                                    Map<String, Object> components,
                                    Map<String, String> watcherTypeMappings,
                                    Map<String, String> listItemTypeMappings) {
        Map<String, Object> responseData = getMapValue(responses, "200");
        Map<String, Object> contents = getMapValue(responseData, "content");
        contents.values().forEach(contentValue -> {
            Map<String, Object> contentValueData = (Map<String, Object>) contentValue;
            Map<String, Object> schemaData = getMapValue(contentValueData, "schema");
            if ("object".equals(schemaData.get("type"))) {
                schemaData.clear();
                schemaData.put("type", "array");
                schemaData.put("items",  Map.of("type", "object"));
                watcherTypeMappings.put("Object", "io.micronaut.kubernetes.client.openapi.watcher.WatchEvent<Object>");
                return;
            }
            String schemaRef = (String) schemaData.get("$ref");
            if (schemaRef != null && schemaRef.endsWith("List")) {
                // modify reference from object which represents the list to array of list items (for example, from V1NamespaceList to array of V1Namespace)
                String itemSchemaRef = getItemSchemaRef(schemaRef, components);
                schemaData.clear();
                schemaData.put("type", "array");
                schemaData.put("items",  Map.of("$ref", itemSchemaRef));

                // since openapi 3.0.1 doesn't support generic types, we need to map created types to our generic type
                String itemSchemaType = itemSchemaRef.substring("#/components/schemas/".length());
                String itemJavaType = getJavaType(itemSchemaType);
                watcherTypeMappings.put(itemSchemaType, "io.micronaut.kubernetes.client.openapi.watcher.WatchEvent<" + itemJavaType + ">");

                // if list type name is not equal to item type name plus suffix List, keep list type name to item type name mapping
                String listSchemaType = schemaRef.substring("#/components/schemas/".length());
                String listJavaType = getJavaType(listSchemaType);
                if (!listJavaType.equals(itemJavaType + "List")) {
                    listItemTypeMappings.put(listJavaType, itemJavaType);
                }
            }
        });
    }

    private String getItemSchemaRef(String listSchemaRef, Map<String, Object> components) {
        String schemaType = listSchemaRef.substring("#/components/schemas/".length());
        Map<String, Object> schemas = getMapValue(components, "schemas");
        Map<String, Object> schema = getMapValue(schemas, schemaType);
        Map<String, Object> properties = getMapValue(schema, "properties");
        Map<String, Object> items = getMapValue(properties, "items");
        Map<String, Object> nestedItems = getMapValue(items, "items");
        return (String) nestedItems.get("$ref");
    }

    private String getJavaType(String schemaType) {
        String javaType = Arrays.stream(schemaType.split("\\."))
            .map(word -> Character.toUpperCase(word.charAt(0)) + word.substring(1))
            .collect(Collectors.joining(""));
        return getModelPackageName().get() + "." + javaType;
    }

    private Map<String, Object> getMapValue(Map<String, Object> map, String key) {
        return (Map<String, Object>) map.get(key);
    }

    private void createDefaultTypeNameMapperFile(Map<String, String> typeMappings, File file) throws IOException {
        String mappings = typeMappings.entrySet().stream()
            .map(e -> "        mappings.put(\"" + e.getKey() + "\", \"" + e.getValue() + "\");")
            .collect(Collectors.joining(System.lineSeparator()));

        String javaClass = LIST_ITEM_TYPE_MAPPER_CLASS_TEMPLATE.formatted(mappings);

        try (FileWriter fileWriter = new FileWriter(file)) {
            fileWriter.write(javaClass);
        }
    }
}
