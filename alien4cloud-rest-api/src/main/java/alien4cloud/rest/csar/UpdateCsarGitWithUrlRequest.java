package alien4cloud.rest.csar;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import org.hibernate.validator.constraints.NotBlank;
@Getter
@Setter
@Data
@ToString
@AllArgsConstructor
@NoArgsConstructor
public class UpdateCsarGitWithUrlRequest {

    @SuppressWarnings("PMD.UnusedPrivateField")
    @NotBlank
    private String repositoryUrlToUpdate;
    @NotBlank
    private String repositoryUrl;
    @NotBlank
    private String username;
    @NotBlank
    private String password;

}
