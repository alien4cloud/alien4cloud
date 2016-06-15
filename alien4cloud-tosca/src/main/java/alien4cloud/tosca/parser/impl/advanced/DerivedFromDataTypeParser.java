package alien4cloud.tosca.parser.impl.advanced;

import java.util.List;

import org.springframework.stereotype.Component;
import org.yaml.snakeyaml.nodes.Node;

import alien4cloud.model.components.IndexedDataType;
import alien4cloud.tosca.normative.ToscaType;
import alien4cloud.tosca.parser.ParsingContextExecution;

import com.google.common.collect.Lists;

@Component
public class DerivedFromDataTypeParser extends DerivedFromParser {
    public DerivedFromDataTypeParser() {
        super(IndexedDataType.class);
    }

    @Override
    public List<String> parse(Node node, ParsingContextExecution context) {
        String valueAsString = scalarParser.parse(node, context);
        if (valueAsString == null || valueAsString.isEmpty()) {
            return null;
        }
        if (ToscaType.isSimple(valueAsString)) {
            List<String> derivedFrom = Lists.newArrayList();
            derivedFrom.add(0, valueAsString);
            return derivedFrom;
        } else {
            return super.parse(node, context);
        }
    }

}
