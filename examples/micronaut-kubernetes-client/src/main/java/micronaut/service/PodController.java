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
package micronaut.service;

import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.openapi.models.V1Pod;
import io.kubernetes.client.openapi.models.V1PodList;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.runtime.context.scope.Refreshable;
import jakarta.inject.Inject;
import jakarta.validation.constraints.NotNull;

import java.util.Map;
import java.util.stream.Collectors;

@Controller("/pods")
@Refreshable
public class PodController {

    @Inject
    CoreV1Api coreV1Api;

    @Get("/{namespace}/{name}")
    public String getPod(final @NotNull String namespace, final @NotNull String name) throws ApiException {
        V1Pod v1Pod = coreV1Api.readNamespacedPod(name, namespace, null);
        return v1Pod.getStatus().getPhase();
    }

    @Get("/{namespace}")
    public Map<String, String> index(final @NotNull String namespace) throws ApiException {
        V1PodList v1PodList = coreV1Api.listNamespacedPod(namespace, null, null, null, null, null, null, null, null, null, false);
        return v1PodList.getItems().stream()
                .filter(p -> p.getStatus() != null)
                .collect(Collectors.toMap(
                        p -> p.getMetadata().getName(),
                        p -> p.getStatus().getPhase()));
    }
}
