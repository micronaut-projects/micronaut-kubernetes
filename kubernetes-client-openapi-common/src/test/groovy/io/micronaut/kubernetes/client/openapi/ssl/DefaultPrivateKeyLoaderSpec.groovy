package io.micronaut.kubernetes.client.openapi.ssl

import io.micronaut.core.io.ResourceResolver
import spock.lang.Specification

import java.security.PrivateKey

class DefaultPrivateKeyLoaderSpec extends Specification {

    def 'load key pkcs8 rsa'() {
        when:
        PrivateKey privateKey = loadPrivateKey("pkcs8-rsa.key")

        then:
        privateKey.getAlgorithm() == "RSA"
    }

    def 'load key pkcs8 ec'() {
        when:
        PrivateKey privateKey = loadPrivateKey("pkcs8-ec.key")

        then:
        privateKey.getAlgorithm() == "EC"
    }

    def 'load key pkcs1'() {
        when:
        PrivateKey privateKey = loadPrivateKey("pkcs1.key")

        then:
        privateKey.getAlgorithm() == "RSA"
    }

    def 'load key sec1'() {
        when:
        PrivateKey privateKey = loadPrivateKey("sec1.key")

        then:
        privateKey.getAlgorithm() == "EC"
    }

    private PrivateKey loadPrivateKey(String filePath) {
        DefaultPrivateKeyLoader keyLoader = new DefaultPrivateKeyLoader()
        ResourceResolver resourceResolver = new ResourceResolver()
        Optional<InputStream> inputStream = resourceResolver.getResourceAsStream("classpath:key/" + filePath)
        byte[] resourceBytes = inputStream.get().readAllBytes()
        return keyLoader.loadPrivateKey(resourceBytes)
    }
}
