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
package io.micronaut.kubernetes.client.openapi.type;

import io.micronaut.serde.annotation.Serdeable;

import java.util.Objects;

/**
 * Custom type which is a wrapper for string or integer value.
 *
 * <p>
 * The code has been copied from the official client and modified:
 * <a href="https://raw.githubusercontent.com/kubernetes-client/java/refs/tags/v21.0.2/kubernetes/src/main/java/io/kubernetes/client/custom/IntOrString.java">IntOrString</a>
 * </p>
 */
@Serdeable
public class IntOrString {

    private final boolean isInt;
    private final String strValue;
    private final Integer intValue;

    public IntOrString(final String value) {
        this.isInt = false;
        this.strValue = value;
        this.intValue = null;
    }

    public IntOrString(final int value) {
        this.isInt = true;
        this.intValue = value;
        this.strValue = null;
    }

    public boolean isInteger() {
        return isInt;
    }

    public String getStrValue() {
        if (isInt) {
            throw new IllegalStateException("Not a string");
        }
        return strValue;
    }

    public Integer getIntValue() {
        if (!isInt) {
            throw new IllegalStateException("Not an integer");
        }
        return intValue;
    }

    @Override
    public String toString() {
        return (isInt ? String.valueOf(intValue) : strValue);
    }

    @Override
    public boolean equals(Object o) {
        return this == o || (o instanceof IntOrString && equals((IntOrString) o));
    }

    private boolean equals(IntOrString o) {
        if (isInt != o.isInt) {
            return false;
        }
        return isInt ? Objects.equals(intValue, o.intValue) : Objects.equals(strValue, o.strValue);
    }

    @Override
    public int hashCode() {
        return Objects.hash(isInt, strValue, intValue);
    }
}
