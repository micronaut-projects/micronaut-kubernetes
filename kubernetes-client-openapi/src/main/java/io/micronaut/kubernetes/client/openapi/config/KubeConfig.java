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
package io.micronaut.kubernetes.client.openapi.config;

import io.micronaut.core.util.CollectionUtils;
import io.micronaut.core.util.StringUtils;
import io.micronaut.kubernetes.client.openapi.config.model.AuthInfo;
import io.micronaut.kubernetes.client.openapi.config.model.Cluster;
import io.micronaut.kubernetes.client.openapi.config.model.Context;
import io.micronaut.kubernetes.client.openapi.config.model.ExecConfig;
import io.micronaut.kubernetes.client.openapi.config.model.ExecEnvVar;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Holder for data loaded from the kube config file.
 */
public class KubeConfig {
    static final String REQUIRED_FIELD_ERROR_MSG = "'%s' not found in the kube config file";

    private final Path kubeConfigParentPath;
    private final String currentContextName;
    private final Map<String, Context> contexts = new HashMap<>();
    private final Map<String, Cluster> clusters = new HashMap<>();
    private final Map<String, AuthInfo> users = new HashMap<>();

    public KubeConfig(String kubeConfigPath, Map<String, Object> configMap) {
        kubeConfigParentPath = kubeConfigPath.startsWith("file:")
            ? Path.of(kubeConfigPath.substring(5)).getParent()
            : Path.of(kubeConfigPath).getParent();

        String currentContext = (String) configMap.get("current-context");
        validateRequiredField(currentContext, "current-context", null);
        currentContextName = currentContext;

        List<Object> contextList = (List<Object>) configMap.get("contexts");
        validateRequiredField(contextList, "contexts", null);
        contextList.forEach(obj -> {
            Map<String, Object> map = (Map<String, Object>) obj;
            contexts.put(getName(map, "contexts"), getContext(map));
        });

        List<Object> clusterList = (List<Object>) configMap.get("clusters");
        validateRequiredField(clusterList, "clusters", null);
        clusterList.forEach(obj -> {
            Map<String, Object> map = (Map<String, Object>) obj;
            clusters.put(getName(map, "clusters"), getCluster(map));
        });

        List<Object> userList = (List<Object>) configMap.get("users");
        validateRequiredField(userList, "users", null);
        userList.forEach(obj -> {
            Map<String, Object> map = (Map<String, Object>) obj;
            users.put(getName(map, "users"), getUser(map));
        });
    }

    /**
     * Gets a cluster from the current context.
     *
     * @return {@link Cluster} instance
     */
    public Cluster getCluster() {
        Context currentContext = contexts.get(currentContextName);
        return clusters.get(currentContext.cluster());
    }

    /**
     * Gets a user from the current context.
     *
     * @return {@link AuthInfo} instance
     */
    public AuthInfo getUser() {
        Context currentContext = contexts.get(currentContextName);
        return users.get(currentContext.user());
    }

    /**
     * Gets a path of the parent folder of the kube config file.
     *
     * @return {@link Path} instance
     */
    public Path getKubeConfigParentPath() {
        return kubeConfigParentPath;
    }

    /**
     * Checks whether the exec command is provided for getting authentication token.
     *
     * @return {@code true} if the exec command is provided in the kube config
     */
    public boolean isExecCommandProvided() {
        return getUser().exec() != null;
    }

    private String getName(Map<String, Object> map, String parentFieldName) {
        String name = (String) map.get("name");
        validateRequiredField(name, "name", parentFieldName);
        return name;
    }

    private Context getContext(Map<String, Object> map) {
        Map<String, Object> contextMap = (Map<String, Object>) map.get("context");
        validateRequiredField(contextMap, "context", "contexts");
        String cluster = (String) contextMap.get("cluster");
        validateRequiredField(cluster, "cluster", "contexts.context");
        String user = (String) contextMap.get("user");
        validateRequiredField(user, "user", "contexts.context");
        String namespace = (String) contextMap.get("namespace");
        return new Context(cluster, user, namespace);
    }

    private Cluster getCluster(Map<String, Object> map) {
        Map<String, Object> clusterMap = (Map<String, Object>) map.get("cluster");
        validateRequiredField(clusterMap, "cluster", "clusters");
        String server = (String) clusterMap.get("server");
        validateRequiredField(server, "server", "clusters.cluster");
        byte[] certificateAuthorityData = getDataBytes(
            (String) clusterMap.get("certificate-authority-data"),
            (String) clusterMap.get("certificate-authority"));
        Boolean insecureSkipTlsVerify = (Boolean) clusterMap.get("insecure-skip-tls-verify");
        return new Cluster(server, certificateAuthorityData, insecureSkipTlsVerify);
    }

