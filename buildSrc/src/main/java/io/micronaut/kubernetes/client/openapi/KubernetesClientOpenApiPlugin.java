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
package io.micronaut.kubernetes.client.openapi;

import io.micronaut.gradle.MicronautExtension;
import io.micronaut.gradle.PluginsHelper;
import io.micronaut.kubernetes.client.openapi.tasks.DownloadSpec;
import org.gradle.api.Plugin;
import org.gradle.api.Project;

/**
 * Plugin which provides tasks for kubernetes client openapi spec.
 */
public class KubernetesClientOpenApiPlugin implements Plugin<Project> {

    private static final String DEFAULT_OPENAPI_SPEC_FILE_NAME = "openapi.yaml";

    @Override
    public void apply(Project project) {
        MicronautExtension micronautExtension = PluginsHelper.findMicronautExtension(project);
        KubernetesClientOpenApiExtension kubernetesClientOpenApiExtension = micronautExtension.getExtensions()
            .create("kubernetesClientOpenApi", KubernetesClientOpenApiExtension.class);
        project.getTasks().register("downloadKubernetesClientOpenApiSpec", DownloadSpec.class, task -> {
            task.setGroup("micronaut openapi");
            task.setDescription("Downloads kubernetes client openapi spec");
            task.getSpecUrl().set(kubernetesClientOpenApiExtension.getSpecUrl());
            task.getSpecVersion().set(kubernetesClientOpenApiExtension.getSpecVersion());
            task.getSpecFile().set(project.getLayout().getBuildDirectory().file(kubernetesClientOpenApiExtension.getFileName().convention(DEFAULT_OPENAPI_SPEC_FILE_NAME)));
        });
    }
}
