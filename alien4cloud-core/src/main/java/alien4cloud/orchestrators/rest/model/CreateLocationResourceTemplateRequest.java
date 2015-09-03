package alien4cloud.orchestrators.rest.model;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateLocationResourceTemplateRequest {
    private String resourceType;
    private String resourceName;
}
