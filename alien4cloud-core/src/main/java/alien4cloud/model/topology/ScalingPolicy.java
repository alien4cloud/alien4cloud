package alien4cloud.model.topology;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@SuppressWarnings("PMD.UnusedPrivateField")
public class ScalingPolicy {

    private int minInstances;

    private int maxInstances;

    private int initialInstances;

}
