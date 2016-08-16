package alien4cloud.rest.repository.model;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateRepositoryRequest {

    private String name;

    private Object configuration;

}
