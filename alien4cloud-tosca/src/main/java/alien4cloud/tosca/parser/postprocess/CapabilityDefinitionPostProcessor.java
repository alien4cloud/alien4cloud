package alien4cloud.tosca.parser.postprocess;

import org.alien4cloud.tosca.model.definitions.CapabilityDefinition;
import org.alien4cloud.tosca.model.types.NodeType;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 *
 */
@Component
public class CapabilityDefinitionPostProcessor implements IPostProcessor<CapabilityDefinition> {
    @Resource
    private CapabilityReferencePostProcessor capabilityReferencePostProcessor;

    @Override
    public void process(CapabilityDefinition instance) {
        capabilityReferencePostProcessor.process(new ReferencePostProcessor.TypeReference(instance, instance.getType()));
    }
}
