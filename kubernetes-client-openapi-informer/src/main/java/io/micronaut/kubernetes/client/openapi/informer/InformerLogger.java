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

import io.micronaut.core.util.StringUtils;
import org.slf4j.Logger;

/**
 * Logger which adds kubernetes type and namespace details to all logged messages.
 */
final class InformerLogger {

    private final Logger logger;
    private final Class apiTypeClass;
    private final String namespace;
    private final String messagePrefix;

    InformerLogger(Logger logger, Class apiTypeClass, String namespace) {
        this.logger = logger;
        this.apiTypeClass = apiTypeClass;
        this.namespace = namespace;
        this.messagePrefix = StringUtils.isEmpty(namespace) ? "Type={}: " : "Type={}, Namespace={}: ";
    }

    void logError(String message, Object... arguments) {
        if (logger.isErrorEnabled()) {
            logger.error(createLogMessage(message), createLogArgs(arguments));
        }
    }

    void logWarn(String message, Object... arguments) {
        if (logger.isWarnEnabled()) {
            logger.warn(createLogMessage(message), createLogArgs(arguments));
        }
    }

    void logInfo(String message, Object... arguments) {
        if (logger.isInfoEnabled()) {
            logger.info(createLogMessage(message), createLogArgs(arguments));
        }
    }

    void logDebug(String message, Object... arguments) {
        if (logger.isDebugEnabled()) {
            logger.debug(createLogMessage(message), createLogArgs(arguments));
        }
    }

    private String createLogMessage(String message) {
        return messagePrefix + message;
    }

    private Object[] createLogArgs(Object... arguments) {
        Object[] newArguments;
        if (StringUtils.isEmpty(namespace)) {
            newArguments = new Object[arguments.length + 1];
            newArguments[0] = apiTypeClass.getSimpleName();
        } else {
            newArguments = new Object[arguments.length + 2];
            newArguments[0] = apiTypeClass.getSimpleName();
            newArguments[1] = namespace;
        }
        if (arguments.length > 0) {
            int destPos = StringUtils.isEmpty(namespace) ? 1 : 2;
            System.arraycopy(arguments, 0, newArguments, destPos, arguments.length);
        }
        return newArguments;
    }
}
