package micronaut.operator;

import io.micronaut.management.endpoint.env.EnvironmentEndpointFilter;
import io.micronaut.management.endpoint.env.EnvironmentFilterSpecification;
import jakarta.inject.Singleton;
import jakarta.validation.constraints.NotNull;

@Singleton
public class LegacyEnvEndpointFilter implements EnvironmentEndpointFilter {
    @Override
    public void specifyFiltering(@NotNull EnvironmentFilterSpecification specification) {
        specification.legacyMasking();
    }
}