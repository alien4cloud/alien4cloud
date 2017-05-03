package alien4cloud.tosca.parser.postprocess;

import javax.annotation.Resource;

import org.alien4cloud.tosca.model.templates.SubstitutionMapping;
import org.alien4cloud.tosca.model.types.NodeType;
import org.springframework.stereotype.Component;

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
        referencePostProcessor
                .process(new ReferencePostProcessor.TypeReference(instance.getSubstitutionType(), instance.getSubstitutionType(), NodeType.class));
    }
}