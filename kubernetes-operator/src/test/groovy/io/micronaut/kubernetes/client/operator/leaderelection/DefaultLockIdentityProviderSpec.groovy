package io.micronaut.kubernetes.client.operator.leaderelection

import io.micronaut.context.env.Environment
import io.micronaut.test.extensions.spock.annotation.MicronautTest
import jakarta.inject.Inject
import spock.lang.Specification

@MicronautTest(environments = [Environment.KUBERNETES])
class DefaultLockIdentityProviderSpec extends Specification {

    @Inject
    LockIdentityProvider provider;

    def "it resolves the provider from env variable"() {
        expect:
        provider.getIdentity() == "test"
    }
}
