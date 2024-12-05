package io.micronaut.kubernetes.client.openapi.tasks;

import org.gradle.api.DefaultTask;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputFile;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.TaskAction;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;
import org.yaml.snakeyaml.representer.Representer;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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

        Yaml yaml = createYaml();

        FileReader fileReader = new FileReader(getInputSpecFile().getAsFile().get());
        Map<String, Object> inputSpecMap = yaml.load(fileReader);
        inputSpecMap.forEach((specKey, specValue) -> {
            if ("paths".equals(specKey)) {
                Map<String, Object> paths = (Map<String, Object>) specValue;
                Map<String, Object> newPaths = new LinkedHashMap<>(paths.size());
                watcherSpecMap.put(specKey, newPaths);
                paths.forEach((pathKey, pathValue) -> {
                    Map<String, Map<String, Object>> operations = (Map<String, Map<String, Object>>) pathValue;
                    Map<String, Object> operationData = operations == null ? null : operations.get("get");
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
                                Map<String, Object> responses = (Map<String, Object>) operationData.get("responses");
                                Map<String, Object> responseData = (Map<String, Object>) responses.get("200");
                                Map<String, Object> contents = (Map<String, Object>) responseData.get("content");
                                contents.values().forEach(contentValue -> {
                                    Map<String, Object> contentValueData = (Map<String, Object>) contentValue;
                                    Map<String, Object> schemaData = (Map<String, Object>) contentValueData.get("schema");
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
                                newPaths.put(pathKey,  Map.of("get", operationData));
                            }
                        }
                    }
                });
            } else if (!"components".equals(specKey)) {
                watcherSpecMap.put(specKey, specValue);
            }
        });

        createWatcherSpecFile(yaml, watcherSpecMap);
        createWatcherTypeMappingsFile(watcherTypeMappings);
    }

    private Yaml createYaml() {
        LoaderOptions loaderOptions = new LoaderOptions();
        loaderOptions.setCodePointLimit(10485760);
        DumperOptions dumperOptions = new DumperOptions();
        dumperOptions.setPrettyFlow(true);
        dumperOptions.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        return new Yaml(new Constructor(loaderOptions), new Representer(dumperOptions));
    }

    private void createWatcherSpecFile(Yaml yaml, Map<String, Object> watcherSpecMap) throws IOException {
        File file = getWatcherSpecFile().getAsFile().get();
        try (FileWriter fileWriter = new FileWriter(file)) {
            yaml.dump(watcherSpecMap, fileWriter);
        }
        getLogger().info("Created kubernetes client watcher openapi spec file: {}", file.getAbsolutePath());
    }

    private void createWatcherTypeMappingsFile(Map<String, String> typeMappings) throws IOException {
        File file = getWatcherTypeMappingsFile().getAsFile().get();
        String typeMappingsStr = typeMappings.entrySet()
            .stream()
            .map(entry -> "\"" + entry.getKey() + "\": \"" + entry.getValue() + "\"")
            .collect(Collectors.joining(", "));
        try (FileWriter fileWriter = new FileWriter(file)) {
            fileWriter.write("[" + typeMappingsStr + "]");
        }
        getLogger().info("Created kubernetes client watcher openapi type mappings file: {}", file.getAbsolutePath());
    }
}
