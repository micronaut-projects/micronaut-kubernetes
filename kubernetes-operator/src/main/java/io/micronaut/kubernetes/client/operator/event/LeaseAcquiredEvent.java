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
package io.micronaut.kubernetes.client.operator.event;

import io.kubernetes.client.extended.controller.ControllerManager;
import io.micronaut.kubernetes.client.operator.ControllerConfiguration;

/**
 * Event fired when the controller created from the {@link AbstracLeaderElectingControllerEvent#getOperatorConfiguration()}
 * has become a leader.
 *
 * @author Pavol Gressa
 * @since 3.3
 */
public class LeaseAcquiredEvent extends AbstracLeaderElectingControllerEvent {

    /**
     * Create the event.
     *
     * @param controllerConfiguration operator configuration
     * @param source                controller manager
     */
    public LeaseAcquiredEvent(ControllerConfiguration controllerConfiguration, ControllerManager source) {
        super(controllerConfiguration, source);
    }
}
