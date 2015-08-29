package alien4cloud.rest.csar;

import lombok.Getter;
import lombok.Setter;

import org.hibernate.validator.constraints.NotBlank;

@Getter
@Setter
@SuppressWarnings("PMD.UnusedPrivateField")
public class SpecifyCsarFromGit {

    @NotBlank
    private String repositoryUrl;
    @NotBlank
    private String branchId;
    @NotBlank
    private String target;
    @NotBlank
    private String localDirectory;
}
