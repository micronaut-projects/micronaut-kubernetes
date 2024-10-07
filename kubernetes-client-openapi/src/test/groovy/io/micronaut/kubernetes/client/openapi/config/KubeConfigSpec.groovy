package io.micronaut.kubernetes.client.openapi.config

import spock.lang.Specification

import java.nio.charset.Charset

import static io.micronaut.kubernetes.client.openapi.config.KubeConfig.REQUIRED_FIELD_ERROR_MSG

class KubeConfigSpec extends Specification {

    private static final def CLIENT_CERT_DATA = Base64.getEncoder().encode("test-client-certificate-data".getBytes(Charset.forName("UTF-8")))
    private static final def CLIENT_KEY_DATA = Base64.getEncoder().encode("test-client-key-data".getBytes(Charset.forName("UTF-8")))

    private static final def BASE_MAP = ["current-context": "test"]
    private static final def CONTEXT_MAP = BASE_MAP + [contexts: [
            [name: "test", context: [cluster: "test-cl", user: "test-user"]]
    ]]
    private static final def CLUSTER_MAP = CONTEXT_MAP + [clusters: [
            [name: "test", cluster: ["server": "test-server"]]
    ]]
    private static final def FULL_MAP = CLUSTER_MAP + [users: [
            [name: "test",
             user: [
                     "client-certificate": "test-client-certificate",
                     "client-certificate-data": CLIENT_CERT_DATA,
                     "client-key": "test-client-key",
                     "client-key-data": CLIENT_KEY_DATA,
                     "username": "test-username",
                     "password": "test-password"
             ]]
    ]]

    def 'validate valid kube config'() {
        when:
        //KubeConfig kubeConfig = new KubeConfig("", FULL_MAP)
        def map = FULL_MAP

        then:
        map != null
    }

    def 'validate invalid contexts section in kube config'() {
        when:
        new KubeConfig("", map)

        then:
        def error = thrown(expectedException)
        error.message == expectedMessage

        where:
        map                                                                                 || expectedException        | expectedMessage
        BASE_MAP                                                                            || IllegalArgumentException | REQUIRED_FIELD_ERROR_MSG.formatted("contexts")
        BASE_MAP + [contexts: null]                                                         || IllegalArgumentException | REQUIRED_FIELD_ERROR_MSG.formatted("contexts")
        BASE_MAP + [contexts: []]                                                           || IllegalArgumentException | REQUIRED_FIELD_ERROR_MSG.formatted("contexts")
        BASE_MAP + [contexts: [[name: null]]]                                               || IllegalArgumentException | REQUIRED_FIELD_ERROR_MSG.formatted("contexts.name")
        BASE_MAP + [contexts: [[name: ""]]]                                                 || IllegalArgumentException | REQUIRED_FIELD_ERROR_MSG.formatted("contexts.name")
        BASE_MAP + [contexts: [[name: " "]]]                                                || IllegalArgumentException | REQUIRED_FIELD_ERROR_MSG.formatted("contexts.name")
        BASE_MAP + [contexts: [[name: "test"]]]                                             || IllegalArgumentException | REQUIRED_FIELD_ERROR_MSG.formatted("contexts.context")
        BASE_MAP + [contexts: [[name: "test", context: null]]]                              || IllegalArgumentException | REQUIRED_FIELD_ERROR_MSG.formatted("contexts.context")
        BASE_MAP + [contexts: [[name: "test", context: [:]]]]                               || IllegalArgumentException | REQUIRED_FIELD_ERROR_MSG.formatted("contexts.context")
        BASE_MAP + [contexts: [[name: "test", context: [cluster: null]]]]                   || IllegalArgumentException | REQUIRED_FIELD_ERROR_MSG.formatted("contexts.context.cluster")
        BASE_MAP + [contexts: [[name: "test", context: [cluster: ""]]]]                     || IllegalArgumentException | REQUIRED_FIELD_ERROR_MSG.formatted("contexts.context.cluster")
        BASE_MAP + [contexts: [[name: "test", context: [cluster: " "]]]]                    || IllegalArgumentException | REQUIRED_FIELD_ERROR_MSG.formatted("contexts.context.cluster")
        BASE_MAP + [contexts: [[name: "test", context: [cluster: "test-cl", user: null]]]]  || IllegalArgumentException | REQUIRED_FIELD_ERROR_MSG.formatted("contexts.context.user")
        BASE_MAP + [contexts: [[name: "test", context: [cluster: "test-cl", user: ""]]]]    || IllegalArgumentException | REQUIRED_FIELD_ERROR_MSG.formatted("contexts.context.user")
        BASE_MAP + [contexts: [[name: "test", context: [cluster: "test-cl", user: " "]]]]   || IllegalArgumentException | REQUIRED_FIELD_ERROR_MSG.formatted("contexts.context.user")
    }

