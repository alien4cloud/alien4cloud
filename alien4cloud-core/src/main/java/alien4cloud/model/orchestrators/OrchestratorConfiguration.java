package alien4cloud.model.orchestrators;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.elasticsearch.annotation.ESObject;
import org.elasticsearch.annotation.Id;

/**
 * Global configuration for the orchestrators. Usually contains connexion settings etc.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@SuppressWarnings("PMD.UnusedPrivateField")
@ESObject
public class OrchestratorConfiguration {
    /** Id of the cloud. */
    @Id
    private String id;
    /** Configuration object. */
    private Object configuration;
}