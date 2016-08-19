package alien4cloud.rest.repository.model;

import lombok.Getter;
import lombok.Setter;
import org.hibernate.validator.constraints.NotBlank;

import javax.validation.constraints.NotNull;

@Getter
@Setter
public class CreateRepositoryRequest {

    @NotBlank
    private String name;

    @NotBlank
    private String pluginId;

    @NotNull
    private Object configuration;
}
