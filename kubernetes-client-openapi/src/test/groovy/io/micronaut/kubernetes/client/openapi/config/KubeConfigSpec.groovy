package io.micronaut.kubernetes.client.openapi.config

import spock.lang.Specification

import java.nio.charset.Charset

import static io.micronaut.kubernetes.client.openapi.config.KubeConfig.REQUIRED_FIELD_ERROR_MSG

class KubeConfigSpec extends Specification {

    private static final def CLUSTER_CERT_AUTH_DATA_BYTES = "test-cluster-certificate-data".getBytes(Charset.forName("UTF-8"))
    private static final def CLUSTER_CERT_AUTH_DATA = Base64.getEncoder().encodeToString(CLUSTER_CERT_AUTH_DATA_BYTES)

    private static final def CLIENT_CERT_DATA_BYTES = "test-client-certificate-data".getBytes(Charset.forName("UTF-8"))
    private static final def CLIENT_CERT_DATA = Base64.getEncoder().encodeToString(CLIENT_CERT_DATA_BYTES)

    private static final def CLIENT_KEY_DATA_BYTES = "test-client-key-data".getBytes(Charset.forName("UTF-8"))
    private static final def CLIENT_KEY_DATA = Base64.getEncoder().encodeToString(CLIENT_KEY_DATA_BYTES)

    private static final def BASE_MAP = ["current-context": "test-context"]
    private static final def CONTEXT_MAP = BASE_MAP + [contexts: [
            [name: "test-context", context: [cluster: "test-cluster", user: "test-user"]]
    ]]
    private static final def CLUSTER_MAP = CONTEXT_MAP + [clusters: [
            [name: "test-cluster",
             cluster: [
                     "server": "test-server",
                     "certificate-authority-data": CLUSTER_CERT_AUTH_DATA,
                     "insecure-skip-tls-verify": false
             ]]
    ]]
    private static final def FULL_MAP = CLUSTER_MAP + [users: [
            [name: "test-user",
             user: [
                     "client-certificate": "test-client-certificate",
                     "client-certificate-data": CLIENT_CERT_DATA,
                     "client-key": "test-client-key",
                     "client-key-data": CLIENT_KEY_DATA,
                     "username": "test-username",
                     "password": "test-password",
                     "exec": [
                             "apiVersion": "client.authentication.k8s.io/v1beta1",
                             "command": "test-command",
                             "args": ["test-arg1"],
                             "env": [["name": "test-env-name", "value": "test-env-value"]]
                     ],
                     "token": "test-token"
             ]]
    ]]

    def 'validate valid kube config'() {
        when:
        KubeConfig kubeConfig = new KubeConfig("", FULL_MAP)

        then:
        kubeConfig.getCluster() != null
        kubeConfig.getCluster().server() == "test-server"
        kubeConfig.getCluster().certificateAuthorityData() == CLUSTER_CERT_AUTH_DATA_BYTES
        kubeConfig.getUser() != null
        kubeConfig.getUser().clientCertificateData() == CLIENT_CERT_DATA_BYTES
        kubeConfig.getUser().clientKeyData() == CLIENT_KEY_DATA_BYTES
        kubeConfig.getUser().username() == "test-username"
        kubeConfig.getUser().password() == "test-password"
        kubeConfig.getUser().exec() != null
        kubeConfig.getUser().exec().apiVersion() == "client.authentication.k8s.io/v1beta1"
        kubeConfig.getUser().exec().command() == "test-command"
        kubeConfig.getUser().exec().args() != null
        kubeConfig.getUser().exec().args().size() == 1
        kubeConfig.getUser().exec().args().get(0) == "test-arg1"
        kubeConfig.getUser().exec().env() != null
        kubeConfig.getUser().exec().env().size() == 1
        kubeConfig.getUser().exec().env().get(0).name() == "test-env-name"
        kubeConfig.getUser().exec().env().get(0).value() == "test-env-value"
        kubeConfig.getUser().token() == "test-token"
    }

    def 'validate invalid contexts section in kube config'() {
        when:
        new KubeConfig("", map)

        then:
        def error = thrown(expectedException)
        error.message == expectedMessage

        where:
        map                                                                                         || expectedException        | expectedMessage
        BASE_MAP                                                                                    || IllegalArgumentException | REQUIRED_FIELD_ERROR_MSG.formatted("contexts")
        BASE_MAP + [contexts: null]                                                                 || IllegalArgumentException | REQUIRED_FIELD_ERROR_MSG.formatted("contexts")
        BASE_MAP + [contexts: []]                                                                   || IllegalArgumentException | REQUIRED_FIELD_ERROR_MSG.formatted("contexts")
        BASE_MAP + [contexts: [[name: null]]]                                                       || IllegalArgumentException | REQUIRED_FIELD_ERROR_MSG.formatted("contexts.name")
        BASE_MAP + [contexts: [[name: ""]]]                                                         || IllegalArgumentException | REQUIRED_FIELD_ERROR_MSG.formatted("contexts.name")
        BASE_MAP + [contexts: [[name: " "]]]                                                        || IllegalArgumentException | REQUIRED_FIELD_ERROR_MSG.formatted("contexts.name")
        BASE_MAP + [contexts: [[name: "test-context"]]]                                             || IllegalArgumentException | REQUIRED_FIELD_ERROR_MSG.formatted("contexts.context")
        BASE_MAP + [contexts: [[name: "test-context", context: null]]]                              || IllegalArgumentException | REQUIRED_FIELD_ERROR_MSG.formatted("contexts.context")
        BASE_MAP + [contexts: [[name: "test-context", context: [:]]]]                               || IllegalArgumentException | REQUIRED_FIELD_ERROR_MSG.formatted("contexts.context")
        BASE_MAP + [contexts: [[name: "test-context", context: [cluster: null]]]]                   || IllegalArgumentException | REQUIRED_FIELD_ERROR_MSG.formatted("contexts.context.cluster")
        BASE_MAP + [contexts: [[name: "test-context", context: [cluster: ""]]]]                     || IllegalArgumentException | REQUIRED_FIELD_ERROR_MSG.formatted("contexts.context.cluster")
        BASE_MAP + [contexts: [[name: "test-context", context: [cluster: " "]]]]                    || IllegalArgumentException | REQUIRED_FIELD_ERROR_MSG.formatted("contexts.context.cluster")
        BASE_MAP + [contexts: [[name: "test-context", context: [cluster: "test-cl", user: null]]]]  || IllegalArgumentException | REQUIRED_FIELD_ERROR_MSG.formatted("contexts.context.user")
        BASE_MAP + [contexts: [[name: "test-context", context: [cluster: "test-cl", user: ""]]]]    || IllegalArgumentException | REQUIRED_FIELD_ERROR_MSG.formatted("contexts.context.user")
        BASE_MAP + [contexts: [[name: "test-context", context: [cluster: "test-cl", user: " "]]]]   || IllegalArgumentException | REQUIRED_FIELD_ERROR_MSG.formatted("contexts.context.user")
    }

