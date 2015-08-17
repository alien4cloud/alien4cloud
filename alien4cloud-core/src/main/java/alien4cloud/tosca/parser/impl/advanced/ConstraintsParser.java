package alien4cloud.tosca.parser.impl.advanced;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import alien4cloud.model.components.PropertyConstraint;
import alien4cloud.tosca.parser.ParsingContextExecution;
import alien4cloud.tosca.parser.impl.base.ListParser;

@Component
public class ConstraintsParser extends ListParser<PropertyConstraint> {

    @Autowired
    public ConstraintsParser(ConstraintParser valueParser) {
        super(valueParser, "sequence of constraints");
    }

    @Override
    public boolean isDeferred(ParsingContextExecution context) {
        return true;
    }

    @Override
    public int getDefferedOrder(ParsingContextExecution context) {
        return 3;
    }
}
