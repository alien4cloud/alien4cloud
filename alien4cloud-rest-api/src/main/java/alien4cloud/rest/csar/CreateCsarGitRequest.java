package alien4cloud.rest.csar;

import java.util.List;

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
    @NotBlank
    private String repositoryUrl;

    private String username;

    private String password;
    
    //private boolean saveCredentials;
    
    List<CsarGitCheckoutLocation> importLocations;
}