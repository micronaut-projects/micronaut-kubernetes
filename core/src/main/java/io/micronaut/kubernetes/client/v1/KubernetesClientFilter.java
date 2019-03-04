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

    private final String token;

    public KubernetesClientFilter() throws IOException {
        this.token = new String(Files.readAllBytes(Paths.get("/var/run/secrets/kubernetes.io/serviceaccount/token")));
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
