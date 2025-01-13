package io.micronaut.kubernetes.client.openapi.informer.cache

import io.micronaut.kubernetes.client.openapi.common.KubernetesObject
import io.micronaut.kubernetes.client.openapi.model.V1ConfigMap
import io.micronaut.kubernetes.client.openapi.model.V1ObjectMeta
import io.micronaut.kubernetes.client.openapi.model.V1Pod
import io.micronaut.kubernetes.client.openapi.model.V1Secret
import spock.lang.Specification

import java.util.function.Function

class CacheSpec extends Specification {

    def 'add new object' () {
        given:
        Cache cache = new Cache<>()
        V1Pod pod = new V1Pod()
        V1ObjectMeta podMetadata = new V1ObjectMeta()
        podMetadata.setName('test-pod')
        podMetadata.setNamespace('test-namespace')
        pod.setMetadata(podMetadata)
        cache.add(pod)

        when:
        def object = cache.getByKey(cache.getKeyFunction().apply(pod))
        def objects = cache.byIndex(Cache.DEFAULT_INDEX_NAME, 'test-namespace')

        then:
        object != null
        object.getMetadata() != null
        with(object.getMetadata()) {
            getName() == 'test-pod'
            getNamespace() == 'test-namespace'
        }
        objects.size() == 1
        object == objects.get(0)
    }

    def 'update existing object' () {
        given:
        Cache cache = new Cache<>(
                Collections.singletonMap(Cache.DEFAULT_INDEX_NAME, new Function<KubernetesObject, List<String>>() {
                    @Override
                    List<String> apply(KubernetesObject object) {
                        V1ObjectMeta metadata = object.getMetadata()
                        return Arrays.asList(metadata.getGenerateName(), metadata.getResourceVersion())
                    }
                }))

        V1Pod pod1 = new V1Pod()
        V1ObjectMeta podMetadata1 = new V1ObjectMeta()
        podMetadata1.setName('test-pod')
        podMetadata1.setNamespace('test-namespace')
        podMetadata1.setGenerateName('test-gen-name-1')
        podMetadata1.setResourceVersion('test-res-version-1')
        pod1.setMetadata(podMetadata1)

        V1Pod pod2 = new V1Pod()
        V1ObjectMeta podMetadata2 = new V1ObjectMeta()
        podMetadata2.setName('test-pod')
        podMetadata2.setNamespace('test-namespace')
        podMetadata2.setGenerateName('test-gen-name-2')
        podMetadata2.setResourceVersion('test-res-version-2')
        pod2.setMetadata(podMetadata2)

        when:
        cache.add(pod1)
        def objects1 = cache.byIndex(Cache.DEFAULT_INDEX_NAME, 'test-gen-name-1')
        def objects2 = cache.byIndex(Cache.DEFAULT_INDEX_NAME, 'test-res-version-1')

        then:
        objects1 != null
        objects2 != null
        objects1.size() == 1
        objects2.size() == 1
        objects1.get(0) == objects2.get(0)

        when:
        cache.update(pod2)
        objects1 = cache.byIndex(Cache.DEFAULT_INDEX_NAME, 'test-gen-name-1')
        objects2 = cache.byIndex(Cache.DEFAULT_INDEX_NAME, 'test-res-version-1')
        def objects3 = cache.byIndex(Cache.DEFAULT_INDEX_NAME, 'test-gen-name-2')
        def objects4 = cache.byIndex(Cache.DEFAULT_INDEX_NAME, 'test-res-version-2')

        then:
        objects1 != null
        objects2 != null
        objects1.size() == 0
        objects2.size() == 0
        objects3 != null
        objects4 != null
        objects3.size() == 1
        objects4.size() == 1
        objects3.get(0) == objects4.get(0)
    }

    def 'delete existing object' () {
        given:
        Cache cache = new Cache<>()

        V1Pod pod1 = new V1Pod()
        V1ObjectMeta podMetadata1 = new V1ObjectMeta()
        podMetadata1.setName('test-pod')
        podMetadata1.setNamespace('test-namespace')
        pod1.setMetadata(podMetadata1)

        cache.add(pod1)

        V1Pod pod2 = new V1Pod()
        V1ObjectMeta podMetadata2 = new V1ObjectMeta()
        podMetadata2.setName('test-pod')
        podMetadata2.setNamespace('test-namespace')
        pod2.setMetadata(podMetadata2)

        when:
        cache.delete(pod2)
        def object = cache.getByKey(cache.getKeyFunction().apply(pod2))
        def objects = cache.byIndex(Cache.DEFAULT_INDEX_NAME, 'test-namespace')

        then:
        object == null
        objects.size() == 0
    }

