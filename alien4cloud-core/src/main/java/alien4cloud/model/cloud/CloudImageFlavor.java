package alien4cloud.model.cloud;

import javax.validation.constraints.Min;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import org.hibernate.validator.constraints.NotBlank;

@Getter
@Setter
@EqualsAndHashCode(of = "id")
@SuppressWarnings("PMD.UnusedPrivateField")
public class CloudImageFlavor {

    @NotBlank
    private String id;

    /**
     * Number of CPUs
     */
    @NotBlank
    @Min(1)
    private int numCPUs;

    /**
     * Disk size
     */
    @NotBlank
    @Min(1)
    private long diskSize;

    /**
     * Memory size
     */
    @NotBlank
    @Min(1)
    private long memSize;
}
