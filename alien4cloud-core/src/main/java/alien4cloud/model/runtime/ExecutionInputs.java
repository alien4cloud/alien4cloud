package alien4cloud.model.runtime;

import alien4cloud.json.deserializer.PropertyValueDeserializer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.collect.Maps;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.alien4cloud.tosca.model.definitions.AbstractPropertyValue;
import org.elasticsearch.annotation.*;
import org.elasticsearch.annotation.query.TermFilter;
import org.elasticsearch.mapping.IndexType;

import java.util.Date;
import java.util.Map;

@Getter
@Setter
@ESObject
@NoArgsConstructor
public class ExecutionInputs {

    /** Unique id of the execution, provided by the orchestrator. */
    @Id
    private String id;

    /** The name of the workflow in A4C (can be different than the ID). **/
    @TermFilter
    @StringField(indexType = IndexType.not_analyzed)
    private String workflowName;

    /** Start date of the execution */
    @TimeStamp(format = "", index = IndexType.not_analyzed)
    private Date timestamp;

    @TermFilter
    @StringField(includeInAll = false, indexType = IndexType.not_analyzed)
    private String environmentId;

    @ObjectField(enabled = false)
    @JsonDeserialize(contentUsing = PropertyValueDeserializer.class)
    private Map<String, AbstractPropertyValue> inputs = Maps.newHashMap();

}
