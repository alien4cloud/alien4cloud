package alien4cloud.model.orchestrators;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.elasticsearch.annotation.ESObject;
import org.elasticsearch.annotation.Id;
import org.elasticsearch.annotation.ObjectField;

/**
 * Global configuration for the orchestrator. Usually contains connexion settings etc.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor(suppressConstructorProperties = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@ESObject
public class OrchestratorConfiguration {
    /** Id of the orchestrator. */
    @Id
    private String id;
    /** Configuration object. */
    @ObjectField(enabled = false)
    private Object configuration;
}