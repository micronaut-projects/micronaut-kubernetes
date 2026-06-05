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

import io.micronaut.core.type.Argument;
import io.micronaut.serde.Decoder;
import io.micronaut.serde.Encoder;
import io.micronaut.serde.Serde;
import io.micronaut.serde.exceptions.SerdeException;
import jakarta.inject.Singleton;
import org.jspecify.annotations.NonNull;

import java.io.IOException;

/**
 * Custom serializer and deserializer for {@link IntOrString}.
 */
@Singleton
public class IntOrStringSerde implements Serde<IntOrString> {

    @Override
    public void serialize(@NonNull Encoder encoder, @NonNull EncoderContext context, @NonNull Argument<? extends IntOrString> type, @NonNull IntOrString value) throws IOException {
        if (value.isInteger()) {
            encoder.encodeInt(value.getIntValue());
        } else {
            encoder.encodeString(value.getStrValue());
        }
    }

    @Override
    public IntOrString deserialize(@NonNull Decoder decoder, @NonNull DecoderContext context, @NonNull Argument<? super IntOrString> type) throws IOException {
        Object value = decoder.decodeArbitrary();
        if (value instanceof String stringValue) {
            return new IntOrString(stringValue);
        } else if (value instanceof Integer intValue) {
            return new IntOrString(intValue);
        } else {
            throw new SerdeException("Could not deserialize " + value + " into IntOrString type.");
        }
    }
}
