package alien4cloud.tosca.parser.postprocess;

import java.util.Map;

import javax.annotation.Resource;

import org.springframework.stereotype.Component;

import alien4cloud.model.components.IndexedCapabilityType;
import alien4cloud.model.topology.Capability;
import alien4cloud.tosca.context.ToscaContext;
import alien4cloud.tosca.parser.ParsingContextExecution;

/**
 * Ensure that the type exists and check capability properties.
 */
@Component
public class CapabilityPostProcessor implements IPostProcessor<Map.Entry<String, Capability>> {
    @Resource
    private ReferencePostProcessor referencePostProcessor;
    @Resource
    private PropertyValueChecker propertyValueChecker;

    @Override
    public void process(Map.Entry<String, Capability> instance) {
        referencePostProcessor.process(new ReferencePostProcessor.TypeReference(instance.getValue().getType(), IndexedCapabilityType.class));
        IndexedCapabilityType capabilityType = ToscaContext.get(IndexedCapabilityType.class, instance.getValue().getType());
        propertyValueChecker.checkProperties(capabilityType, instance.getValue().getProperties(), instance.getKey());
    }
}