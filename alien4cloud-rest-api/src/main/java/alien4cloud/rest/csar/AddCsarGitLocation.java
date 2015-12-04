package alien4cloud.rest.csar;

import java.util.List;

import lombok.Getter;
import lombok.Setter;

import org.hibernate.validator.constraints.NotBlank;

import alien4cloud.model.git.CsarGitCheckoutLocation;
@Getter
@Setter
public class AddCsarGitLocation {
    @SuppressWarnings("PMD.UnusedPrivateField")
    @NotBlank
    List<CsarGitCheckoutLocation> importLocations;
}