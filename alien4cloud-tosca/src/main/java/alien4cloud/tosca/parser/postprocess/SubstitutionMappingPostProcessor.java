package alien4cloud.tosca.parser.postprocess;

import javax.annotation.Resource;

import org.springframework.stereotype.Component;

import org.alien4cloud.tosca.model.types.NodeType;
import org.alien4cloud.tosca.model.templates.SubstitutionMapping;
import alien4cloud.tosca.context.ToscaContext;

/**
 * Perform post processing of a substitution mapping.
 */
@Component
public class SubstitutionMappingPostProcessor implements IPostProcessor<SubstitutionMapping> {
    @Resource
    private ReferencePostProcessor referencePostProcessor;

    @Override
    public void process(SubstitutionMapping instance) {
        if (instance == null) {
            // no substitution mapping.
            return;
        }
        referencePostProcessor.process(new ReferencePostProcessor.TypeReference(instance.getSubstitutionType().getElementId(), NodeType.class));
        NodeType nodeType = ToscaContext.get(NodeType.class, instance.getSubstitutionType().getElementId());
        instance.setSubstitutionType(nodeType);

        // FIXME Ensure that the capabilities/ requirement exposed exists in the topology

    }
}