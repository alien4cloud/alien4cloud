package alien4cloud.topology.task;

import alien4cloud.rest.model.RestErrorCode;
import alien4cloud.tosca.properties.constraints.ConstraintUtil;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class NodeFilterConstraintViolation {
    private RestErrorCode errorCode;
    private ConstraintUtil.ConstraintInformation constraintInformation;
}
