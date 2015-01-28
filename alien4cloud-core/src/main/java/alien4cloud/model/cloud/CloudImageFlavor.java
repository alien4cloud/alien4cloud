package alien4cloud.model.cloud;

import javax.validation.constraints.Min;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import org.hibernate.validator.constraints.NotBlank;

@Getter
@Setter
@EqualsAndHashCode
@ToString
@SuppressWarnings("PMD.UnusedPrivateField")
public class CloudImageFlavor implements Comparable<CloudImageFlavor>, ICloudResourceTemplate {

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

    @Override
    public int compareTo(CloudImageFlavor right) {
        int thisPoint = 0;
        int rightPoint = 0;
        if (this.getNumCPUs() > right.getNumCPUs()) {
            thisPoint++;
        } else if (right.getNumCPUs() > this.getNumCPUs()) {
            rightPoint++;
        }
        if (this.getDiskSize() > right.getDiskSize()) {
            thisPoint++;
        } else if (right.getDiskSize() > this.getDiskSize()) {
            rightPoint++;
        }
        if (this.getMemSize() > right.getMemSize()) {
            thisPoint++;
        } else if (right.getMemSize() > this.getMemSize()) {
            rightPoint++;
        }
        if (thisPoint > rightPoint) {
            return 1;
        } else if (rightPoint > thisPoint) {
            return -1;
        } else {
            return 0;
        }
    }
}
