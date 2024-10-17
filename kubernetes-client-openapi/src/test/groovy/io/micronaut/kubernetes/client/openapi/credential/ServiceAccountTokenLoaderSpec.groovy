package io.micronaut.kubernetes.client.openapi.credential

import io.micronaut.context.ApplicationContext
import spock.lang.Shared
import spock.lang.Specification
import spock.lang.Stepwise

import java.nio.file.Files
import java.nio.file.Path

@Stepwise
class ServiceAccountTokenLoaderSpec extends Specification {

    @Shared
    Path tokenDir = Files.createTempDirectory("token-temp-")

    @Shared
    Path tokenFile = tokenDir.resolve("config")

    @Shared
    ApplicationContext clientContext = ApplicationContext.run([
            'kubernetes.client.service-account.token-path': "file:" + tokenFile,
            'kubernetes.client.service-account.token-reload-interval': '1s'
    ])

    @Shared
    def tokenLoader = clientContext.getBean(ServiceAccountTokenLoader.class)

    def cleanupSpec() {
        if (tokenFile != null) {
            Files.deleteIfExists(tokenFile)
        }
        if (tokenDir) {
            Files.deleteIfExists(tokenDir)
        }
    }

    def 'get token first time'() {
        given:
        tokenFile.toFile().text = 'old-test-token'

        when:
        def token = tokenLoader.getToken()

        then:
        token == 'old-test-token'
    }

    def 'get token from cache'() {
        given:
        tokenFile.toFile().text = 'new-test-token'

        when:
        def token = tokenLoader.getToken()

        then:
        token == 'old-test-token'
    }

    def 'get reloaded token'() {
        given:
        sleep(1000)

        when:
        def token = tokenLoader.getToken()

        then:
        token == 'new-test-token'
    }
}
