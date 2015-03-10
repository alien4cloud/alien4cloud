package alien4cloud.model.cloud;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@EqualsAndHashCode(callSuper = true, exclude = { "description" })
@NoArgsConstructor
@ToString
@SuppressWarnings("PMD.UnusedPrivateField")
public class ActivableComputeTemplate extends ComputeTemplate {

    private boolean enabled = true;

    private String description;

    public ActivableComputeTemplate(String cloudImageId, String cloudImageFlavorId, String description) {
        super(cloudImageId, cloudImageFlavorId);
        this.description = description;
    }

    public ActivableComputeTemplate(String cloudImageId, String cloudImageFlavorId, String description, boolean enabled) {
        super(cloudImageId, cloudImageFlavorId);
        this.enabled = enabled;
        this.description = description;
    }
}
