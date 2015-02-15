package alien4cloud.tosca.parser.impl.base;

import org.yaml.snakeyaml.nodes.Node;

import alien4cloud.tosca.parser.IChecker;
import alien4cloud.tosca.parser.ParsingContextExecution;

public class CheckedTypeNodeParser<T> extends TypeNodeParser<T> {

    private IChecker checker;
    
    public CheckedTypeNodeParser(Class<T> type, String toscaType, IChecker checker) {
        super(type, toscaType);
        this.checker = checker;
    }

    @Override
    public T parse(Node node, ParsingContextExecution context) {
        T result = super.parse(node, context);
        checker.check(result, context, node);
        return result;
    }

    @Override
    public T parse(Node node, ParsingContextExecution context, T instance) {
        T result = super.parse(node, context, instance);
        checker.check(result, context, node);
        return result;
    }

}
