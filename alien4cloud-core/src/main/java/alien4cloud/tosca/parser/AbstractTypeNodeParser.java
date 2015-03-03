package alien4cloud.tosca.parser;

import java.util.Map;
import java.util.Map.Entry;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import org.apache.commons.collections4.keyvalue.DefaultMapEntry;
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
        // let's store the parent in the context for future use
        context.setParent(target.getWrappedInstance());

        Entry<BeanWrapper, String> entry = findWrapperPropertyByPath(context.getRoot(), target, mappingTarget.getPath());
        BeanWrapper realTarget = entry.getKey();
        String propertyName = entry.getValue();

        Object value = ((INodeParser<?>) mappingTarget.getParser()).parse(valueNode, context);
        if (!propertyName.equals("void")) {
            // property named 'void' means : process the parsing but do not set anything
            try {
                realTarget.setPropertyValue(propertyName, value);
            } catch (NotWritablePropertyException e) {
                log.warn("Error while setting property for yaml parsing.", e);
                context.getParsingErrors().add(
                        new ParsingError(ParsingErrorLevel.WARNING, ErrorCode.ALIEN_MAPPING_ERROR, "Invalid definition for type", valueNode.getStartMark(), "",
                                valueNode.getEndMark(), toscaType));
            }
        }

        if (mappingTarget instanceof KeyValueMappingTarget) {
            KeyValueMappingTarget kvmt = (KeyValueMappingTarget) mappingTarget;
            BeanWrapper keyBeanWrapper = realTarget;
            try {
                if (!(keyBeanWrapper.getPropertyValue(kvmt.getKeyPath()) != null && propertyName.equals(key))) {
                    keyBeanWrapper.setPropertyValue(kvmt.getKeyPath(), key);
                }
            } catch (NotWritablePropertyException e) {
                log.warn("Error while setting key to property for yaml parsing.", e);
                context.getParsingErrors().add(
                        new ParsingError(ParsingErrorLevel.WARNING, ErrorCode.ALIEN_MAPPING_ERROR, "Invalid definition for type", valueNode.getStartMark(), "",
                                valueNode.getEndMark(), toscaType));
            }
        }
    }

    /**
     * For example:
     * <ul>
     * <li>.something : the value will be set to the property of root named 'something'
     * <li>child1.child2.prop : the value will be mapped u getChild1().getChild2().setProp()
     * </ul>
     */
    private Entry<BeanWrapper, String> findWrapperPropertyByPath(BeanWrapper root, BeanWrapper current, String path) {
        int dotIdx = path.indexOf(".");
        if (dotIdx < 0) {
            return new DefaultMapEntry<BeanWrapper, String>(current, path);
        }
        BeanWrapper base = current;
        String nextPath = path;
        if (path.startsWith(".")) {
            base = root;
            nextPath = path.substring(1);
        } else {
            String wrapperCandidateName = path.substring(0, path.indexOf("."));
            Object wrapperCandidate = current.getPropertyValue(wrapperCandidateName);
            base = new BeanWrapperImpl(wrapperCandidate);
            nextPath = path.substring(path.indexOf(".") + 1);
        }
        return findWrapperPropertyByPath(root, base, nextPath);
    }

}
