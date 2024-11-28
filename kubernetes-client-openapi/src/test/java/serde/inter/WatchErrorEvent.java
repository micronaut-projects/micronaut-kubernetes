package serde.inter;

import io.micronaut.kubernetes.client.openapi.model.V1Status;
import io.micronaut.serde.annotation.Serdeable;

@Serdeable
public record WatchErrorEvent(String type, V1Status object) implements WatchEvent {}
