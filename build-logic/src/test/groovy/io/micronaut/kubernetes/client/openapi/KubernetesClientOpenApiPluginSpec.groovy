package io.micronaut.kubernetes.client.openapi

import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import spock.lang.Specification

import java.nio.file.Path

class KubernetesClientOpenApiPluginSpec extends Specification {

    @Rule
    TemporaryFolder testProjectDir = new TemporaryFolder()
    Path baseDir

    File getSettingsFile() {
        baseDir.resolve("settings.gradle").toFile()
    }

    File getBuildFile() {
        baseDir.resolve("build.gradle").toFile()
    }

    String getMicronautVersion() {
        System.getProperty("micronautVersion")
    }

    def setup() {
        baseDir = testProjectDir.root.toPath()
    }

    BuildResult build(String... args) {
        def runner = GradleRunner.create().withPluginClasspath()
        runner.withProjectDir(baseDir.toFile())
                .withArguments(["--no-watch-fs",
                                "-S",
                                "-Porg.gradle.java.installations.auto-download=false",
                                "-Porg.gradle.java.installations.auto-detect=false",
                                "-Dio.micronaut.graalvm.rich.output=false",
                                *args])
                .forwardStdOutput(System.out.newWriter())
                .forwardStdError(System.err.newWriter())
                .withDebug(true)
                .run()
    }

    def "test plugin"() {
        given:
        settingsFile << "rootProject.name = 'hello-world'"
        buildFile << """
            plugins {
                id 'io.micronaut.build.internal.kubernetes-module'
                id 'io.micronaut.openapi'
                id 'io.micronaut.kubernetes.client.openapi'
            }

            micronaut {
                version "$micronautVersion"
                runtime "netty"
                testRuntime "junit5"
            }
            mainClassName="example.Application"
        """


        when:
        def result = build('build')

        def task = result.task(":build")
        println result.output

        then:
        task.outcome == TaskOutcome.SUCCESS
        //testProjectDir.root.toPath()
                //.resolve('build/classes/java/test/example/$ExampleTest$Definition.class').toFile().exists()
    }
}
