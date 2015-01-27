package alien4cloud.rest.cloud;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@SuppressWarnings("PMD.UnusedPrivateField")
public class CloudImageUpdateRequest {

    private String name;

    /**
     * The architect x64 or x86
     */
    private String osArch;

    /**
     * OS type
     */
    private String osType;

    /**
     * OS Distribution
     */
    private String osDistribution;

    /**
     * OS Version
     */
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
