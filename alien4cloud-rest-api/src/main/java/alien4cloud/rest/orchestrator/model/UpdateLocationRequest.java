package alien4cloud.rest.orchestrator.model;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateLocationRequest {
    private String name;
    private String environmentType;
}