package io.confluent.devrel.producer;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Data
public class MyEvent {

    @JsonProperty
    private String id;
    @JsonProperty
    private String type;
    @JsonProperty
    private String content;
}