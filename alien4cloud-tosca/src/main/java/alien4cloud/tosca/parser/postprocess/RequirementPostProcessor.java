package alien4cloud.tosca.parser.postprocess;

import java.util.Map;

import javax.annotation.Resource;

import org.springframework.stereotype.Component;

import alien4cloud.model.components.IndexedCapabilityType;
import alien4cloud.model.topology.Requirement;
import alien4cloud.tosca.context.ToscaContext;

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
        // Note both post processors below are optional post processors and applied based on DSL version.
        // In previous alien DSL we authorized a dependency on a node type and not just capability type.
        capabilityOrNodeReferencePostProcessor.process(new ReferencePostProcessor.TypeReference(instance.getValue().getType()));
        // In latest versions we process the capability only.
        capabilityReferencePostProcessor.process(new ReferencePostProcessor.TypeReference(instance.getValue().getType()));

        IndexedCapabilityType capabilityType = ToscaContext.get(IndexedCapabilityType.class, instance.getValue().getType());
        propertyValueChecker.checkProperties(capabilityType, instance.getValue().getProperties(), instance.getKey());
    }
}