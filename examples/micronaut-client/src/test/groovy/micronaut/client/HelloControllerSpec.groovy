package micronaut.client

import io.micronaut.context.annotation.Property
import io.micronaut.context.env.Environment
import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.Post
import io.micronaut.http.client.annotation.Client
import io.micronaut.kubernetes.test.KubectlCommands
import io.micronaut.kubernetes.test.TestUtils
import io.micronaut.test.annotation.MicronautTest
import spock.lang.Requires
import spock.lang.Specification

import javax.inject.Inject

import io.micronaut.context.annotation.Requires as MicronautRequires

@MicronautTest(environments = Environment.KUBERNETES)
@Property(name = "spec.name", value = "HelloControllerSpec")
class HelloControllerSpec extends Specification implements KubectlCommands {

    @Inject
    TestClient testClient

    @Requires({ TestUtils.available("http://localhost:8888") })
    void "test index"() {
        expect:
        testClient.index().startsWith("Hello, example-client")
    }

    @Requires({ TestUtils.available("http://localhost:8888") })
    void "test all"() {
        expect:
        testClient.all().contains("example-service")
    }

    @Requires({ TestUtils.available("http://localhost:8888") })
    void "test enemies"() {
        expect:
        testClient.enemies().equals("noGoodRotten")
    }

    @Requires({ TestUtils.available("http://localhost:8888") })
    void "test config"() {
        expect:
        testClient.config("foo").equals("NOTHING")

        when:
        createConfigMap("hello-controller-spec")
        sleep 2_000
        testClient.refresh()

        then:
        testClient.config("foo").equals("bar")

        when:
        modifyConfigMap("hello-controller-spec")
        sleep 2_000

        then:
        testClient.config("foo").equals("baz")

        when:
        deleteConfigMap("hello-controller-spec")
        sleep 5_000
        testClient.refresh()

        then:
        testClient.config("foo").equals("NOTHING")

        cleanup:
        deleteConfigMap("hello-controller-spec")
    }

    @Requires({ TestUtils.available("http://localhost:8888") })
    void "test reading secrets from mounted volumes"() {
        given:
        testClient.refresh()

        expect:
        testClient.config("mounted-volume-key").equals("mountedVolumeValue")
    }

    @Client("http://localhost:8888")
    @MicronautRequires(property = "spec.name", value = "HelloControllerSpec")
    static interface TestClient {

        @Get
        String index()

        @Get("/all")
        String all()

        @Get("/enemies")
        String enemies()

        @Get("/config/{key}")
        String config(String key)

        @Post("/refreshService")
        String refresh()

    }
}