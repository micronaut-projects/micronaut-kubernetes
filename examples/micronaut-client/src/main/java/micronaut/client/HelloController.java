/*
 * Copyright 2017-2019 original authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package micronaut.client;

import io.micronaut.discovery.DiscoveryClient;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.Post;
import org.reactivestreams.Publisher;

import java.util.List;

@Controller
public class HelloController {

    private ExampleClient client;
    private DiscoveryClient discoveryClient;

    public HelloController(ExampleClient client, DiscoveryClient discoveryClient) {
        this.client = client;
        this.discoveryClient = discoveryClient;
    }

    @Get
    public String index() {
        return client.hello("example-client");
    }

    @Get("/all")
    public Publisher<List<String>> all() {
        return discoveryClient.getServiceIds();
    }

    @Get("/enemies")
    public String enemies() {
        return client.enemiesCheatLevel();
    }

    @Get("/config/{key}")
    String config(String key) {
        return client.config(key);
    }

    @Post("/refreshService")
    String refresh() {
        return client.refresh();
    }
}