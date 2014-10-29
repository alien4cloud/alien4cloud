package alien4cloud.tosca.parser;

import lombok.AllArgsConstructor;

import org.springframework.beans.BeanWrapper;
import org.yaml.snakeyaml.nodes.Node;

@AllArgsConstructor
public class DefferedParsingValueExecutor extends AbstractTypeNodeParser implements Runnable {
    private final BeanWrapper target;
    private final ParsingContext context;
    private final String path;
    private final IParser parser;
    private final Node valueNode;

    @Override
    public void run() {
        parseAndSetValue(target, valueNode, context, path, parser);
    }
}