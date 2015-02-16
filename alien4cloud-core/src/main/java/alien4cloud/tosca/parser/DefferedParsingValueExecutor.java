package alien4cloud.tosca.parser;

import org.springframework.beans.BeanWrapper;
import org.yaml.snakeyaml.nodes.Node;

public class DefferedParsingValueExecutor extends AbstractTypeNodeParser implements Runnable, Comparable<DefferedParsingValueExecutor> {
    private final String key;
    private final BeanWrapper target;
    private final ParsingContextExecution context;
    private final MappingTarget mappingTarget;
    private final Node valueNode;
    private int deferredOrder;

    public DefferedParsingValueExecutor(String key, BeanWrapper target, ParsingContextExecution context, MappingTarget mappingTarget, Node valueNode) {
        this(key, target, context, mappingTarget, valueNode, 0);
    }

    public DefferedParsingValueExecutor(String key, BeanWrapper target, ParsingContextExecution context, MappingTarget mappingTarget, Node valueNode,
            int deferredOrder) {
        super("");
        this.key = key;
        this.target = target;
        this.context = context;
        this.mappingTarget = mappingTarget;
        this.valueNode = valueNode;
        this.deferredOrder = deferredOrder;
    }

    public String getKey() {
        return key;
    }

    public int getDeferredOrder() {
        return deferredOrder;
    }

    public void setDeferredOrder(int deferredOrder) {
        this.deferredOrder = deferredOrder;
    }

    @Override
    public int compareTo(DefferedParsingValueExecutor o) {
        return Integer.compare(deferredOrder, o.getDeferredOrder());
    }

    @Override
    public void run() {
        parseAndSetValue(target, key, valueNode, context, mappingTarget);
    }

}