package alien4cloud.rest.deployment.model;

import com.fasterxml.jackson.annotation.JsonRawValue;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Similar to get multiple data result but keep the result as a raw json string to avoid deserialization and serialization.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class GetMultipleJsonResult {
    private static final long serialVersionUID = 1L;
    @JsonRawValue
    private String data;
    private long queryDuration;
    private long totalResults;
    private int from;
    private int to;
}