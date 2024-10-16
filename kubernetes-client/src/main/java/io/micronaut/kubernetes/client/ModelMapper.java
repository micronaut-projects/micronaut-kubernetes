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
import io.kubernetes.client.common.KubernetesObject;
import io.kubernetes.client.util.Strings;
import org.apache.commons.lang3.tuple.MutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * This class is inspired by the implementation of the <a href="https://github.com/kubernetes-client/java/blob/release-13/util/src/main/java/io/kubernetes/client/util/ModelMapper.java">io.kubernetes.client.util.ModelMapper.java</a>.
 * <p>
 * The difference is that this class doesn't prefetch all model classes but instead does the resolution
 * on demand. Also the integration with {@link io.kubernetes.client.Discovery} is placed in the {@link DiscoveryCache}
 * bean.
 *
 * @author Pavol Gressa
 * @since 3.1
 */
public class ModelMapper {

    private static final Logger LOG = LoggerFactory.getLogger(ModelMapper.class);

    // Model's api-group prefix to kubernetes api-group
    private final Map<String, String> preBuiltApiGroups = new HashMap<>();

    // Model's api-version midfix to kubernetes api-version
    private final List<String> preBuiltApiVersions = new ArrayList<>();

    // This allows parsing custom (not included in kubernetes core) api versions
    // It is based on the kubernetes core one available at https://github.com/kubernetes/apimachinery/blob/master/pkg/util/version/version.go
    // and is completed to be able to proceed to extraction from a java class name
    private final Pattern customVersionParser = Pattern.compile("^\\s*(V(?:[0-9]+(?:\\.[0-9]+)*)(?:[a-z0-9]*)*)[A-Z]+[a-zA-Z0-9]*$");

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
                .map(e -> new MutablePair<>(e.getValue(), name.substring(e.getKey().length())))
                .findFirst()
                .orElse(new MutablePair<>(null, name));
    }

    private Pair<String, String> getApiVersion(String name) {
        return preBuiltApiVersions.stream()
                .filter(name::startsWith)
                .map(v -> new MutablePair<>(v, extractKind(v, name)))
                .findFirst()
                .orElseGet(() -> {
                    String version = tryGuessCustomApiVersion(name);
                    return new MutablePair<>(version, extractKind(version, name));
                });
    }

    private String extractKind(String version, String name){
        return version == null ? name : name.substring(version.length());
    }

    private String tryGuessCustomApiVersion(String name) {
        var patternMatcher = customVersionParser.matcher(name);

        if (patternMatcher.matches() && patternMatcher.groupCount() == 1) {
            return patternMatcher.group(1);
        }

        // Warn the user to avoid wasted debug time (cfr https://github.com/micronaut-projects/micronaut-kubernetes/issues/639)
        LOG.warn("Could not extract ApiVersion from entity {}", name);

        return null;
    }

    /**
     * Resolves version and kind of the the {@code clazz}. To resolve the api group of the clazz use {@link DiscoveryCache}.
     *
     * @param clazz class to resolve
     * @return version kind
     */
    public GroupVersionKind getGroupVersionKindByClass(Class<? extends KubernetesObject> clazz) {
        Pair<String, String> groupAndOther = getApiGroup(clazz.getSimpleName());
        Pair<String, String> versionAndOther = getApiVersion(groupAndOther.getRight());

        String group = Strings.nullToEmpty(groupAndOther.getLeft());
        String version = versionAndOther.getLeft() == null ? null : versionAndOther.getLeft().toLowerCase();
        String kind = versionAndOther.getRight();

        return new GroupVersionKind(group, version, kind);
    }
}
