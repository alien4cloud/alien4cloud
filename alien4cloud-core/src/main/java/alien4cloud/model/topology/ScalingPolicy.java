package alien4cloud.model.topology;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@EqualsAndHashCode
@SuppressWarnings("PMD.UnusedPrivateField")
public class ScalingPolicy {

    private int minInstances;

    private int maxInstances;

    private int initialInstances;

    public static final ScalingPolicy NOT_SCALABLE_POLICY = new ScalingPolicy(1, 1, 1);
}
