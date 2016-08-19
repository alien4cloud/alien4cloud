package alien4cloud.rest.csar;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
@AllArgsConstructor(suppressConstructorProperties = true)
@NoArgsConstructor
public class CreateCsarRequest {

    private String name;
    private String version;
    private String description;

}
