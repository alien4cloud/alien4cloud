package alien4cloud.rest.model;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@SuppressWarnings("PMD.UnusedPrivateField")
public class RestResponse<T> {
    private T data;
    private RestError error;
}