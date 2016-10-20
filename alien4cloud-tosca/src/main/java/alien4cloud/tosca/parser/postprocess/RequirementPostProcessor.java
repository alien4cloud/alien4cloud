package alien4cloud.tosca.parser.postprocess;

import java.util.Map;

import javax.annotation.Resource;

import org.alien4cloud.tosca.model.templates.Requirement;
import org.alien4cloud.tosca.model.types.CapabilityType;
import org.springframework.stereotype.Component;

import alien4cloud.tosca.context.ToscaContext;
import alien4cloud.tosca.parser.ParsingContextExecution;

/**
 * Post processor that performs validation of references in a tosca template.
 */
@Component
public class RequirementPostProcessor implements IPostProcessor<Map.Entry<String, Requirement>> {
    @Resource
    private CapabilityOrNodeReferencePostProcessor capabilityOrNodeReferencePostProcessor;
    @Resource
    private CapabilityReferencePostProcessor capabilityReferencePostProcessor;
    @Resource
    private PropertyValueChecker propertyValueChecker;

    @Override
    public void process(Map.Entry<String, Requirement> instance) {
        String definitionVersion = ParsingContextExecution.getDefinitionVersion();
        // Note both post processors below are optional post processors and applied based on DSL version.
        // In previous alien DSL we authorized a dependency on a node type and not just capability type.
        // TODO Handle multiple version post processor
        switch (definitionVersion) {
        case "tosca_simple_yaml_1_0_0_wd03":
        case "alien_dsl_1_1_0":
        case "alien_dsl_1_2_0":
            capabilityOrNodeReferencePostProcessor.process(new ReferencePostProcessor.TypeReference(instance.getValue().getType()));
            break;
        default:
            // In latest versions we process the capability only.
            capabilityReferencePostProcessor.process(new ReferencePostProcessor.TypeReference(instance.getValue().getType()));
            break;
        }
        CapabilityType capabilityType = ToscaContext.get(CapabilityType.class, instance.getValue().getType());
        propertyValueChecker.checkProperties(capabilityType, instance.getValue().getProperties(), instance.getKey());
    }
}