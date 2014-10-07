package alien4cloud.model.cloud;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@SuppressWarnings("PMD.UnusedPrivateField")
public class CloudImageRequirement {

    private Integer numCPUs;

    private Long diskSize;

    private Long memSize;
}
