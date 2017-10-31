package org.alien4cloud.tosca.editor.processors.policies;

import javax.annotation.Resource;

import org.alien4cloud.tosca.editor.EditionContextManager;
import org.alien4cloud.tosca.editor.exception.PropertyValueException;
import org.alien4cloud.tosca.editor.operations.policies.UpdatePolicyPropertyValueOperation;
import org.alien4cloud.tosca.exceptions.ConstraintFunctionalException;
import org.alien4cloud.tosca.model.Csar;
import org.alien4cloud.tosca.model.definitions.PropertyDefinition;
import org.alien4cloud.tosca.model.templates.PolicyTemplate;
import org.alien4cloud.tosca.model.templates.Topology;
import org.alien4cloud.tosca.model.types.PolicyType;
import org.springframework.stereotype.Component;

import alien4cloud.tosca.context.ToscaContext;
import alien4cloud.utils.AlienUtils;
import alien4cloud.utils.services.PropertyService;
import lombok.extern.slf4j.Slf4j;

/**
 * Updates the value of a property in a topology.
 */
@Slf4j
@Component
public class UpdatePolicyPropertyValueProcessor extends AbstractPolicyProcessor<UpdatePolicyPropertyValueOperation> {
    @Resource
    private PropertyService propertyService;

    @Override
    protected void process(Csar csar, Topology topology, UpdatePolicyPropertyValueOperation operation, PolicyTemplate policyTemplate) {
        PolicyType policyType = ToscaContext.getOrFail(PolicyType.class, policyTemplate.getType());
        PropertyDefinition propertyDefinition = AlienUtils.getOrFail(policyType.getProperties(), operation.getPropertyName(),
                "Property [ {} ] doesn't exists in type [ {} ] for policy [ {} ].", operation.getPropertyName(), policyTemplate.getType(), operation.getPolicyName());

        log.debug("Updating property [ {} ] of the policy template [ {} ] from the topology [ {} ]: changing value from [{}] to [{}].", operation.getPropertyName(),
                operation.getPolicyName(), topology.getId(), policyTemplate.getProperties().get(operation.getPropertyName()), operation.getPropertyValue());

        try {
            propertyService.setPropertyValue(policyTemplate, propertyDefinition, operation.getPropertyName(), operation.getPropertyValue());
        } catch (ConstraintFunctionalException e) {
            throw new PropertyValueException("Error when setting policy " + operation.getPolicyName() + " property.", e, operation.getPropertyName(),
                    operation.getPropertyValue());
        }
    }
}
