package alien4cloud.rest.service.model;

import org.hibernate.validator.constraints.NotEmpty;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Create a service for an environment.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor(suppressConstructorProperties = true)
@ApiModel("Request for creation of a new service.")
public class CreateManagedServiceResourceRequest {
    @NotEmpty
    @ApiModelProperty(value = "Name of the new service (must be unique for a given version).", required = true)
    private String serviceName;
}