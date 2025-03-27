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
package io.micronaut.kubernetes.client.openapi.operator.leaderelection;

import io.micronaut.context.annotation.Requires;
import io.micronaut.context.event.ShutdownEvent;
import io.micronaut.context.event.StartupEvent;
import io.micronaut.core.util.StringUtils;
import io.micronaut.kubernetes.client.openapi.config.KubernetesClientConfiguration;
import io.micronaut.kubernetes.client.openapi.informer.InformerConfiguration;
import io.micronaut.kubernetes.client.openapi.util.ThreadFactoryUtil;
import io.micronaut.runtime.event.annotation.EventListener;
import jakarta.inject.Singleton;

import java.util.concurrent.ThreadFactory;

/**
 * Starts up and shuts down the {@link LeaderElector}.
 */
@Singleton
@Requires(beans = KubernetesClientConfiguration.class)
@Requires(property = InformerConfiguration.PREFIX + ".enabled", value = StringUtils.TRUE, defaultValue = StringUtils.TRUE)
final class LeaderElectorLifecycleListener {

    private final ThreadFactory threadFactory;
    private final LeaderElector leaderElector;

    LeaderElectorLifecycleListener(ThreadFactoryUtil threadFactoryUtil, LeaderElector leaderElector) {
        this.threadFactory = threadFactoryUtil.getNamedThreadFactory("leader-elector-%d");
        this.leaderElector = leaderElector;
    }

    /**
     * Start leader elector on startup event.
     *
     * @param startupEvent startup event
     */
    @EventListener
    void startLeaderElector(StartupEvent startupEvent) {
        threadFactory.newThread(leaderElector::run).start();
    }

    /**
     * Stop leader elector on shutdown event.
     *
     * @param shutdownEvent shutdown event
     */
    @EventListener
    void stopLeaderElector(ShutdownEvent shutdownEvent) {
        leaderElector.stop();
    }
}
