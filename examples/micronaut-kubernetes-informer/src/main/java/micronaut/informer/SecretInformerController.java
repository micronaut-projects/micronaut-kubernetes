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

import io.kubernetes.client.openapi.models.V1Secret;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import java.util.Collection;

@Controller
public class SecretInformerController {

    private final SecretResourceEventHandler secretResourceEventHandler;

    public SecretInformerController(SecretResourceEventHandler secretResourceEventHandler) {
        this.secretResourceEventHandler = secretResourceEventHandler;
    }

    @Get("/all")
    public Collection<V1Secret> all() {
        return secretResourceEventHandler.getV1SecretMap().values();
    }

    @Get("/secret/{key}")
    public V1Secret secret(String key) {
        return secretResourceEventHandler.getV1SecretMap().get(key);
    }
}
