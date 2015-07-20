package alien4cloud.rest.csar;

import java.io.Serializable;

import lombok.Getter;
import lombok.Setter;

import org.hibernate.validator.constraints.NotBlank;

@Getter
@Setter
@SuppressWarnings("PMD.UnusedPrivateField")
public class  CreateCsarGitCheckoutLocation {
    @NotBlank
    private String branchId;
    @NotBlank
    private String subPath;
}
