/*
 * Copyright 2017-2019 original authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.micronaut.kubernetes.client.v1;

import io.micronaut.context.annotation.Requires;
import io.micronaut.context.env.Environment;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.MutableHttpRequest;
import io.micronaut.http.annotation.Filter;
import io.micronaut.http.filter.ClientFilterChain;
import io.micronaut.http.filter.FilterChain;
import io.micronaut.http.filter.HttpClientFilter;
import io.micronaut.http.filter.HttpFilter;
import org.reactivestreams.Publisher;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

@Filter(value = "/api/v1/**", serviceId = KubernetesClient.SERVICE_ID)
@Requires(env = Environment.KUBERNETES)
public class KubernetesClientFilter implements HttpClientFilter {

    public static final String TOKEN_PATH = "/var/run/secrets/kubernetes.io/serviceaccount/token";

    private final String token;

    public KubernetesClientFilter() throws IOException {
        this.token = new String(Files.readAllBytes(Paths.get(TOKEN_PATH)));
    }

    /**
     * A variation of {@link HttpFilter#doFilter(HttpRequest, FilterChain)} that receives a {@link MutableHttpRequest}
     * allowing the request to be modified.
     *
     * @param request The request
     * @param chain   The filter chain
     * @return The publisher of the response
     * @see HttpFilter
     */
    @Override
    public Publisher<? extends HttpResponse<?>> doFilter(MutableHttpRequest<?> request, ClientFilterChain chain) {
        return chain.proceed(request.bearerAuth(token));
    }
}
