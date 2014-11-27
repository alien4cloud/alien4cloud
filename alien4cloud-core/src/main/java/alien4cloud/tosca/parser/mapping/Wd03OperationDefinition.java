package alien4cloud.tosca.parser.mapping;

import java.util.Map;

import javax.annotation.Resource;

import org.springframework.stereotype.Component;

import alien4cloud.tosca.model.FunctionPropertyValue;
import alien4cloud.tosca.model.IOperationParameter;
import alien4cloud.tosca.model.Operation;
import alien4cloud.tosca.model.ScalarPropertyValue;
import alien4cloud.tosca.parser.INodeParser;
import alien4cloud.tosca.parser.KeyValueMappingTarget;
import alien4cloud.tosca.parser.MappingTarget;
import alien4cloud.tosca.parser.impl.advanced.ImplementationArtifactParser;
import alien4cloud.tosca.parser.impl.base.KeyDiscriminatorParser;
import alien4cloud.tosca.parser.impl.base.ListParser;
import alien4cloud.tosca.parser.impl.base.MapParser;
import alien4cloud.tosca.parser.impl.base.TypeNodeParser;

import com.google.common.collect.Maps;

@Component
public class Wd03OperationDefinition extends AbstractMapper<Operation> {
    @Resource
    private ImplementationArtifactParser implementationArtifactParser;
    @Resource
    private Wd03PropertyDefinition propertyDefinition;

    public Wd03OperationDefinition() {
        super(new TypeNodeParser<Operation>(Operation.class, "Capability definition"));
    }

    @Override
    public void initMapping() {
        instance.getYamlToObjectMapping().put("implementation", new MappingTarget("implementationArtifact", implementationArtifactParser));
        quickMap("description");

        Map<String, INodeParser<? extends IOperationParameter>> discriminationMap = Maps.newHashMap();

        // key value mapping target for operation parameters
        TypeNodeParser<FunctionPropertyValue> functionParser = new TypeNodeParser<>(FunctionPropertyValue.class, "Property function");
        // String keyPath, boolean keyPathRelativeToValue, String path, INodeParser<?> parser
        functionParser.getYamlOrderedToObjectMapping().put(0,
                new KeyValueMappingTarget("function", "parameters", new ListParser<String>(getScalarParser(), "function parameter")));

        discriminationMap.put("type", propertyDefinition.getParser());
        discriminationMap.put("get_input", functionParser);
        discriminationMap.put("get_property", functionParser);
        discriminationMap.put("get_attribute", functionParser);
        discriminationMap.put("get_operation_output", functionParser);

        TypeNodeParser<? extends IOperationParameter> propertyValueParser = new TypeNodeParser<ScalarPropertyValue>(ScalarPropertyValue.class,
                "Parameter definition");

        KeyDiscriminatorParser<? extends IOperationParameter> discriminatorParser = new KeyDiscriminatorParser(discriminationMap, propertyValueParser);
        MapParser<? extends IOperationParameter> parametersParser = new MapParser(discriminatorParser, "Parameter definitions");

        // support both as TOSCA wd03 is not consistent.
        instance.getYamlToObjectMapping().put("input", new MappingTarget("inputParameters", parametersParser));
        instance.getYamlToObjectMapping().put("inputs", new MappingTarget("inputParameters", parametersParser));
    }
}