    def 'replace all objects' () {
        given:
        Cache cache = new Cache<>()

        V1Pod pod = new V1Pod()
        V1ObjectMeta podMetadata = new V1ObjectMeta()
        podMetadata.setName('test-pod')
        podMetadata.setNamespace('test-namespace')
        pod.setMetadata(podMetadata)
        cache.add(pod)

        V1Secret secret = new V1Secret()
        V1ObjectMeta secretMetadata = new V1ObjectMeta()
        secretMetadata.setName('test-secret')
        secretMetadata.setNamespace('test-namespace')
        secret.setMetadata(secretMetadata)
        cache.add(secret)

        V1ConfigMap configMap = new V1ConfigMap()
        V1ObjectMeta configMapMetadata = new V1ObjectMeta()
        configMapMetadata.setName('test-config-map')
        configMapMetadata.setNamespace('test-namespace')
        configMap.setMetadata(configMapMetadata)

        when:
        def podObject = cache.getByKey(cache.getKeyFunction().apply(pod))
        def secretObject = cache.getByKey(cache.getKeyFunction().apply(secret))
        def objects = cache.byIndex(Cache.DEFAULT_INDEX_NAME, 'test-namespace')

        then:
        podObject != null
        podObject.getMetadata().getName() == 'test-pod'
        secretObject != null
        secretObject.getMetadata().getName() == 'test-secret'
        objects.size() == 2

        when:
        cache.replace(Collections.singletonList(configMap))
        podObject = cache.getByKey(cache.getKeyFunction().apply(pod))
        secretObject = cache.getByKey(cache.getKeyFunction().apply(secret))
        def configMapObject = cache.getByKey(cache.getKeyFunction().apply(configMap))
        objects = cache.byIndex(Cache.DEFAULT_INDEX_NAME, 'test-namespace')

        then:
        podObject == null
        secretObject == null
        configMapObject != null
        objects.size() == 1
        objects.get(0).getMetadata().getName() == 'test-config-map'
    }

    def 'get object keys' () {
        given:
        Cache cache = new Cache<>()

        V1Pod pod = new V1Pod()
        V1ObjectMeta podMetadata = new V1ObjectMeta()
        podMetadata.setName('test-pod')
        podMetadata.setNamespace('test-namespace')
        pod.setMetadata(podMetadata)
        cache.add(pod)

        V1Secret secret = new V1Secret()
        V1ObjectMeta secretMetadata = new V1ObjectMeta()
        secretMetadata.setName('test-secret')
        secretMetadata.setNamespace('test-namespace')
        secret.setMetadata(secretMetadata)
        cache.add(secret)

        V1ConfigMap configMap = new V1ConfigMap()
        V1ObjectMeta configMapMetadata = new V1ObjectMeta()
        configMapMetadata.setName('test-config-map')
        configMapMetadata.setNamespace('test-namespace')
        configMap.setMetadata(configMapMetadata)
        cache.add(configMap)

        when:
        def objects = cache.indexKeys(Cache.DEFAULT_INDEX_NAME, 'test-namespace')

        then:
        objects.size() == 3
        objects.contains('test-namespace/test-pod')
        objects.contains('test-namespace/test-secret')
        objects.contains('test-namespace/test-config-map')
    }

    def 'add new object when multiple indexers are used' () {
        given:
        Map<String, Function<KubernetesObject, List<String>>> indexFunctions = new HashMap<>()
        indexFunctions.put("index1", new Function<KubernetesObject, List<String>>() {
            @Override
            List<String> apply(KubernetesObject object) {
                V1ObjectMeta metadata = object.getMetadata()
                return Arrays.asList(metadata.getGenerateName())
            }
        })
        indexFunctions.put("index2", new Function<KubernetesObject, List<String>>() {
            @Override
            List<String> apply(KubernetesObject object) {
                V1ObjectMeta metadata = object.getMetadata()
                return Arrays.asList(metadata.getResourceVersion())
            }
        })
        Cache cache = new Cache<>(indexFunctions)
        V1Pod pod = new V1Pod()
        V1ObjectMeta podMetadata = new V1ObjectMeta()
        podMetadata.setName('test-pod')
        podMetadata.setNamespace('test-namespace')
        podMetadata.setGenerateName('test-gen-name-1')
        podMetadata.setResourceVersion('test-res-version-1')
        pod.setMetadata(podMetadata)
        cache.add(pod)

        when:
        def object = cache.getByKey(cache.getKeyFunction().apply(pod))
        def firstIndexObjects = cache.byIndex('index1', 'test-gen-name-1')
        def secondIndexObjects = cache.byIndex("index2", 'test-res-version-1')

        then:
        object != null
        firstIndexObjects != null
        secondIndexObjects != null
        firstIndexObjects.size() == 1
        secondIndexObjects.size() == 1
        firstIndexObjects.get(0) == secondIndexObjects.get(0)
    }
}
