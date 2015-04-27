package alien4cloud.model.cloud;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * This represents a compute template with a defined HA policy
 */
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@ToString(callSuper = true)
public class HighAvailabilityComputeTemplate extends ComputeTemplate {

    /**
     * This is the Alien technical availability zone id stored in a Cloud and not IaaS availability zone id
     */
    private String availabilityZoneId;

    public HighAvailabilityComputeTemplate(String cloudImageId, String cloudImageFlavorId, String description, String availabilityZoneId) {
        super(cloudImageId, cloudImageFlavorId, description);
        this.availabilityZoneId = availabilityZoneId;
    }
}
