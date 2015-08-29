package alien4cloud.rest.csar;

import java.util.List;

import javax.validation.constraints.NotNull;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import org.hibernate.validator.constraints.NotBlank;

import alien4cloud.security.model.CsarGitCheckoutLocation;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateCsarGitRequest {
    @SuppressWarnings("PMD.UnusedPrivateField")
    @NotNull
    private String repositoryUrl;
   
    private String username;

    private String password;

    private boolean storedLocally;
    @NotNull
    private List<CsarGitCheckoutLocation> importLocations;
}