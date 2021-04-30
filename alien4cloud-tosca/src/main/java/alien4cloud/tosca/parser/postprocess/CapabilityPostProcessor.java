package alien4cloud.tosca.parser.postprocess;

import java.util.Map;

import javax.annotation.Resource;

import alien4cloud.services.PropertyDefaultValueService;
import org.alien4cloud.tosca.model.templates.NodeTemplate;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import org.alien4cloud.tosca.model.types.CapabilityType;
import org.alien4cloud.tosca.model.templates.Capability;
import alien4cloud.tosca.context.ToscaContext;

/**
 * Ensure that the type exists and check capability properties.
 */
@Component
public class CapabilityPostProcessor extends BiPostProcessor<NodeTemplate,Map.Entry<String, Capability>> {
    @Resource
    private ReferencePostProcessor referencePostProcessor;
    @Resource
    private PropertyValueChecker propertyValueChecker;

    @Autowired
    private PropertyDefaultValueService defaultValueService;

    @Override
    public void process(NodeTemplate nodeTemplate,Map.Entry<String, Capability> instance) {
        referencePostProcessor.process(new ReferencePostProcessor.TypeReference(instance.getValue(), instance.getValue().getType(), CapabilityType.class));

        var fedProperties = defaultValueService.feedDefaultValuesForCapability(nodeTemplate, instance.getKey());

        CapabilityType capabilityType = ToscaContext.get(CapabilityType.class, instance.getValue().getType());
        if (capabilityType == null) {
            return;
        }
        propertyValueChecker.checkProperties(capabilityType, fedProperties, instance.getKey());
    }

}