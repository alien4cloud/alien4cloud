package alien4cloud.model.cloud;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import org.elasticsearch.annotation.ESObject;
import org.elasticsearch.annotation.Id;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

/**
 * Specific configuration of an IPaaSProvider for a given Cloud.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(Include.NON_NULL)
@SuppressWarnings("PMD.UnusedPrivateField")
@ESObject
public class CloudConfiguration {
    /** Id of the cloud. */
    @Id
    private String id;
    /** Configuration of the paas provider bean. */
    private Object configuration;
}
