/*
 * Copyright 2017-2021 original authors
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
package io.micronaut.kubernetes.client.informer;

import java.util.function.Supplier;

/**
 * Default namespaces supplier for the {@link Informer#namespacesSupplier()} ()} that returns empty array.
 *
 * @author Pavol Gressa
 * @since 3.1
 */
public class EmptyNamespacesSupplier implements Supplier<String[]> {
    @Override
    public String[] get() {
        return new String[0];
    }
}
