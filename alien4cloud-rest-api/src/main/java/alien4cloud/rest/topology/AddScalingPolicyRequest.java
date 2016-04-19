package alien4cloud.rest.topology;

import alien4cloud.model.topology.ScalingPolicy;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AddScalingPolicyRequest {

    private String nodeTemplateId;

    private ScalingPolicy scalingPolicy;
}
