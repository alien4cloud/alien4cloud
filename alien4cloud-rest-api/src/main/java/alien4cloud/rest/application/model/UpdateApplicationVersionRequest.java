package alien4cloud.rest.application.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Created by lucboutier on 16/01/2017.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UpdateApplicationVersionRequest {
    private String version;
    private String description;
}