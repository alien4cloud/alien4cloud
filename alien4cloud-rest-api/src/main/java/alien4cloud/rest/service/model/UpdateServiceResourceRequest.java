package alien4cloud.rest.service.model;

import io.swagger.annotations.ApiModel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Update request for a service.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor(suppressConstructorProperties = true)
@ApiModel(value = "Service update request.", description = "A request object to pass when updating a service. Contains updatable fields.")
public class UpdateServiceResourceRequest {
    private String name;
    private String version;
    private String description;
}