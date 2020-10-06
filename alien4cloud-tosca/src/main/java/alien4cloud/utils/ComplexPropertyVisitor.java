package alien4cloud.utils;

import org.alien4cloud.tosca.model.definitions.ComplexPropertyValue;
import org.yaml.snakeyaml.nodes.MappingNode;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.UnaryOperator;

public class ComplexPropertyVisitor {

    private final ComplexPropertyValue property;

    public ComplexPropertyVisitor(ComplexPropertyValue property) {
        this.property = property;
    }

    public void visit(UnaryOperator<Object> operator) {
        recurse(property.getValue(),operator);
    }

    private void recurse(Map<String,Object> map,UnaryOperator<Object> operator) {
        for (Map.Entry<String,Object> e : map.entrySet()) {
            Object ovalue = e.getValue();

            if (ovalue instanceof Map) {
                recurse((Map) ovalue,operator);
            } else if (e.getValue() instanceof List) {
                recurse((List) ovalue,operator);
            }

            Object nvalue = operator.apply(ovalue);
            if (nvalue != ovalue) {
                e.setValue(nvalue);
            }
        }
    }

    private void recurse(List<Object> list,UnaryOperator<Object> operator) {
        for (int i = 0 ; i < list.size(); i ++) {
            Object ovalue = list.get(i);

            if (ovalue instanceof Map) {
                recurse((Map) ovalue,operator);
            } else if (ovalue instanceof List) {
                recurse((List) ovalue,operator);
            }

            Object nvalue = operator.apply(ovalue);
            if (nvalue != ovalue) {
                list.set(i,nvalue);
            }
        }
    }
}
