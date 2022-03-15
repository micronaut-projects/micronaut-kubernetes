package micronaut.client;

import io.micronaut.management.endpoint.env.EnvironmentEndpointFilter;
import io.micronaut.management.endpoint.env.EnvironmentFilterSpecification;
import jakarta.inject.Singleton;

import javax.validation.constraints.NotNull;

@Singleton
public class LegacyEnvEndpointFilter implements EnvironmentEndpointFilter {
    @Override
    public void specifyFiltering(@NotNull EnvironmentFilterSpecification specification) {
        specification.legacyMasking();
    }
}