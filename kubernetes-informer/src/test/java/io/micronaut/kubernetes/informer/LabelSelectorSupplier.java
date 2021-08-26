/*
 * Copyright 2021 original authors
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
package io.micronaut.kubernetes.informer;

import io.micronaut.context.ApplicationContext;
import io.micronaut.context.annotation.Requires;
import jakarta.inject.Singleton;

import java.util.function.Supplier;

@Singleton
@Requires(property = "spec.name", value = "ConfigMapLabelSelectorInformerSpec")
public class LabelSelectorSupplier implements Supplier<String> {

    private final ApplicationContext  applicationContext;

    public LabelSelectorSupplier(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    @Override
    public String get() {
        return "environment=test";
    }
}
