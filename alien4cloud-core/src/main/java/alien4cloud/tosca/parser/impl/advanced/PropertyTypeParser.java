package alien4cloud.tosca.parser.impl.advanced;

import javax.annotation.Resource;

import org.springframework.stereotype.Component;
import org.yaml.snakeyaml.nodes.Node;

import alien4cloud.component.ICSARRepositorySearchService;
import alien4cloud.model.components.IndexedDataType;
import alien4cloud.tosca.model.ArchiveRoot;
import alien4cloud.tosca.normative.ToscaType;
import alien4cloud.tosca.parser.ParsingContextExecution;
import alien4cloud.tosca.parser.ParsingError;
import alien4cloud.tosca.parser.impl.ErrorCode;
import alien4cloud.tosca.parser.impl.base.ScalarParser;
import alien4cloud.tosca.parser.mapping.DefaultDeferredParser;

@Component
public class PropertyTypeParser extends DefaultDeferredParser<String> {

    @Resource
    private ICSARRepositorySearchService searchService;
    @Resource
    private ScalarParser scalarParser;

    @Override
    public String parse(Node node, ParsingContextExecution context) {
        String propertyType = scalarParser.parse(node, context);
        if (ToscaType.fromYamlTypeName(propertyType) == null) {
            // It's not a primitive type
            ArchiveRoot archiveRoot = (ArchiveRoot) context.getRoot().getWrappedInstance();
            if (!archiveRoot.getDataTypes().containsKey(propertyType)) {
                if (!searchService.isElementExistInDependencies(IndexedDataType.class, propertyType, archiveRoot.getArchive().getDependencies())) {
                    context.getParsingErrors().add(
                            new ParsingError(ErrorCode.VALIDATION_ERROR, "ToscaPropertyType", node.getStartMark(), "Type " + propertyType
                                    + " is not valid for the property definition", node.getEndMark(), "type"));
                }
            }
        }
        return propertyType;
    }

    @Override
    public int getDeferredOrder(ParsingContextExecution context) {
        return 1;
    }
}
