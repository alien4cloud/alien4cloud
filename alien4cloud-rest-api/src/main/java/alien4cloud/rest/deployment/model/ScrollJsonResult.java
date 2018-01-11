package alien4cloud.rest.deployment.model;

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonRawValue;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Return sources with no alien serialization steps (just return source from ES).
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ScrollJsonResult implements Serializable {
    private static final long serialVersionUID = 1L;
    @JsonRawValue
    private String data;
    private long queryDuration;
    private long totalResults;
    private String scrollId;
}