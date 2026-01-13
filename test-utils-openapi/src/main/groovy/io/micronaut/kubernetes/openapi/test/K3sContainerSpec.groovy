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
package io.micronaut.kubernetes.openapi.test

import io.micronaut.context.ApplicationContext
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.testcontainers.containers.output.Slf4jLogConsumer
import org.testcontainers.k3s.K3sContainer
import org.testcontainers.utility.DockerImageName
import spock.lang.AutoCleanup
import spock.lang.Shared
import spock.lang.Specification

import java.nio.file.Files
import java.nio.file.Path

abstract class K3sContainerSpec extends Specification {

    private static final Logger LOG_K3S = LoggerFactory.getLogger("K3S." + K3sContainerSpec.getSimpleName())

    @Shared
    @AutoCleanup
    K3sContainer k3s = new K3sContainer(DockerImageName.parse("rancher/k3s:v1.35.0-k3s1"))
            .withLogConsumer(new Slf4jLogConsumer(LOG_K3S))

    @Shared
    Path kubeConfigDir = Files.createTempDirectory("kube-temp-")

    @Shared
    Path kubeConfigFile = kubeConfigDir.resolve("config")

    def setupSpec() {
        k3s.start()
        kubeConfigFile.toFile().text = k3s.getKubeConfigYaml()
        getLogger().info("Kubernetes config file path: {}", kubeConfigFile)
        setupKubernetes()
    }

    def cleanupSpec() {
        if (kubeConfigFile != null) {
            Files.deleteIfExists(kubeConfigFile)
        }
        if (kubeConfigDir) {
            Files.deleteIfExists(kubeConfigDir)
        }
    }

    def setupKubernetes() {
        try (ApplicationContext context = ApplicationContext.run([
                "spec.name"                                              : "KubernetesInitContext",
                "kubernetes.client.kube-config-path"                     : "file:" + kubeConfigFile.toString(),
                "kubernetes.client.operator.enabled"                     : false,
                "kubernetes.client.operator.leader-election.lock.enabled": false,
                "micronaut.config-client.enabled"                        : false
        ])) {
            setupKubernetes(context)
        }
    }

    def setupKubernetes(ApplicationContext context) {}

    abstract Logger getLogger();
}
