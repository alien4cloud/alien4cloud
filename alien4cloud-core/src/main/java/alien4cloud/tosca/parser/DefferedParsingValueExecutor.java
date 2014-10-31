package alien4cloud.tosca.parser;

import lombok.AllArgsConstructor;

import org.springframework.beans.BeanWrapper;
import org.yaml.snakeyaml.nodes.Node;

@AllArgsConstructor
public class DefferedParsingValueExecutor extends AbstractTypeNodeParser implements Runnable {
    private final String key;
    private final BeanWrapper target;
    private final ParsingContext context;
    private final MappingTarget mappingTarget;
    private final Node valueNode;

    @Override
    public void run() {
        parseAndSetValue(target, key, valueNode, context, mappingTarget);
    }
}