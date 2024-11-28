package serde.inter;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.micronaut.serde.annotation.Serdeable;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes({
    @JsonSubTypes.Type(value = WatchSuccessEvent.class, name = "ADDED"),
    @JsonSubTypes.Type(value = WatchErrorEvent.class, name = "ERROR")
})
@Serdeable
public sealed interface WatchEvent permits WatchErrorEvent, WatchSuccessEvent {
}
