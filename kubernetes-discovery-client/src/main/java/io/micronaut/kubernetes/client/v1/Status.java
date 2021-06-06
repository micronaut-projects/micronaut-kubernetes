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
package io.micronaut.kubernetes.client.v1;

import io.micronaut.core.annotation.Introspected;

/**
 * @see <a href="https://github.com/kubernetes/community/blob/master/contributors/devel/sig-architecture/api-conventions.md#response-status-kind">Response Status Kind</a>
 *
 * @author Régis Desgroppes
 * @since 2.3.4
 */
@Introspected
public class Status extends KubernetesObject {

    private String status;
    private String message;
    private String reason;
    private int code;

    /**
     * @return The Status
     */
    public String getStatus() {
        return status;
    }

    /**
     * @param status The Status
     */
    public void setStatus(final String status) {
        this.status = status;
    }

    /**
     * @return The Message
     */
    public String getMessage() {
        return message;
    }

    /**
     * @param message The Message
     */
    public void setMessage(final String message) {
        this.message = message;
    }

    /**
     * @return The Reason
     */
    public String getReason() {
        return reason;
    }

    /**
     * @param reason The Reason
     */
    public void setReason(final String reason) {
        this.reason = reason;
    }

    /**
     * @return The Code
     */
    public int getCode() {
        return code;
    }

    /**
     * @param code The Code
     */
    public void setCode(final int code) {
        this.code = code;
    }

    @Override
    public String toString() {
        return "Status{" +
                "status=" + status +
                ", message=" + message +
                ", reason=" + reason +
                ", code=" + code +
                '}';
    }
}
