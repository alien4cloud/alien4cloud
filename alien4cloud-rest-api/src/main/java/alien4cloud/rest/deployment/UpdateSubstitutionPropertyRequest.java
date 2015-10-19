package alien4cloud.rest.deployment;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class UpdateSubstitutionPropertyRequest {
    private String propertyName;
    private Object propertyValue;
}
