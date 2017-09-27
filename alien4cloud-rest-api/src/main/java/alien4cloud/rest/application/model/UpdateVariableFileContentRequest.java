package alien4cloud.rest.application.model;

import lombok.Getter;
import lombok.Setter;

/**
 * DTO object for ui update of the application variable file content.
 */
@Getter
@Setter
public class UpdateVariableFileContentRequest {
    String content;
}
