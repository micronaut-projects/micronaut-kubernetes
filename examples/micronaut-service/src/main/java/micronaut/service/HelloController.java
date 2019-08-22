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
package micronaut.service;

import io.micronaut.context.ApplicationContext;
import io.micronaut.context.annotation.Value;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.runtime.context.scope.Refreshable;
import io.micronaut.runtime.server.EmbeddedServer;

import javax.inject.Inject;

@Controller("/hello")
@Refreshable
public class HelloController {

    @Inject
    EmbeddedServer embeddedServer;

    @Inject
    ApplicationContext context;

    @Value("${enemies.cheat.level:defaultCheatLevel}")
    private String enemiesCheatLevel;

    @Get("/{name}")
    public String index(final String name) {
        return "Hello, " + name + ". From " + embeddedServer.getHost() + ":" + embeddedServer.getPort() + "\n";
    }

    @Get("/enemies")
    public String enemies() {
        return enemiesCheatLevel;
    }

    @Get("/config/{key}")
    public String config(String key) {
        return context.get(key, String.class).orElse("NOTHING");
    }
}