package micronaut.client

import io.micronaut.context.env.Environment
import io.micronaut.http.annotation.Get
import io.micronaut.http.client.annotation.Client
import io.micronaut.kubernetes.test.EnabledIfAvailable
import io.micronaut.kubernetes.test.KubectlCommands
import io.micronaut.kubernetes.test.TestUtils
import io.micronaut.test.annotation.MicronautTest
import spock.lang.Ignore
import spock.lang.Requires
import spock.lang.Specification

import javax.inject.Inject

@MicronautTest(environments = Environment.KUBERNETES)
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
    @Ignore
    void "test config"() {
        expect:
        testClient.config("foo").equals("NOTHING")

        when:
        //FIXME this doesn't appear to work. Shoyld use an alternative eg https://github.com/kubernetes-client/java
        createConfigMap("HelloControllerSpec")

        then:
        testClient.config("foo").equals("bar")

        cleanup:
        deleteConfigMap("HelloControllerSpec")
    }
}


@Client("http://localhost:8888")
interface TestClient {

    @Get
    String index()

    @Get("/all")
    String all()

    @Get("/enemies")
    String enemies()

    @Get("/config/{key}")
    String config(String key)

}