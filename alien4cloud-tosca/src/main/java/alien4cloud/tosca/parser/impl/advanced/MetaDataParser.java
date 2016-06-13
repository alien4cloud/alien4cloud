package alien4cloud.tosca.parser.impl.advanced;

import alien4cloud.model.common.Tag;
import alien4cloud.model.components.Csar;
import alien4cloud.tosca.model.ArchiveRoot;
import alien4cloud.tosca.parser.ParserUtils;
import alien4cloud.tosca.parser.ParsingContextExecution;
import alien4cloud.tosca.parser.mapping.DefaultParser;
import alien4cloud.utils.VersionUtil;
import com.google.common.collect.Lists;
import org.springframework.stereotype.Component;
import org.yaml.snakeyaml.nodes.MappingNode;
import org.yaml.snakeyaml.nodes.Node;
import org.yaml.snakeyaml.nodes.NodeTuple;

import java.util.List;

/**
 * Specific parser to enrich the Csar archive object with meta-data
 */
@Component
public class MetaDataParser extends DefaultParser<Csar> {
    private static final String TEMPLATE_NAME = "template_name";
    private static final String TEMPLATE_AUTHOR = "template_author";
    private static final String TEMPLATE_VERSION = "template_version";

    @Override
    public Csar parse(Node node, ParsingContextExecution context) {
        ArchiveRoot parent = (ArchiveRoot) context.getParent();
        Csar csar = parent.getArchive();

        List<Tag> tagList = Lists.newArrayList();
        if (node instanceof MappingNode) {
            MappingNode mapNode = (MappingNode) node;
            for (NodeTuple entry : mapNode.getValue()) {
                String key = ParserUtils.getScalar(entry.getKeyNode(), context);
                String value = ParserUtils.getScalar(entry.getValueNode(), context);
                if (TEMPLATE_NAME.equals(key)) {
                    csar.setName(value);
                } else if (TEMPLATE_AUTHOR.equals(key)) {
                    csar.setTemplateAuthor(value);
                } else if (TEMPLATE_VERSION.equals(key)) {
                    csar.setVersion(value);
                    if (!VersionUtil.isValid(value)) {
                        ParserUtils.addTypeError(entry.getValueNode(), context.getParsingErrors(), "version");
                    }
                } else if (value != null) {
                    tagList.add(new Tag(key, value));
                }
            }
            csar.setTags(tagList);
        } else {
            ParserUtils.addTypeError(node, context.getParsingErrors(), "meta-data");
        }

        return csar;
    }
}
