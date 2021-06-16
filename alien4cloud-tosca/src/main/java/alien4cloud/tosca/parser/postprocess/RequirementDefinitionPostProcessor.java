package alien4cloud.tosca.parser.postprocess;

import javax.annotation.Resource;

import alien4cloud.tosca.parser.ParsingErrorLevel;
import org.alien4cloud.tosca.model.definitions.RequirementDefinition;
import org.alien4cloud.tosca.model.types.NodeType;
import org.alien4cloud.tosca.model.types.RelationshipType;
import org.springframework.stereotype.Component;

import alien4cloud.tosca.parser.ParsingContextExecution;

/**
 * Performs validation of a requirement definition.
 */
@Component
public class RequirementDefinitionPostProcessor implements IPostProcessor<RequirementDefinition> {
    @Resource
    private ReferencePostProcessor referencePostProcessor;
    @Resource
    private CapabilityOrNodeReferencePostProcessor capabilityOrNodeReferencePostProcessor;
    @Resource
    private CapabilityReferencePostProcessor capabilityReferencePostProcessor;

    @Override
    public void process(RequirementDefinition instance) {
        String definitionVersion = ParsingContextExecution.getDefinitionVersion();
        switch (definitionVersion) {
        case "tosca_simple_yaml_1_0_0_wd03":
        case "alien_dsl_1_1_0":
        case "alien_dsl_1_2_0":
            capabilityOrNodeReferencePostProcessor.process(new ReferencePostProcessor.TypeReference(instance, instance.getType()));
            break;
        default:
            // In latest versions we process the capability only.
            capabilityReferencePostProcessor.process(ParsingErrorLevel.WARNING, new ReferencePostProcessor.TypeReference(instance, instance.getType()));
            break;
        }
        if(instance.getNodeType() != null) {
            referencePostProcessor.process(new ReferencePostProcessor.TypeReference(instance, instance.getNodeType(), NodeType.class));
        }

        if (instance.getRelationshipType() != null) {
            referencePostProcessor.process(new ReferencePostProcessor.TypeReference(instance, instance.getRelationshipType(), RelationshipType.class));
        }
    }
}
