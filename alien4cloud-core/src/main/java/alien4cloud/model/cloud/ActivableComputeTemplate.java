package alien4cloud.model.cloud;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@ToString
@SuppressWarnings("PMD.UnusedPrivateField")
public class ActivableComputeTemplate extends ComputeTemplate {

    private boolean enabled = true;

    public ActivableComputeTemplate(String cloudImageId, String cloudImageFlavorId, String description) {
        super(cloudImageId, cloudImageFlavorId, description);
    }

    public ActivableComputeTemplate(String cloudImageId, String cloudImageFlavorId, String description, boolean enabled) {
        super(cloudImageId, cloudImageFlavorId, description);
        this.enabled = enabled;
    }
}
