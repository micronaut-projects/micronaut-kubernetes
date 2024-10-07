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
package io.micronaut.kubernetes.client.openapi.credential;

import io.micronaut.kubernetes.client.openapi.config.KubernetesClientConfiguration;
import io.micronaut.kubernetes.client.openapi.config.model.ExecConfig;
import io.micronaut.kubernetes.client.openapi.config.model.ExecEnvVar;
import io.micronaut.kubernetes.client.openapi.config.KubeConfig;
import io.micronaut.kubernetes.client.openapi.credential.model.ExecCredential;
import io.micronaut.serde.ObjectMapper;
import jakarta.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.InputStream;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * The credential loader which uses the exec command from the kube config to get credentials.
 */
@Singleton
class ExecCommandCredentialLoader implements KubernetesCredentialLoader {
    private static final Logger LOG = LoggerFactory.getLogger(ExecCommandCredentialLoader.class);

    private static final long BUFFER_IN_MINUTES = 1L;

    private final KubeConfig kubeConfig;
    private final ObjectMapper objectMapper;

    private ExecCredential execCredential;

    ExecCommandCredentialLoader(KubernetesClientConfiguration kubernetesClientConfiguration,
                                ObjectMapper objectMapper) {
        kubeConfig = kubernetesClientConfiguration.getKubeConfig();
        this.objectMapper = objectMapper;
        setExecCredential();
    }

    @Override
    public String getToken() {
        setExecCredential();
        return execCredential == null ? null : execCredential.status().token();
    }

    private void setExecCredential() {
        if (!kubeConfig.isExecCommandProvided()) {
            return;
        }
        if (shouldLoadCredential()) {
            synchronized (this) {
                if (shouldLoadCredential()) {
                    try {
                        execCredential = loadCredential();
                    } catch (Exception e) {
                        throw new RuntimeException("Failed to load exec credential", e);
                    }
                }
            }
        }
    }

    private boolean shouldLoadCredential() {
        if (execCredential == null) {
            return true;
        }
        ZonedDateTime expiration = execCredential.status().expirationTimestamp();
        if (expiration == null) {
            return false;
        }
        ZonedDateTime now = ZonedDateTime.now(ZoneId.of("UTC"));
        LOG.debug("Check whether credential loading needed, now={}, buffer={}, expiration={}", now, BUFFER_IN_MINUTES, expiration);
        return expiration.isBefore(now.plusMinutes(BUFFER_IN_MINUTES));
    }

    private ExecCredential loadCredential() throws Exception {
        LOG.debug("Loading credential using exec command from kube config file");
        List<String> processArgs = new ArrayList<>();

        ExecConfig exec = kubeConfig.getUser().exec();
        String command = exec.command();
        if (command.contains(File.separator) && !command.startsWith(File.separator)) {
            // path relative to the directory of the kube config file
            command = kubeConfig.getKubeConfigParentPath().resolve(command).normalize().toString();
        }
        processArgs.add(command);

        List<String> args = exec.args();
        if (args != null) {
            processArgs.addAll(args);
        }

        ProcessBuilder processBuilder = new ProcessBuilder(processArgs);
        processBuilder.redirectError(ProcessBuilder.Redirect.INHERIT);

        List<ExecEnvVar> env = exec.env();
        if (env != null) {
            Map<String, String> environment = processBuilder.environment();
            env.forEach(execEnvVar -> environment.put(execEnvVar.name(), execEnvVar.value()));
        }

        Process process = processBuilder.start();
        ExecCredential execCredentialResult;
        try (InputStream inputStream = process.getInputStream()) {
            execCredentialResult = objectMapper.readValue(inputStream, ExecCredential.class);
        }
        if (execCredentialResult.status() == null || execCredentialResult.status().token() == null) {
            throw new RuntimeException("Command '" + command + "' didn't provide token");
        }
        int exitCode = process.waitFor();
        if (exitCode != 0) {
            throw new RuntimeException("Command '" + command + "' failed with exit code " + exitCode);
        }
        return execCredentialResult;
    }
}
