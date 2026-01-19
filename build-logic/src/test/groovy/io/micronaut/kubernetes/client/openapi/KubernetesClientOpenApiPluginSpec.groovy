package io.micronaut.kubernetes.client.openapi

import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.testfixtures.ProjectBuilder
import spock.lang.Specification
import spock.lang.TempDir

import java.nio.file.Files
import java.nio.file.Paths

class KubernetesClientOpenApiPluginSpec extends Specification {

    @TempDir
    File testProjectDir

    Project project

    Task downloadOpenApiSpecTask

    Action downloadOpenApiSpecAction

    Task createWatcherOpenApiSpecTask

    Action createWatcherOpenApiSpecAction

    def setup() {
        new File(testProjectDir, "build").mkdir()
        project = ProjectBuilder.builder().withProjectDir(testProjectDir).build()
        project.getPluginManager().apply("io.micronaut.kubernetes.client.openapi")
        def tasks = project.getTasks()
        downloadOpenApiSpecTask = tasks.getByName("downloadOpenApiSpec")
        downloadOpenApiSpecAction = downloadOpenApiSpecTask.getActions().get(0)
        createWatcherOpenApiSpecTask = tasks.getByName("createWatcherOpenApiSpec")
        createWatcherOpenApiSpecAction = createWatcherOpenApiSpecTask.getActions().get(0)
    }

    def "test download open api spec"() {
        given:
        def extension = project.getExtensions().getByName("kubernetesClientOpenApi")
        extension.specUrl.set(getClass().getResource('/openapi-input-1.yaml').toString())
        def expectedFile = new File(getClass().getResource('/openapi-output-1.yaml').toURI())

        when:
        downloadOpenApiSpecAction.execute(downloadOpenApiSpecTask)

        then:
        expectedFile.text == project.getLayout().getBuildDirectory().file("openapi.yaml").get().asFile.text
    }

    def "test create watcher open api spec"() {
        given:
        Files.createDirectories(Paths.get(project.rootDir.toString(), "build/generated/watcher/src/main/java/io/micronaut/kubernetes/client/openapi/watcher/mapper"));
        def extension = project.getExtensions().getByName("kubernetesClientOpenApi")
        extension.specUrl.set(getClass().getResource('/openapi-input-2.yaml').toString())
        extension.modelPackageName.set("io.micronaut.kubernetes.client.openapi.model")
        def expectedWatcherSpec = new File(getClass().getResource('/openapi-output-2.yaml').toURI())
        def expectedWatcherTypeMapping = new File(getClass().getResource('/openapi-output-type-mappings.txt').toURI())

        when:
        downloadOpenApiSpecAction.execute(downloadOpenApiSpecTask)
        createWatcherOpenApiSpecAction.execute(createWatcherOpenApiSpecTask)

        then:
        expectedWatcherSpec.text == project.getLayout().getBuildDirectory().file("openapi-watcher.yaml").get().asFile.text
        expectedWatcherTypeMapping.text == project.getLayout().getBuildDirectory().file("openapi-watcher-type-mappings.txt").get().asFile.text
    }
}
