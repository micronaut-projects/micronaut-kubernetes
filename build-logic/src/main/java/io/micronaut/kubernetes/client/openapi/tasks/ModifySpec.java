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
 * <p>The main difference between the downloaded spec and the new spec is in response schemas for
 * delete resource operations. In the downloaded spec, response schemas are {@code v1.Status} or
 * schema of object which is being deleted (for example, {@code v1.ConfigMap}). In the new spec,
 * those schemas are replaced with placeholder schemas (for example, {@code v1.ConfigMapPlaceholder}).
 * The placeholder schemas are mapped to generic types in {@code openapi-delete-response-mappings.txt}
 * file (for example, {@code v1.ConfigMapPlaceholder} is mapped to {@code DeleteResponse<V1ConfigMap>}).
 * When the open api generator is executed, the placeholders are replaced with mapped values from
 * the mapping file.
 * </p>
 * <br />
 * <p>
 * Usage of {@code DeleteResponse} class solves the issue caused by receiving different jsons
 * ({@code V1Status} vs. resource being deleted) from the kubernetes api server based on the server
 * configuration for each resource.
 * </p>
 * @see <a href="https://github.com/kubernetes/kubernetes/issues/59501#issuecomment-370013330">Delete Resource Issue</a>
 */
@SuppressWarnings("unchecked")
public abstract class ModifySpec extends DefaultTask {

    @InputFile
    public abstract RegularFileProperty getInputSpecFile();

    @OutputFile
    public abstract RegularFileProperty getModifiedSpecFile();

    @OutputFile
    public abstract RegularFileProperty getDeleteResponseMappingsFile();

    @TaskAction
    void modifySpecFile() throws IOException {
        getLogger().info("Modifying openapi spec file");

        Map<String, String> deleteResponseMappings = new LinkedHashMap<>();

        Yaml yaml = TaskUtils.createYaml();

        FileReader fileReader = new FileReader(getInputSpecFile().getAsFile().get());
        Map<String, Object> inputSpecMap = yaml.load(fileReader);

        inputSpecMap.forEach((specKey, specValue) -> {
            if ("paths".equals(specKey)) {
                processPaths((Map<String, Object>) specValue, deleteResponseMappings);
            }
        });

        File specFile = getModifiedSpecFile().getAsFile().get();
        TaskUtils.createSpecFile(yaml, inputSpecMap, specFile);
        getLogger().info("Created kubernetes client modified spec file: {}", specFile.getAbsolutePath());

        File typeMappingsFile = getDeleteResponseMappingsFile().getAsFile().get();
        TaskUtils.createTypeMappingsFile(deleteResponseMappings, typeMappingsFile);
        getLogger().info("Created kubernetes client delete response mappings file: {}", typeMappingsFile.getAbsolutePath());
    }

    private void processPaths(Map<String, Object> paths, Map<String, String> deleteResponseMappings) {
        for (Object pathData : paths.values()) {
            Map<String, Object> operations = (Map<String, Object>) pathData;
            Map<String, Object> deleteOpData = getMapValue(operations, "delete");
            Map<String, Object> getOpData = getMapValue(operations, "get");
            if (deleteOpData != null && getOpData != null) {
                String deleteObjectSchemaRef = getSchemaRef(getOpData);
                if (deleteObjectSchemaRef != null) {
                    String placeholderSchemaRef = deleteObjectSchemaRef + "Placeholder";
                    if (modifyResponseData(deleteOpData, placeholderSchemaRef)) {
                        addMapping(deleteObjectSchemaRef, placeholderSchemaRef, deleteResponseMappings);
                    }
                }
            }
        }
    }

    private String getSchemaRef(Map<String, Object> operationData) {
        boolean nameParamFound = false;
        List<Map<String, Object>> parameters = (List<Map<String, Object>>) operationData.get("parameters");
        if (parameters != null) {
            for (Map<String, Object> parameterMap : parameters) {
                if (parameterMap.get("name").equals("name")) {
                    nameParamFound = true;
                    break;
                }
            }
        }
        if (!nameParamFound) {
            return null;
        }
        Map<String, Object> responses = getMapValue(operationData, "responses");
        Map<String, Object> responseData = getMapValue(responses, "200");
        Map<String, Object> contents = getMapValue(responseData, "content");
        if (contents != null) {
            for (Object contentValue : contents.values()) {
                Map<String, Object> contentValueData = (Map<String, Object>) contentValue;
                Map<String, Object> schemaData = getMapValue(contentValueData, "schema");
                String schemaRef = schemaData == null ? null : (String) schemaData.get("$ref");
                if (schemaRef != null) {
                    return schemaRef;
                }
            }
        }
        return null;
    }

    private boolean modifyResponseData(Map<String, Object> operationData, String placeholderSchemaRef) {
        boolean responseDataModified = false;
        Map<String, Object> responses = getMapValue(operationData, "responses");
        for (Object responseData: responses.values()) {
            Map<String, Object> contents = getMapValue((Map<String, Object>) responseData, "content");
            for (Object contentData: contents.values()) {
                Map<String, Object> schemaData = getMapValue((Map<String, Object>) contentData, "schema");
                String schemaRef = (String) schemaData.get("$ref");
                if (schemaRef != null) {
                    schemaData.put("$ref", placeholderSchemaRef);
                    responseDataModified = true;
                }
            }
        }
        return responseDataModified;
    }

    private void addMapping(String deleteObjectSchemaRef, String placeholderSchemaRef, Map<String, String> deleteResponseMappings) {
        String deleteObjectSchemaType = deleteObjectSchemaRef.substring("#/components/schemas/".length());
        String deleteObjectSimpleName = Arrays.stream(deleteObjectSchemaType.split("\\."))
            .map(word -> Character.toUpperCase(word.charAt(0)) + word.substring(1))
            .collect(Collectors.joining(""));

        String placeholderSchemaType = placeholderSchemaRef.substring("#/components/schemas/".length());
        deleteResponseMappings.put(placeholderSchemaType, "io.micronaut.kubernetes.client.openapi.response.DeleteResponse<" + deleteObjectSimpleName + ">");
    }

    private Map<String, Object> getMapValue(Map<String, Object> map, String key) {
        return map == null ? null : (Map<String, Object>) map.get(key);
    }
}
