package alien4cloud.tosca.parser;

import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;
import org.yaml.snakeyaml.nodes.Node;

/**
 * Abstract class to work with Type Node Parsing.
 */
public abstract class AbstractTypeNodeParser {

    protected void parseAndSetValue(BeanWrapper target, String key, Node valueNode, ParsingContextExecution context, MappingTarget mappingTarget) {
        Object value = ((INodeParser<?>) mappingTarget.getParser()).parse(valueNode, context);
        target.setPropertyValue(mappingTarget.getPath(), value);

        if (mappingTarget instanceof KeyValueMappingTarget) {
            KeyValueMappingTarget kvmt = (KeyValueMappingTarget) mappingTarget;
            BeanWrapper keyBeanWrapper = target;
            if (kvmt.isKeyPathRelativeToValue()) {
                keyBeanWrapper = new BeanWrapperImpl(value);
            }
            keyBeanWrapper.setPropertyValue(kvmt.getKeyPath(), key);
        }
    }
}