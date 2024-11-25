/*
 * Copyright 2017-2024 original authors
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
import org.gradle.api.tasks.Optional;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.TaskAction;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.URL;

/**
 * Downloads kubernetes client openapi spec.
 */
public abstract class DownloadSpec extends DefaultTask {

    private static final String SPEC_URL_TEMPLATE = "https://raw.githubusercontent.com/kubernetes-client/java/refs/tags/v%s/kubernetes/api/openapi.yaml";

    @Input
    @Optional
    public abstract Property<String> getSpecUrl();

    @Input
    @Optional
    public abstract Property<String> getSpecVersion();

    @OutputFile
    public abstract RegularFileProperty getSpecFile();

    @TaskAction
    void downloadSpecFile() throws IOException {
        URL specUrl;
        if (getSpecUrl().isPresent()) {
            specUrl = new URL(getSpecUrl().get());
        } else if (getSpecVersion().isPresent()) {
            specUrl = new URL(SPEC_URL_TEMPLATE.formatted(getSpecVersion().get()));
        } else {
            throw new IllegalArgumentException("specUrl or specVersion must be provided in order to download kubernetes client spec file");
        }
        File specFile = getSpecFile().getAsFile().get();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(specUrl.openStream()));
             BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(specFile)))) {
            getLogger().info("Downloading kubernetes client openapi spec file: {}", specUrl);
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.contains("x-implements")) {
                    // skip lines which contains x-implements because we don't want that generated classes implement
                    // io.kubernetes.client.common.KubernetesObject and io.kubernetes.client.common.KubernetesListObject
                    reader.readLine(); // skip one more line because it contains KubernetesObject interface
                } else {
                    writer.write(line);
                    writer.newLine();
                }
            }
            getLogger().info("Downloaded kubernetes client openapi spec file: {}", specFile.getAbsolutePath());
        }
    }
}
