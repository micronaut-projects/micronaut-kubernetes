package io.micronaut.kubernetes.client.openapi.watcher

import io.micronaut.core.type.Argument
import io.micronaut.json.JsonMapper
import io.micronaut.kubernetes.client.openapi.model.V1Namespace
import io.micronaut.test.extensions.spock.annotation.MicronautTest
import jakarta.inject.Inject
import spock.lang.Specification

@MicronautTest
class WatchEventDeserializerSpec extends Specification {

    @Inject
    JsonMapper jsonMapper

    void "test deserialization of added event"() {
        given:
        def addedEvent = '{"type":"ADDED","object":{"kind":"Namespace","apiVersion":"v1","spec":{"finalizers":["kubernetes"]},"status":{"phase":"Active"}}}'

        when:
        def watchEvent = jsonMapper.readValue(addedEvent, Argument.of(WatchEvent.class, Argument.of(V1Namespace.class)))

        then:
        watchEvent != null
        watchEvent.type() == 'ADDED'
        watchEvent.status() == null
        watchEvent.object().kind == 'Namespace'
        watchEvent.object().apiVersion == 'v1'
        watchEvent.object().spec.finalizers.get(0) == 'kubernetes'
        watchEvent.object().status.phase == 'Active'
    }

    void "test deserialization of error event"() {
        given:
        def errorEvent = '{"type":"ERROR","object":{"kind":"Status","apiVersion":"v1","status":"Failure","message":"too old resource version: 1200 (184080)","reason":"Expired"}}'

        when:
        def watchEvent = jsonMapper.readValue(errorEvent, Argument.of(WatchEvent.class, Argument.of(V1Namespace.class)))

        then:
        watchEvent != null
        watchEvent.type() == 'ERROR'
        watchEvent.object() == null
        watchEvent.status().kind == 'Status'
        watchEvent.status().apiVersion == 'v1'
        watchEvent.status().status == 'Failure'
        watchEvent.status().message == 'too old resource version: 1200 (184080)'
        watchEvent.status().reason == 'Expired'
    }
}