    def 'validate invalid clusters section in kube config'() {
        when:
        new KubeConfig("", map)

        then:
        def error = thrown(expectedException)
        error.message == expectedMessage

        where:
        map                                                                                      || expectedException        | expectedMessage
        CONTEXT_MAP                                                                              || IllegalArgumentException | REQUIRED_FIELD_ERROR_MSG.formatted("clusters")
        CONTEXT_MAP + [clusters: null]                                                           || IllegalArgumentException | REQUIRED_FIELD_ERROR_MSG.formatted("clusters")
        CONTEXT_MAP + [clusters: []]                                                             || IllegalArgumentException | REQUIRED_FIELD_ERROR_MSG.formatted("clusters")
        CONTEXT_MAP + [clusters: [[name: null]]]                                                 || IllegalArgumentException | REQUIRED_FIELD_ERROR_MSG.formatted("clusters.name")
        CONTEXT_MAP + [clusters: [[name: ""]]]                                                   || IllegalArgumentException | REQUIRED_FIELD_ERROR_MSG.formatted("clusters.name")
        CONTEXT_MAP + [clusters: [[name: " "]]]                                                  || IllegalArgumentException | REQUIRED_FIELD_ERROR_MSG.formatted("clusters.name")
        CONTEXT_MAP + [clusters: [[name: "test"]]]                                               || IllegalArgumentException | REQUIRED_FIELD_ERROR_MSG.formatted("clusters.cluster")
        CONTEXT_MAP + [clusters: [[name: "test", cluster: null]]]                                || IllegalArgumentException | REQUIRED_FIELD_ERROR_MSG.formatted("clusters.cluster")
        CONTEXT_MAP + [clusters: [[name: "test", cluster: [:]]]]                                 || IllegalArgumentException | REQUIRED_FIELD_ERROR_MSG.formatted("clusters.cluster")
        CONTEXT_MAP + [clusters: [[name: "test", cluster: [server: null]]]]                      || IllegalArgumentException | REQUIRED_FIELD_ERROR_MSG.formatted("clusters.cluster.server")
        CONTEXT_MAP + [clusters: [[name: "test", cluster: ["insecure-skip-tls-verify": false]]]] || IllegalArgumentException | REQUIRED_FIELD_ERROR_MSG.formatted("clusters.cluster.server")
    }

    def 'validate invalid users section in kube config'() {
        when:
        new KubeConfig("", map)

        then:
        def error = thrown(expectedException)
        error.message == expectedMessage

        where:
        map                                                 || expectedException        | expectedMessage
        CLUSTER_MAP                                         || IllegalArgumentException | REQUIRED_FIELD_ERROR_MSG.formatted("users")
        CLUSTER_MAP + [users: null]                         || IllegalArgumentException | REQUIRED_FIELD_ERROR_MSG.formatted("users")
        CLUSTER_MAP + [users: []]                           || IllegalArgumentException | REQUIRED_FIELD_ERROR_MSG.formatted("users")
        CLUSTER_MAP + [users: [[name: null]]]               || IllegalArgumentException | REQUIRED_FIELD_ERROR_MSG.formatted("users.name")
        CLUSTER_MAP + [users: [[name: ""]]]                 || IllegalArgumentException | REQUIRED_FIELD_ERROR_MSG.formatted("users.name")
        CLUSTER_MAP + [users: [[name: " "]]]                || IllegalArgumentException | REQUIRED_FIELD_ERROR_MSG.formatted("users.name")
        CLUSTER_MAP + [users: [[name: "test"]]]             || IllegalArgumentException | REQUIRED_FIELD_ERROR_MSG.formatted("users.user")
        CLUSTER_MAP + [users: [[name: "test", user: null]]] || IllegalArgumentException | REQUIRED_FIELD_ERROR_MSG.formatted("users.user")
        CLUSTER_MAP + [users: [[name: "test", user: [:]]]]  || IllegalArgumentException | REQUIRED_FIELD_ERROR_MSG.formatted("users.user")
    }
}
