/*
 * Copyright 2017-2021 original authors
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
package io.micronaut.kubernetes.client;

import io.kubernetes.client.apimachinery.GroupVersionKind;
import io.kubernetes.client.util.Strings;
import org.apache.commons.lang3.tuple.MutablePair;
import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This class is inspired by the implementation of the <a href="https://github.com/kubernetes-client/java/blob/release-13/util/src/main/java/io/kubernetes/client/util/ModelMapper.java">io.kubernetes.client.util.ModelMapper.java</a>.
 * <p>
 * The difference is that this class doesn't prefetch all model classes but instead does the resolution
 * on demand.
 *
 * @author Pavol Gressa
 * @since 3.1
 */
public class ModelMapper {

    // Model's api-group prefix to kubernetes api-group
    private final Map<String, String> preBuiltApiGroups = new HashMap<>();

    // Model's api-version midfix to kubernetes api-version
    private final List<String> preBuiltApiVersions = new ArrayList<>();

    public ModelMapper() {
        initApiGroupMap();
        initApiVersionList();
    }

    private void initApiGroupMap() {
        preBuiltApiGroups.put("Admissionregistration", "admissionregistration.k8s.io");
        preBuiltApiGroups.put("Apiextensions", "apiextensions.k8s.io");
        preBuiltApiGroups.put("Apiregistration", "apiregistration.k8s.io");
        preBuiltApiGroups.put("Apps", "apps");
        preBuiltApiGroups.put("Authentication", "authentication.k8s.io");
        preBuiltApiGroups.put("Authorization", "authorization.k8s.io");
        preBuiltApiGroups.put("Autoscaling", "autoscaling");
        preBuiltApiGroups.put("Batch", "batch");
        preBuiltApiGroups.put("Certificates", "certificates.k8s.io");
        preBuiltApiGroups.put("Core", "");
        preBuiltApiGroups.put("Extensions", "extensions");
        preBuiltApiGroups.put("Events", "events.k8s.io");
        preBuiltApiGroups.put("FlowControl", "flowcontrol.apiserver.k8s.io");
        preBuiltApiGroups.put("Networking", "networking.k8s.io");
        preBuiltApiGroups.put("Policy", "policy");
        preBuiltApiGroups.put("RbacAuthorization", "rbac.authorization.k8s.io");
        preBuiltApiGroups.put("Scheduling", "scheduling.k8s.io");
        preBuiltApiGroups.put("Settings", "settings.k8s.io");
        preBuiltApiGroups.put("Storage", "storage.k8s.io");
    }

    private void initApiVersionList() {
        // Order important
        preBuiltApiVersions.add("V2beta1");
        preBuiltApiVersions.add("V2beta2");
        preBuiltApiVersions.add("V2alpha1");
        preBuiltApiVersions.add("V1beta2");
        preBuiltApiVersions.add("V1beta1");
        preBuiltApiVersions.add("V1alpha1");
        preBuiltApiVersions.add("V1");
    }

    private Pair<String, String> getApiGroup(String name) {
        return preBuiltApiGroups.entrySet().stream()
                .filter(e -> name.startsWith(e.getKey()))
                .map(e -> new MutablePair<String, String>(e.getValue(), name.substring(e.getKey().length())))
                .findFirst()
                .orElse(new MutablePair<String, String>(null, name));
    }

    private Pair<String, String> getApiVersion(String name) {
        return preBuiltApiVersions.stream()
                .filter(name::startsWith)
                .map(v -> new MutablePair<String, String>(v.toLowerCase(), name.substring(v.length())))
                .findFirst()
                .orElse(new MutablePair<String, String>(null, name));
    }

    /**
     * @param clazz class to resolve
     * @return group version kind
     */
    public GroupVersionKind getGroupVersionKindByClass(Class<?> clazz) {
        Pair<String, String> groupAndOther = getApiGroup(clazz.getSimpleName());
        Pair<String, String> versionAndOther = getApiVersion(groupAndOther.getRight());

        String group = Strings.nullToEmpty(groupAndOther.getLeft());
        String version = versionAndOther.getLeft();
        String kind = versionAndOther.getRight();
        return new GroupVersionKind(group, version, kind);
    }
}
