package alien4cloud.rest.repository.model;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateRepositoryRequest {

    private String name;

    private String pluginId;
    
    private Object configuration;
}
