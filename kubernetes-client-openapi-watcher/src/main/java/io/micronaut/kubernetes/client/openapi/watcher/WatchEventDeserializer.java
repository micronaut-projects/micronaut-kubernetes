/*
 * Copyright 2017-2024 original authors
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
package io.micronaut.kubernetes.client.openapi.watcher;

import io.micronaut.core.annotation.Internal;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import io.micronaut.core.type.Argument;
import io.micronaut.kubernetes.client.openapi.model.V1Status;
import io.micronaut.serde.Decoder;
import io.micronaut.serde.Deserializer;
import io.micronaut.serde.exceptions.SerdeException;
import jakarta.inject.Singleton;

import java.io.IOException;
import java.util.Optional;

/**
 * <p>Custom deserializer for kubernetes streamed events.</p>
 * <br />
 * <p>The deserializer first checks a value of the {@code type} field to determine how to
 * deserialize a value of the {@code object} field. If the value of the {@code type} field is equal
 * to {@code ERROR}, then the {@code object} field will be deserialized into an instance of {@link V1Status},
 * otherwise the {@code object} field will be deserialized into an instance of the class passed as the type parameter.
 * </p>
 */
@Internal
@Singleton
final class WatchEventDeserializer implements Deserializer<WatchEvent<?>> {

    @Override
    public @Nullable WatchEvent<?> deserialize(@NonNull Decoder decoder, @NonNull DecoderContext context, @NonNull Argument<? super WatchEvent<?>> type) throws IOException {
        Decoder objectDecoder = decoder.decodeObject();
        String typePropertyName = objectDecoder.decodeKey();
        if (!"type".equalsIgnoreCase(typePropertyName)) {
            throw new SerdeException("Unknown property [" + typePropertyName + "] encountered during deserialization of type: " + type);
        }
        String typePropertyValue = objectDecoder.decodeString();
        String objectPropertyName = objectDecoder.decodeKey();
        if (!"object".equalsIgnoreCase(objectPropertyName)) {
            throw new SerdeException("Unknown property [" + objectPropertyName + "] encountered during deserialization of type: " + type);
        }

        WatchEvent watchEvent;
        if ("ERROR".equalsIgnoreCase(typePropertyValue)) {
            Argument<V1Status> statusArgument = Argument.of(V1Status.class);
            Deserializer<? extends V1Status> statusDeserializer = context.findDeserializer(statusArgument).createSpecific(context, statusArgument);
            V1Status objectPropertyValue = statusDeserializer.deserializeNullable(objectDecoder, context, statusArgument);
            watchEvent = new WatchEvent(typePropertyValue, null, objectPropertyValue);
        } else {
            Optional<Argument<?>> typeParamOpt = type.getFirstTypeVariable();
            if (typeParamOpt.isEmpty()) {
                throw new SerdeException("Not found type parameter in type: " + type);
            }
            Argument typeVariable = typeParamOpt.get();
            Deserializer<?> objectDeserializer = context.findDeserializer(typeVariable).createSpecific(context, typeVariable);
            Object objectPropertyValue = objectDeserializer.deserializeNullable(objectDecoder, context, typeVariable);
            watchEvent = new WatchEvent(typePropertyValue, objectPropertyValue, null);
        }

        objectDecoder.finishStructure();

        return watchEvent;
    }
}
