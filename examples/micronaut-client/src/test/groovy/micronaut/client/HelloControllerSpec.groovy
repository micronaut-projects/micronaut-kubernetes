package micronaut.client

import groovy.util.logging.Slf4j
import io.micronaut.context.annotation.Property
import io.micronaut.context.env.Environment
import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.Post
import io.micronaut.http.client.annotation.Client
import io.micronaut.kubernetes.test.KubectlCommands
import io.micronaut.kubernetes.test.TestUtils
import io.micronaut.test.extensions.spock.annotation.MicronautTest
import spock.lang.Requires
import spock.lang.Specification
import spock.util.concurrent.PollingConditions

import javax.inject.Inject

import io.micronaut.context.annotation.Requires as MicronautRequires

@MicronautTest(environments = Environment.KUBERNETES)
@Property(name = "spec.name", value = "HelloControllerSpec")
@Slf4j
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
        given:
        PollingConditions conditions = new PollingConditions(timeout: 30, delay: 2)
        String configMapName = "hello-controller-spec"

        expect:
        testClient.config("foo").equals("NOTHING")
        !testClient.env().contains("configMapName")

        when:
        createConfigMap(configMapName)

        then:
        conditions.eventually {
            testClient.config("foo").equals("bar")
            testClient.env().contains(configMapName)
        }

        when:
        modifyConfigMap(configMapName)

        then:
        conditions.eventually {
            testClient.config("foo").equals("baz")
            testClient.env().contains(configMapName)
        }

        when:
        deleteConfigMap(configMapName)

        then:
        conditions.eventually {
            !testClient.env().contains(configMapName)
        }

        cleanup:
        deleteConfigMap(configMapName)
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

        @Get("/serviceEnv")
        String env()

    }
}