package alien4cloud.rest.csar;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import org.hibernate.validator.constraints.NotBlank;

import alien4cloud.security.model.CsarGitCheckoutLocation;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateCsarGithubRequest {
    @SuppressWarnings("PMD.UnusedPrivateField")
    @NotBlank
    private String repositoryUrl;

    @NotBlank
    private String username;

    @NotBlank
    private String password;

    List<CsarGitCheckoutLocation> importLocations;
}