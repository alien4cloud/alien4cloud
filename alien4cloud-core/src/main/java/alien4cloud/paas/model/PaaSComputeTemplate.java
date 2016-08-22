package alien4cloud.paas.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor(suppressConstructorProperties = true)
@NoArgsConstructor
public class PaaSComputeTemplate {

    private String imageId;

    private String flavorId;

    private String description;
}
