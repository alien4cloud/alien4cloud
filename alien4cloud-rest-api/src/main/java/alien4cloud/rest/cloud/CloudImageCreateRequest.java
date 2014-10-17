package alien4cloud.rest.cloud;

import lombok.Getter;
import lombok.Setter;

import org.hibernate.validator.constraints.NotBlank;

@Getter
@Setter
@SuppressWarnings("PMD.UnusedPrivateField")
public class CloudImageCreateRequest {

    @NotBlank
    private String name;

    /**
     * The architect x64 or x86
     */
    @NotBlank
    private String osArch;

    /**
     * OS type
     */
    @NotBlank
    private String osType;

    /**
     * OS Distribution
     */
    @NotBlank
    private String osDistribution;

    /**
     * OS Version
     */
    @NotBlank
    private String osVersion;

    /**
     * Number of CPUs
     */
    private Integer numCPUs;

    /**
     * Size of disk
     */
    private Long diskSize;

    /**
     * Size of memory
     */
    private Long memSize;
}
