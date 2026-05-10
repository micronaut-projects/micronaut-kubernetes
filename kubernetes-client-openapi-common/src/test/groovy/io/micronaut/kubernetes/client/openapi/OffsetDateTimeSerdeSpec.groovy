package io.micronaut.kubernetes.client.openapi

import io.micronaut.json.JsonMapper
import io.micronaut.kubernetes.client.openapi.model.V1ObjectMeta
import io.micronaut.test.extensions.spock.annotation.MicronautTest
import jakarta.inject.Inject
import spock.lang.Specification

import java.time.OffsetDateTime

@MicronautTest
class OffsetDateTimeSerdeSpec extends Specification {

    @Inject
    JsonMapper jsonMapper

    def 'deserialize RFC3339 UTC timestamp'() {
        given:
        String metadataJson = '{"creationTimestamp":"' + timestamp + '"}'

        when:
        V1ObjectMeta metadata = jsonMapper.readValue(metadataJson, V1ObjectMeta)

        then:
        metadata.creationTimestamp == OffsetDateTime.parse(timestamp)

        where:
        timestamp << [
                "2026-05-10T15:02:37Z",
                "2026-05-10T15:02:37.123456Z"
        ]
    }
}
