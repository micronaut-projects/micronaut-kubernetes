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

    @InputFile
    public abstract RegularFileProperty getInputSpecFile();

    @Input
    public abstract Property<String> getModelPackageName();

    @OutputFile
    public abstract RegularFileProperty getWatcherSpecFile();

    @OutputFile
    public abstract RegularFileProperty getWatcherTypeMappingsFile();

    @TaskAction
    void createWatcherSpecFile() throws IOException {
        getLogger().info("Creating kubernetes client watcher openapi spec file");

        Map<String, Object> watcherSpecMap = new LinkedHashMap<>();
        Map<String, String> watcherTypeMappings = new LinkedHashMap<>();

        Yaml yaml = TaskUtils.createYaml();

        FileReader fileReader = new FileReader(getInputSpecFile().getAsFile().get());
        Map<String, Object> inputSpecMap = yaml.load(fileReader);

        inputSpecMap.forEach((specKey, specValue) -> {
            if ("paths".equals(specKey)) {
                Map<String, Object> newPaths = processPaths((Map<String, Object>) specValue, watcherTypeMappings);
                watcherSpecMap.put(specKey, newPaths);
            } else if (!"components".equals(specKey)) {
                watcherSpecMap.put(specKey, specValue);
            }
        });

        File specFile = getWatcherSpecFile().getAsFile().get();
        TaskUtils.createSpecFile(yaml, watcherSpecMap, specFile);
        getLogger().info("Created kubernetes client watcher openapi spec file: {}", specFile.getAbsolutePath());

        File typeMappingsFile = getWatcherTypeMappingsFile().getAsFile().get();
        TaskUtils.createTypeMappingsFile(watcherTypeMappings, typeMappingsFile);
        getLogger().info("Created kubernetes client watcher openapi type mappings file: {}", typeMappingsFile.getAbsolutePath());
    }

    private Map<String, Object> processPaths(Map<String, Object> paths, Map<String, String> watcherTypeMappings) {
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
                        modifyResponseData(getMapValue(operationData, "responses"), watcherTypeMappings);
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
     * @param responses           map of all responses
     * @param watcherTypeMappings map of type mappings
     */
    private void modifyResponseData(Map<String, Object> responses, Map<String, String> watcherTypeMappings) {
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
                // modify reference from object list to array of items (for example, from V1NamespaceList to array of V1Namespace)
                String schemaItemRef = schemaRef.substring(0, schemaRef.indexOf("List"));
                schemaData.clear();
                schemaData.put("type", "array");
                schemaData.put("items",  Map.of("$ref", schemaItemRef));
                // since openapi 3.0.1 doesn't support generic types, we need to map created types to our generic type
                String schemaType = schemaItemRef.substring("#/components/schemas/".length());
                String newSchemaType = Arrays.stream(schemaType.split("\\."))
                    .map(word -> Character.toUpperCase(word.charAt(0)) + word.substring(1))
                    .collect(Collectors.joining(""));
                String fullPath = getModelPackageName().get() + "." + newSchemaType;
                watcherTypeMappings.put(schemaType, "io.micronaut.kubernetes.client.openapi.watcher.WatchEvent<" + fullPath + ">");
            }
        });
    }

    private Map<String, Object> getMapValue(Map<String, Object> map, String key) {
        return (Map<String, Object>) map.get(key);
    }
}
