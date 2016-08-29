package alien4cloud.tosca.parser.impl.base;

import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.yaml.snakeyaml.nodes.Node;

import alien4cloud.tosca.parser.INodeParser;
import alien4cloud.tosca.parser.ParsingContextExecution;
import alien4cloud.tosca.parser.ParsingError;
import alien4cloud.tosca.parser.impl.ErrorCode;
import lombok.extern.slf4j.Slf4j;

/**
 * Parser implementation that delegates parsing to a parser referenced in the parser registry based on the type key.
 */
@Slf4j
@Component
@Scope(value = ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class ReferencedParser<T> implements INodeParser<T> {
    private String typeName;

    public ReferencedParser(String typeName) {
        this.typeName = typeName;
    }

    @Override
    public T parse(Node node, ParsingContextExecution context) {
        INodeParser<?> delegate = context.getRegistry().get(typeName);
        if (delegate == null) {
            log.error("No parser found for yaml type {}", typeName);
            context.getParsingErrors().add(
                    new ParsingError(ErrorCode.ALIEN_MAPPING_ERROR, "No parser found for yaml type", node.getStartMark(), "", node.getEndMark(), typeName));
            return null;
        }
        return (T) delegate.parse(node, context);
    }

    private INodeParser<?> getParser(ParsingContextExecution context) {
        INodeParser<?> innerNodeParser = context.getRegistry().get(typeName);
        if (innerNodeParser == null) {
            context.getParsingErrors().add(new ParsingError(ErrorCode.ALIEN_MAPPING_ERROR, "No parser found for yaml type", null, "", null, typeName));
        }
        return innerNodeParser;
    }
}
