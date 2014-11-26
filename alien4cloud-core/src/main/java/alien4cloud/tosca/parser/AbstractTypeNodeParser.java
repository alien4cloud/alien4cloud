package alien4cloud.tosca.parser;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.beans.NotWritablePropertyException;
import org.yaml.snakeyaml.nodes.Node;

import alien4cloud.tosca.parser.impl.ErrorCode;

/**
 * Abstract class to work with Type Node Parsing.
 */
@Slf4j
@Getter
public abstract class AbstractTypeNodeParser {
    private final String toscaType;

    public AbstractTypeNodeParser(String toscaType) {
        this.toscaType = toscaType;
    }

    protected void parseAndSetValue(BeanWrapper target, String key, Node valueNode, ParsingContextExecution context, MappingTarget mappingTarget) {
        Object value = ((INodeParser<?>) mappingTarget.getParser()).parse(valueNode, context);
        try {
            target.setPropertyValue(mappingTarget.getPath(), value);
        } catch (NotWritablePropertyException e) {
            log.debug("Error while setting property for yaml parsing.", e);
            context.getParsingErrors().add(
                    new ParsingError(ParsingErrorLevel.WARNING, ErrorCode.ALIEN_MAPPING_ERROR, "Invalid definition for type", valueNode.getStartMark(), "",
                            valueNode.getEndMark(), toscaType));
        }

        if (mappingTarget instanceof KeyValueMappingTarget) {
            KeyValueMappingTarget kvmt = (KeyValueMappingTarget) mappingTarget;
            BeanWrapper keyBeanWrapper = target;
            if (kvmt.isKeyPathRelativeToValue()) {
                keyBeanWrapper = new BeanWrapperImpl(value);
            }
            try {
                if (!(keyBeanWrapper.getPropertyValue(kvmt.getKeyPath()) != null && mappingTarget.getPath().equals(key))) {
                    keyBeanWrapper.setPropertyValue(kvmt.getKeyPath(), key);
                }
            } catch (NotWritablePropertyException e) {
                log.debug("Error while setting key to property for yaml parsing.", e);
                context.getParsingErrors().add(
                        new ParsingError(ParsingErrorLevel.WARNING, ErrorCode.ALIEN_MAPPING_ERROR, "Invalid definition for type", valueNode.getStartMark(), "",
                                valueNode.getEndMark(), toscaType));
            }
        }
    }
}