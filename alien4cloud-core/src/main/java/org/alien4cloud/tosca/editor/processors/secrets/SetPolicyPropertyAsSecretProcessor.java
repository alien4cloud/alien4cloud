package org.alien4cloud.tosca.editor.processors.secrets;

import java.util.Arrays;

import javax.annotation.Resource;

import org.alien4cloud.tosca.editor.operations.secrets.SetPolicyPropertyAsSecretOperation;
import org.alien4cloud.tosca.editor.processors.policies.AbstractPolicyProcessor;
import org.alien4cloud.tosca.model.Csar;
import org.alien4cloud.tosca.model.definitions.FunctionPropertyValue;
import org.alien4cloud.tosca.model.definitions.PropertyDefinition;
import org.alien4cloud.tosca.model.templates.PolicyTemplate;
import org.alien4cloud.tosca.model.templates.Topology;
import org.alien4cloud.tosca.model.types.PolicyType;
import org.alien4cloud.tosca.normative.constants.ToscaFunctionConstants;
import org.springframework.stereotype.Component;

import alien4cloud.tosca.context.ToscaContext;
import alien4cloud.utils.AlienUtils;
import alien4cloud.utils.services.PropertyService;
import lombok.extern.slf4j.Slf4j;

/**
 * Set the value of a property of policy as a secret.
 */
@Slf4j
@Component
public class SetPolicyPropertyAsSecretProcessor extends AbstractPolicyProcessor<SetPolicyPropertyAsSecretOperation> {
    @Resource
    private PropertyService propertyService;

    @Override
    protected void process(Csar csar, Topology topology, SetPolicyPropertyAsSecretOperation operation, PolicyTemplate policyTemplate) {
        PolicyType policyType = ToscaContext.getOrFail(PolicyType.class, policyTemplate.getType());
        AlienUtils.getOrFail(policyType.getProperties(), operation.getPropertyName(),
                "Property [ {} ] doesn't exists in type [ {} ] for policy [ {} ].", operation.getPropertyName(), policyTemplate.getType(), operation.getPolicyName());

        FunctionPropertyValue secretFunction = new FunctionPropertyValue();
        secretFunction.setFunction(ToscaFunctionConstants.GET_SECRET);
        secretFunction.setParameters(Arrays.asList(operation.getSecretPath()));
        policyTemplate.getProperties().put(operation.getPropertyName(), secretFunction);

        log.debug("Associate the property [ {} ] of the policy template [ {} ] as secret [ {} ] of the topology [ {} ].", operation.getPropertyName(),
                operation.getPolicyName(), operation.getSecretPath(), topology.getId());

    }
}