    def 'validate invalid clusters section in kube config'() {
        when:
        new KubeConfig("", map)

        then:
        def error = thrown(expectedException)
        error.message == expectedMessage

        where:
        map                                                                                              || expectedException        | expectedMessage
        CONTEXT_MAP                                                                                      || IllegalArgumentException | REQUIRED_FIELD_ERROR_MSG.formatted("clusters")
        CONTEXT_MAP + [clusters: null]                                                                   || IllegalArgumentException | REQUIRED_FIELD_ERROR_MSG.formatted("clusters")
        CONTEXT_MAP + [clusters: []]                                                                     || IllegalArgumentException | REQUIRED_FIELD_ERROR_MSG.formatted("clusters")
        CONTEXT_MAP + [clusters: [[name: null]]]                                                         || IllegalArgumentException | REQUIRED_FIELD_ERROR_MSG.formatted("clusters.name")
        CONTEXT_MAP + [clusters: [[name: ""]]]                                                           || IllegalArgumentException | REQUIRED_FIELD_ERROR_MSG.formatted("clusters.name")
        CONTEXT_MAP + [clusters: [[name: " "]]]                                                          || IllegalArgumentException | REQUIRED_FIELD_ERROR_MSG.formatted("clusters.name")
        CONTEXT_MAP + [clusters: [[name: "test-cluster"]]]                                               || IllegalArgumentException | REQUIRED_FIELD_ERROR_MSG.formatted("clusters.cluster")
        CONTEXT_MAP + [clusters: [[name: "test-cluster", cluster: null]]]                                || IllegalArgumentException | REQUIRED_FIELD_ERROR_MSG.formatted("clusters.cluster")
        CONTEXT_MAP + [clusters: [[name: "test-cluster", cluster: [:]]]]                                 || IllegalArgumentException | REQUIRED_FIELD_ERROR_MSG.formatted("clusters.cluster")
        CONTEXT_MAP + [clusters: [[name: "test-cluster", cluster: [server: null]]]]                      || IllegalArgumentException | REQUIRED_FIELD_ERROR_MSG.formatted("clusters.cluster.server")
        CONTEXT_MAP + [clusters: [[name: "test-cluster", cluster: ["insecure-skip-tls-verify": false]]]] || IllegalArgumentException | REQUIRED_FIELD_ERROR_MSG.formatted("clusters.cluster.server")
    }

    def 'validate invalid users section in kube config'() {
        when:
        new KubeConfig("", map)

        then:
        def error = thrown(expectedException)
        error.message == expectedMessage

        where:
        map                                                      || expectedException        | expectedMessage
        CLUSTER_MAP                                              || IllegalArgumentException | REQUIRED_FIELD_ERROR_MSG.formatted("users")
        CLUSTER_MAP + [users: null]                              || IllegalArgumentException | REQUIRED_FIELD_ERROR_MSG.formatted("users")
        CLUSTER_MAP + [users: []]                                || IllegalArgumentException | REQUIRED_FIELD_ERROR_MSG.formatted("users")
        CLUSTER_MAP + [users: [[name: null]]]                    || IllegalArgumentException | REQUIRED_FIELD_ERROR_MSG.formatted("users.name")
        CLUSTER_MAP + [users: [[name: ""]]]                      || IllegalArgumentException | REQUIRED_FIELD_ERROR_MSG.formatted("users.name")
        CLUSTER_MAP + [users: [[name: " "]]]                     || IllegalArgumentException | REQUIRED_FIELD_ERROR_MSG.formatted("users.name")
        CLUSTER_MAP + [users: [[name: "test-user"]]]             || IllegalArgumentException | REQUIRED_FIELD_ERROR_MSG.formatted("users.user")
        CLUSTER_MAP + [users: [[name: "test-user", user: null]]] || IllegalArgumentException | REQUIRED_FIELD_ERROR_MSG.formatted("users.user")
        CLUSTER_MAP + [users: [[name: "test-user", user: [:]]]]  || IllegalArgumentException | REQUIRED_FIELD_ERROR_MSG.formatted("users.user")
    }
}
