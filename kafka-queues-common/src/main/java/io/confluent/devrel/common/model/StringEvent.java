package io.confluent.devrel.common.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Data
public class StringEvent {

    @JsonProperty
    private String id;
    @JsonProperty
    private String type;
    @JsonProperty
    private String content;
}