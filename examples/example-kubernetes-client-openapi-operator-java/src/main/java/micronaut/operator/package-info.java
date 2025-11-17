/**
 * Beans from this package should be loaded only when the service is being executed inside kubernetes.
 */
@Configuration
@Requires(env = Environment.KUBERNETES)
package micronaut.operator;

import io.micronaut.context.annotation.Configuration;
import io.micronaut.context.annotation.Requires;
import io.micronaut.context.env.Environment;
