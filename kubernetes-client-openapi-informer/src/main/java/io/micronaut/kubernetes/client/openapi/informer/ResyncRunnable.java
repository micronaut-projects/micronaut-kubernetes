/*
 * Copyright 2017-2025 original authors
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
package io.micronaut.kubernetes.client.openapi.informer;

import io.micronaut.kubernetes.client.openapi.common.KubernetesObject;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Supplier;

/**
 * Runnable which resync the object cache if required by listeners.
 */
final class ResyncRunnable implements Runnable {

    private static final Logger LOG = LoggerFactory.getLogger(ResyncRunnable.class);

    private final DeltaFifo deltaFifo;
    private final Supplier<Boolean> shouldResyncFunc;
    private final InformerLogger informerLogger;

    ResyncRunnable(DeltaFifo deltaFifo,
                   Supplier<Boolean> shouldResyncFunc,
                   Class<? extends KubernetesObject> apiTypeClass,
                   @Nullable String namespace) {
        this.deltaFifo = deltaFifo;
        this.shouldResyncFunc = shouldResyncFunc;
        this.informerLogger = new InformerLogger(LOG, apiTypeClass, namespace);
    }

    @Override
    public void run() {
        informerLogger.logDebug("ResyncRunnable#resync ticker tick");
        if (shouldResyncFunc == null || Boolean.TRUE.equals(shouldResyncFunc.get())) {
            informerLogger.logDebug("ResyncRunnable#force resync");
            deltaFifo.resync();
        }
    }
}
