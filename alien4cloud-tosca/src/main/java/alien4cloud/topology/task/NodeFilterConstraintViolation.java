package alien4cloud.topology.task;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import alien4cloud.rest.model.RestErrorCode;
import alien4cloud.tosca.properties.constraints.ConstraintUtil;

@AllArgsConstructor(suppressConstructorProperties = true)
@NoArgsConstructor
@Getter
@Setter
public class NodeFilterConstraintViolation {
    private RestErrorCode errorCode;
    private String message;
    private ConstraintUtil.ConstraintInformation constraintInformation;
}
