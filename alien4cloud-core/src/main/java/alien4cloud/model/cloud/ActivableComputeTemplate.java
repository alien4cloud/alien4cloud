package alien4cloud.model.cloud;

import java.util.UUID;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@EqualsAndHashCode(callSuper = true, exclude = "id")
@NoArgsConstructor
@ToString
@SuppressWarnings("PMD.UnusedPrivateField")
public class ActivableComputeTemplate extends ComputeTemplate {
    private String id;

    private boolean enabled;

    public ActivableComputeTemplate(String cloudImageId, String cloudImageFlavorId, String description) {
        this(cloudImageId, cloudImageFlavorId, description, true);
    }

    public ActivableComputeTemplate(String cloudImageId, String cloudImageFlavorId, String description, boolean enabled) {
        super(cloudImageId, cloudImageFlavorId, description);
        this.enabled = enabled;
        this.id = UUID.randomUUID().toString();
    }
}
