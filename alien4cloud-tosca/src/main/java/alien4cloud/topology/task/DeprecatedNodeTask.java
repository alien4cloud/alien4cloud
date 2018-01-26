package alien4cloud.topology.task;

import org.alien4cloud.tosca.model.types.NodeType;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class DeprecatedNodeTask extends AbstractTask {
    private String name;
    private String type;
    private String version;

    public DeprecatedNodeTask(String templateName, NodeType nodeType) {
        super.setCode(TaskCode.DEPRECATED_NODE);
        this.name = templateName;
        this.type = nodeType.getElementId();
        this.version = nodeType.getArchiveVersion();
    }
}