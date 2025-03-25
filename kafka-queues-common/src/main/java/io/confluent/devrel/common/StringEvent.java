package io.confluent.devrel.common;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Data
public class StringEvent {
    private String id;
    private String type;
    private String content;
}