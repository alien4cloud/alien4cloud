package alien4cloud.rest.model;

import com.fasterxml.jackson.annotation.JsonRawValue;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class JsonRawRestResponse {
    @JsonRawValue
    private String data;
    private RestError error;
}