    private AuthInfo getUser(Map<String, Object> map) {
        Map<String, Object> userMap = (Map<String, Object>) map.get("user");
        validateRequiredField(userMap, "user", "users");
        byte[] clientCertificateData = getDataBytes(
            (String) userMap.get("client-certificate-data"),
            (String) userMap.get("client-certificate"));
        byte[] clientKeyData = getDataBytes(
            (String) userMap.get("client-key-data"),
            (String) userMap.get("client-key"));
        String token = getToken(
            (String) userMap.get("token"),
            (String) userMap.get("tokenFile"));
        String username = (String) userMap.get("username");
        String password = (String) userMap.get("password");
        ExecConfig exec = getExecConfig(userMap);
        return new AuthInfo(clientCertificateData, clientKeyData, token, username, password, exec);
    }

    private ExecConfig getExecConfig(Map<String, Object> map) {
        Map<String, Object> execMap = (Map<String, Object>) map.get("exec");
        if (CollectionUtils.isEmpty(execMap)) {
            return null;
        }
        String apiVersion = (String) execMap.get("apiVersion");
        validateRequiredField(execMap, "apiVersion", "users.user.exec");
        if (!"client.authentication.k8s.io/v1beta1".equals(apiVersion)
            && !"client.authentication.k8s.io/v1alpha1".equals(apiVersion)) {
            throw new IllegalArgumentException("Unrecognized users.user.exec.apiVersion: " + apiVersion);
        }
        String command = (String) execMap.get("command");
        validateRequiredField(command, "command", "users.user.exec");
        List<String> args = (List<String>) execMap.get("args");
        List<ExecEnvVar> env = getExecEnvVars(execMap);
        return new ExecConfig(apiVersion, command, args, env);
    }

    private List<ExecEnvVar> getExecEnvVars(Map<String, Object> map) {
        List<Map<String, Object>> envVars = (List<Map<String, Object>>) map.get("env");
        if (CollectionUtils.isEmpty(envVars)) {
            return null;
        }
        List<ExecEnvVar> envVarResult = new ArrayList<>(envVars.size());
        envVars.forEach(envVarMap -> {
            String name = (String) envVarMap.get("name");
            validateRequiredField(name, "name", "users.user.exec.env");
            String value = (String) envVarMap.get("value");
            validateRequiredField(name, "value", "users.user.exec.env");
            envVarResult.add(new ExecEnvVar(name, value));
        });
        return envVarResult;
    }

    private void validateRequiredField(Object field, String fieldName, String parentFieldName) {
        if (field == null
            || field instanceof String fieldString && fieldString.isBlank()
            || field instanceof Collection fieldCollection && fieldCollection.isEmpty()
            || field instanceof Map fieldMap && fieldMap.isEmpty()
        ) {
            String errorField = StringUtils.isEmpty(parentFieldName) ? fieldName : parentFieldName + "." + fieldName;
            throw new IllegalArgumentException(REQUIRED_FIELD_ERROR_MSG.formatted(errorField));
        }
    }

    private byte[] getDataBytes(String configData, String dataRelativePath) {
        if (StringUtils.isNotEmpty(configData)) {
            return Base64.getDecoder().decode(configData);
        } else if (StringUtils.isNotEmpty(dataRelativePath)) {
            Path dataAbsolutePath = kubeConfigParentPath.resolve(dataRelativePath).normalize();
            try {
                return Files.readAllBytes(dataAbsolutePath);
            } catch (IOException e) {
                throw new RuntimeException("Failed to read file: " + dataAbsolutePath, e);
            }
        }
        return null;
    }

    private String getToken(String token, String tokenFile) {
        if (StringUtils.isNotEmpty(token)) {
            return token;
        } else if (StringUtils.isNotEmpty(tokenFile)) {
            try {
                byte[] data = Files.readAllBytes(FileSystems.getDefault().getPath(tokenFile));
                return new String(data, StandardCharsets.UTF_8);
            } catch (IOException e) {
                throw new RuntimeException("Failed to read token file: " + tokenFile, e);
            }
        }
        return null;
    }
}
