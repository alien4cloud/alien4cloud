package alien4cloud.model.topology;

import lombok.*;

@AllArgsConstructor(suppressConstructorProperties = true)
@NoArgsConstructor
@Getter
@Setter
@EqualsAndHashCode
public class ScalingPolicy {

    private int minInstances;

    private int maxInstances;

    private int initialInstances;

    public static final ScalingPolicy NOT_SCALABLE_POLICY = new ScalingPolicy(1, 1, 1);
}
