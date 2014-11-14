package alien4cloud.tosca.parser;

import org.springframework.beans.BeanWrapper;
import org.yaml.snakeyaml.nodes.Node;

public class DefferedParsingValueExecutor extends AbstractTypeNodeParser implements Runnable {
    private final String key;
    private final BeanWrapper target;
    private final ParsingContextExecution context;
    private final MappingTarget mappingTarget;
    private final Node valueNode;

    public DefferedParsingValueExecutor(String key, BeanWrapper target, ParsingContextExecution context, MappingTarget mappingTarget, Node valueNode) {
        super("");
        this.key = key;
        this.target = target;
        this.context = context;
        this.mappingTarget = mappingTarget;
        this.valueNode = valueNode;
    }

    @Override
    public void run() {
        parseAndSetValue(target, key, valueNode, context, mappingTarget);
    }
}