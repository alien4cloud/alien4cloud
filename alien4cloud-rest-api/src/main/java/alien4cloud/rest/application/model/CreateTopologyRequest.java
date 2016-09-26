package alien4cloud.rest.application.model;

import org.hibernate.validator.constraints.NotBlank;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 *
 * Request to create a new topology
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor(suppressConstructorProperties = true)
public class CreateTopologyRequest {
    @NotBlank
    private String name;
    @NotBlank
    private String version;
    private String description;
    private String fromTopologyId;
}