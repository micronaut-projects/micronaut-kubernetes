/*
 * Copyright 2017-2020 original authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package micronaut.informer;

import io.kubernetes.client.informer.SharedIndexInformer;
import io.kubernetes.client.informer.cache.Indexer;
import io.kubernetes.client.openapi.models.V1Secret;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.kubernetes.client.NamespaceResolver;
import io.micronaut.kubernetes.client.informer.SharedIndexInformerFactory;

import java.util.Collection;

@Controller
public class SecretInformerController {

    private final SharedIndexInformerFactory sharedIndexInformerFactory;

    private final NamespaceResolver namespaceResolver;

    public SecretInformerController(SharedIndexInformerFactory sharedIndexInformerFactory, NamespaceResolver namespaceResolver) {
        this.sharedIndexInformerFactory = sharedIndexInformerFactory;
        this.namespaceResolver = namespaceResolver;
    }

    @Get("/all")
    public Collection<V1Secret> all() {
        String namespace = namespaceResolver.resolveNamespace();
        SharedIndexInformer<V1Secret> informer = sharedIndexInformerFactory.getExistingSharedIndexInformer(namespace, V1Secret.class);
        Indexer<V1Secret> indexer = informer.getIndexer();
        return indexer.list();
    }

    @Get("/secret/{key}")
    public V1Secret secret(String key) {
        String namespace = namespaceResolver.resolveNamespace();
        SharedIndexInformer<V1Secret> informer = sharedIndexInformerFactory.getExistingSharedIndexInformer(namespace, V1Secret.class);
        Indexer<V1Secret> indexer = informer.getIndexer();
        return indexer.getByKey(namespace + "/" + key);
    }
}
