package alien4cloud.paas.wf.SUPALIEN_1202;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.Setter;
import org.alien4cloud.tosca.model.workflow.Workflow;

import java.util.Map;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class ParsedTopologySource {

    private Map<String, Workflow> unprocessedWorkflows;
    private Map<String, Workflow> workflows;

}
