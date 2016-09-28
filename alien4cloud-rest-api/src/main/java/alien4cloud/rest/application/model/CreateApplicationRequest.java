package alien4cloud.rest.application.model;

import javax.validation.constraints.NotNull;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * DTO object to create a new application.
 * 
 * @author luc boutier
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor(suppressConstructorProperties = true)
public class CreateApplicationRequest {
    /** Name of the archive that is related to the application. This is also the application id. */
    @NotNull
    private String archiveName;
    @NotNull
    private String name;
    private String description;
    private String topologyTemplateVersionId;
}