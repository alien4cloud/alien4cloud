package alien4cloud.model.cloud;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@EqualsAndHashCode
@SuppressWarnings("PMD.UnusedPrivateField")
public class CloudImageRequirement {

    private Integer numCPUs;

    private Long diskSize;

    private Long memSize;
}
