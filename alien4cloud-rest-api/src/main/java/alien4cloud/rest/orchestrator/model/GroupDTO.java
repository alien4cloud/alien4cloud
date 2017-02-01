package alien4cloud.rest.orchestrator.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Getter
public class GroupDTO {
    private String id;
    private String name;
    private String email;
    private String description;
}