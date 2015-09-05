package alien4cloud.orchestrators.rest.model;

import org.elasticsearch.annotation.ESObject;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateLocationResourceTemplateRequest {

    private String name;

    private Boolean enabled;

    private Boolean generated;

    private Boolean isService;

}
