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

import io.micronaut.kubernetes.client.openapi.tasks.CreateWatcherSpec;
import io.micronaut.kubernetes.client.openapi.tasks.DownloadSpec;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.tasks.TaskProvider;

/**
 * Plugin which provides tasks for kubernetes client openapi spec.
 */
public class KubernetesClientOpenApiPlugin implements Plugin<Project> {

    private static final String OPENAPI_SPEC_FILE_NAME = "openapi.yaml";
    private static final String OPENAPI_WATCHER_SPEC_FILE_NAME = "openapi-watcher.yaml";
    private static final String OPENAPI_WATCHER_TYPE_MAPPINGS_FILE_NAME = "openapi-watcher-type-mappings.txt";

    @Override
    public void apply(Project project) {
        KubernetesClientOpenApiExtension kubernetesClientOpenApiExtension = project.getExtensions()
            .create("kubernetesClientOpenApi", KubernetesClientOpenApiExtension.class);
        TaskProvider<DownloadSpec> downloadSpecTask = project.getTasks().register("downloadOpenApiSpec", DownloadSpec.class, task -> {
            task.setGroup("kubernetes client openapi");
            task.setDescription("Downloads kubernetes client openapi spec");
            task.getSpecUrl().set(kubernetesClientOpenApiExtension.getSpecUrl());
            task.getSpecVersion().set(kubernetesClientOpenApiExtension.getSpecVersion());
            task.getSpecFile().convention(project.getLayout().getBuildDirectory().file(OPENAPI_SPEC_FILE_NAME));
        });
        project.getTasks().register("createWatcherOpenApiSpec", CreateWatcherSpec.class, task -> {
            task.setGroup("kubernetes client openapi");
            task.setDescription("Creates watcher spec from the downloaded client openapi spec");
            task.getInputSpecFile().convention(downloadSpecTask.flatMap(DownloadSpec::getSpecFile));
            task.getModelPackageName().set(kubernetesClientOpenApiExtension.getModelPackageName());
            task.getWatcherSpecFile().convention(project.getLayout().getBuildDirectory().file(OPENAPI_WATCHER_SPEC_FILE_NAME));
            task.getWatcherTypeMappingsFile().convention(project.getLayout().getBuildDirectory().file(OPENAPI_WATCHER_TYPE_MAPPINGS_FILE_NAME));
        });
    }
}
