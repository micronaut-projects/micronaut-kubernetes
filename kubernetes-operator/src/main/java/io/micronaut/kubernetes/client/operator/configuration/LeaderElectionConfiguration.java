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
package io.micronaut.kubernetes.client.operator.configuration;

import io.micronaut.core.annotation.NonNull;

import java.time.Duration;
import java.util.Optional;

/**
 * The {@link io.kubernetes.client.extended.leaderelection.LeaderElectionConfig} configuration.
 *
 * @author Pavol Gressa
 * @since 3.3
 */
public interface LeaderElectionConfiguration {

        /**
         * The lock lease duration. See {@link io.kubernetes.client.extended.leaderelection.LeaderElector}.
         *
         * @return lease duration
         */
        @NonNull
        Duration getLeaseDuration();

        /**
         * The lock renew deadline. If the {@link io.kubernetes.client.extended.leaderelection.LeaderElector} fails to
         * renew the lock within the deadline then the controller looses the lock.
         *
         * @return renew deadline
         */
        @NonNull
        Duration getRenewDeadline();

        /**
         * The lock acquire retry period. See {@link io.kubernetes.client.extended.leaderelection.LeaderElector}.
         *
         * @return lock acquire retry period.
         */
        @NonNull
        Duration getRetryPeriod();

        /**
         * The lock resource name. If not provided then the application name is used.
         *
         * @return the lock resource name
         */
        @NonNull
        Optional<String> getResourceName();

        /**
         * The lock resource namespace. If not provided then the application namespaces is used.
         *
         * @return the lock resource namespace
         */
        @NonNull
        Optional<String> getResourceNamespace();
}
