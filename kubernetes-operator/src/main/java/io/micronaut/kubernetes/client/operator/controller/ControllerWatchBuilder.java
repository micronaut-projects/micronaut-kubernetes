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
package io.micronaut.kubernetes.client.operator.controller;

import io.kubernetes.client.common.KubernetesObject;
import io.kubernetes.client.extended.controller.ControllerWatch;
import io.kubernetes.client.extended.controller.reconciler.Request;
import io.kubernetes.client.extended.workqueue.WorkQueue;
import io.micronaut.context.annotation.DefaultImplementation;
import io.micronaut.core.annotation.NonNull;
import io.micronaut.kubernetes.client.operator.ControllerConfiguration;

/**
 * The {@link ControllerWatch} builder.
 *
 * @author Pavol Gressa
 * @since 3.3
 */
@DefaultImplementation(DefaultControllerWatchBuilder.class)
public interface ControllerWatchBuilder {

     /**
      * Builds {@link ControllerWatch}.
      *
      * @param operator the operator
      * @param workQueue the work queue
      * @return the controller watch
      */
     @NonNull
     ControllerWatch<? extends KubernetesObject> buildControllerWatch(@NonNull ControllerConfiguration operator, @NonNull WorkQueue<Request> workQueue);
}
