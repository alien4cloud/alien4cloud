package alien4cloud.tosca.parser.postprocess;

import static alien4cloud.utils.AlienUtils.safe;

import java.util.Map;

import javax.annotation.Resource;

import org.springframework.stereotype.Component;

import org.alien4cloud.tosca.model.types.CapabilityType;
import org.alien4cloud.tosca.model.templates.Capability;
import alien4cloud.tosca.context.ToscaContext;

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
        referencePostProcessor.process(new ReferencePostProcessor.TypeReference(instance.getValue().getType(), CapabilityType.class));
        CapabilityType capabilityType = ToscaContext.get(CapabilityType.class, instance.getValue().getType());
        if (capabilityType == null) {
            return;
        }
        propertyValueChecker.checkProperties(capabilityType, instance.getValue().getProperties(), instance.getKey());
    }
}