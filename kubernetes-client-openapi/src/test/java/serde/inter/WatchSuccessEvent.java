package serde.inter;

import io.micronaut.kubernetes.client.openapi.model.V1Namespace;
import io.micronaut.serde.annotation.Serdeable;

@Serdeable
public record WatchSuccessEvent(String type, V1Namespace object) implements WatchEvent {}
