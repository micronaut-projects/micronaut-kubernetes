package io.micronaut.kubernetes.client.v1

import spock.lang.Specification

class MetadataSpec extends Specification {

    void "test isSecure()"(Map<String, String> metadataLabels, boolean expectedSecure) {
        given:
        Metadata metadata = new Metadata().tap {
            labels = metadataLabels
        }

        when:
        boolean isSecure = metadata.isSecure()

        then:
        isSecure == expectedSecure

        where:
        metadataLabels          | expectedSecure
        Collections.emptyMap()  | false
        ["foo": "bar"]          | false
        ["secure": "false"]     | false
        ["secure": "true"]      | true
    }

}
