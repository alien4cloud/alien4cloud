package alien4cloud.tosca;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import org.alien4cloud.tosca.model.definitions.AbstractPropertyValue;
import org.alien4cloud.tosca.model.definitions.FunctionPropertyValue;
import org.alien4cloud.tosca.model.definitions.Interface;
import org.alien4cloud.tosca.model.definitions.Operation;
import org.alien4cloud.tosca.model.definitions.ScalarPropertyValue;
import org.alien4cloud.tosca.model.templates.Capability;
import org.alien4cloud.tosca.model.templates.NodeTemplate;
import org.alien4cloud.tosca.model.templates.RelationshipTemplate;
import org.alien4cloud.tosca.normative.ToscaNormativeUtil;
import org.alien4cloud.tosca.normative.constants.NormativeComputeConstants;
import org.alien4cloud.tosca.normative.constants.ToscaFunctionConstants;
import org.alien4cloud.tosca.utils.ToscaTypeUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import com.google.common.collect.Maps;

import alien4cloud.paas.exception.PaaSTechnicalException;
import alien4cloud.paas.model.PaaSNodeTemplate;
import alien4cloud.paas.model.PaaSRelationshipTemplate;
import alien4cloud.utils.AlienUtils;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class PaaSUtils {

    public final static String CAPABILITIES = "CAPABILITIES";

    /**
     * Return
     *
     * @param paaSNodeTemplate
     * @return
     */
    public static PaaSNodeTemplate getMandatoryHostTemplate(final PaaSNodeTemplate paaSNodeTemplate) {
        PaaSNodeTemplate nodeTemplate = getHostTemplate(paaSNodeTemplate);
        if (nodeTemplate == null) {
            throw new PaaSTechnicalException(
                    "Cannot get the service name: The node template <" + paaSNodeTemplate.getId() + "> is not declared as hosted on a compute.");
        } else {
            return nodeTemplate;
        }
    }

    public static PaaSNodeTemplate getHostTemplate(PaaSNodeTemplate paaSNodeTemplate) {
        while (paaSNodeTemplate != null) {
            if (ToscaTypeUtils.isOfType(paaSNodeTemplate.getIndexedToscaElement(), NormativeComputeConstants.COMPUTE_TYPE)) {
                // Found the compute
                return paaSNodeTemplate;
            } else {
                // Not found then go to the parent
                paaSNodeTemplate = paaSNodeTemplate.getParent();
            }
        }
        return null;
    }

    /**
     * Inject properties as input parameter for all related interface operations<br>
     * <p>
     * For a <b>Node template</b>, properties injected in all its interfaces operations are:
     * <ul>
     * <li>Node template properties; {@code SELF_<PROPERTY_NAME>}</li>
     * <li>Node template capabilities properties: {@code SELF_CAPABILITIES_<CAPABILITY_NAME>_<PROPERTY_NAME>}</li>
     * </ul>
     *
     * <p>
     * For a <b>Relationship template</b>, properties injected in all its interfaces operations are:
     * <ul>
     * <li>Relationship template properties: {@code SELF_<PROPERTY_NAME>}</li>
     * <li>Relationship source properties: SOURCE_<PROPERTY_NAME>}</li>
     * <li>Relationship target properties; TARGET_<PROPERTY_NAME>}</li>
     * <li>Relationship targeted capability properties: {@code CAPABILITIES_<CAPABILITY_NAME>_<PROPERTY_NAME>}</li>
     * </ul>
     *
     * @param nodeTemplates The map of @{@link PaaSNodeTemplate} to process. Should contain ALL the nodes, or at least the nodes parts of a relationship
     */
    public static void injectPropertiesAsOperationInputs(Map<String, PaaSNodeTemplate> nodeTemplates) {
        AlienUtils.safe(nodeTemplates).values().forEach(paaSNodeTemplate -> {
            // process node template
            processNodeTemplateProperties(paaSNodeTemplate);
            // process relationships
            paaSNodeTemplate.getRelationshipTemplates()
                    .forEach(paaSRelationshipTemplate -> processRelationshipTemplateProperties(paaSRelationshipTemplate, nodeTemplates));
        });

    }

    /**
     * Inject node template and capabilities properties as input parameters for all its interfaces operations <br>
     * The injected input names are uppercased and is in form:
     * <ul>
     * <li>{@code SELF_<PROPERTY_NAME>} for node property
     * <li>{@code SELF_CAPABILITIES_<CAPABILITY_NAME>_<PROPERTY_NAME>} for capability property
     * </ul>
     * In case of name conflict, the overriding order is: (--> = overrides)
     * 
     * <pre>
     * declared input --> node property input --> capability property input
     * </pre>
     *
     * @param paaSTemplate The {@link PaaSNodeTemplate} to process
     */
    public static void processNodeTemplateProperties(PaaSNodeTemplate paaSTemplate) {
        NodeTemplate template = paaSTemplate.getTemplate();
        // inject nodetemplate properties
        if (MapUtils.isNotEmpty(template.getProperties())) {
            injectPropertiesAsInputs(ToscaFunctionConstants.SELF, null, template.getProperties(), paaSTemplate.getInterfaces(),
                    baseName -> StringUtils.joinWith(AlienUtils.DEFAULT_PREFIX_SEPARATOR, ToscaFunctionConstants.SELF, baseName));
        }
        // inject capabilities properties
        injectCapabilitiesProperties(template, paaSTemplate.getInterfaces());
    }

    private static void injectCapabilitiesProperties(NodeTemplate template, Map<String, Interface> interfaces) {
        if (template.getCapabilities() == null) {
            return;
        }
        template.getCapabilities().forEach((capabilityName, capability) -> {
            // input name: CAPABILITIES_<capabilityName>_<propertyName>
            injectPropertiesAsInputs(ToscaFunctionConstants.SELF, capabilityName, capability.getProperties(), interfaces,
                    baseName -> StringUtils.joinWith(AlienUtils.DEFAULT_PREFIX_SEPARATOR, ToscaFunctionConstants.SELF, CAPABILITIES, capabilityName, baseName));
        });
    }

    /**
     * Inject relationshipTemplate, source, target and targeted capability properties as input parameters for all its interfaces operations <br>
     * The injected input names are uppercased and is in form:
     * <ul>
     * <li>{@code SELF_<PROPERTY_NAME>} for relationship property
     * <li>{@code SOURCE_<PROPERTY_NAME>} for source property
     * <li>{@code TARGET_<PROPERTY_NAME>} for target property
     * <li>{@code TARGET_CAPABILITIES_<CAPABILITY_NAME>_<PROPERTY_NAME>} for capability property
     * </ul>
     * In case of name conflict, the overriding order is: (--> = overrides)
     *
     * <pre>
     * declared input --> relationship property input --> source property input --> target property input --> targeted capability property input
     * </pre>
     *
     * @param paaSRelationshipTemplate The {@link PaaSRelationshipTemplate} to process
     * @param paaSNodeTemplates All the nodes of the topology
     */
    public static void processRelationshipTemplateProperties(PaaSRelationshipTemplate paaSRelationshipTemplate,
            Map<String, PaaSNodeTemplate> paaSNodeTemplates) {
        RelationshipTemplate template = paaSRelationshipTemplate.getTemplate();
        // inject relationship properties
        injectPropertiesAsInputs(ToscaFunctionConstants.SELF, null, template.getProperties(), paaSRelationshipTemplate.getInterfaces(),
                baseName -> StringUtils.joinWith(AlienUtils.DEFAULT_PREFIX_SEPARATOR, ToscaFunctionConstants.SELF, baseName));

        // inject source properties
        injectSourcePropertiesAsInputs(paaSNodeTemplates.get(paaSRelationshipTemplate.getSource()), paaSRelationshipTemplate.getInterfaces());

        // inject target properties
        injectTargetPropertiesAsInputs(paaSNodeTemplates.get(template.getTarget()), paaSRelationshipTemplate.getInterfaces());

        // inject related target capability properties
        injectTargetedCapabilityProperties(paaSNodeTemplates.get(template.getTarget()), template.getTargetedCapabilityName(),
                paaSRelationshipTemplate.getInterfaces());

    }

    private static void injectTargetedCapabilityProperties(PaaSNodeTemplate target, String capabilityName, Map<String, Interface> interfaces) {
        if (target.getTemplate().getCapabilities() != null && target.getTemplate().getCapabilities().containsKey(capabilityName)) {
            Capability capability = target.getTemplate().getCapabilities().get(capabilityName);
            // input name: TARGET_CAPABILITIES_<capabilityName>_<propertyName>
            injectPropertiesAsInputs(ToscaFunctionConstants.TARGET, capabilityName, capability.getProperties(), interfaces, baseName -> StringUtils
                    .joinWith(AlienUtils.DEFAULT_PREFIX_SEPARATOR, ToscaFunctionConstants.TARGET, CAPABILITIES, capabilityName, baseName));
        }
    }

    private static void injectSourcePropertiesAsInputs(PaaSNodeTemplate source, Map<String, Interface> interfaces) {
        // input name: SOURCE_<propertyName>
        injectPropertiesAsInputs(ToscaFunctionConstants.SOURCE, null, source.getTemplate().getProperties(), interfaces,
                baseName -> StringUtils.joinWith(AlienUtils.DEFAULT_PREFIX_SEPARATOR, ToscaFunctionConstants.SOURCE, baseName));

    }

    private static void injectTargetPropertiesAsInputs(PaaSNodeTemplate target, Map<String, Interface> interfaces) {
        // input name: TARGET_<propertyName>
        injectPropertiesAsInputs(ToscaFunctionConstants.TARGET, null, target.getTemplate().getProperties(), interfaces,
                baseName -> StringUtils.joinWith(AlienUtils.DEFAULT_PREFIX_SEPARATOR, ToscaFunctionConstants.TARGET, baseName));

    }

    private static void injectPropertiesAsInputs(String entity, String capabilityName, Map<String, AbstractPropertyValue> properties,
            Map<String, Interface> interfaces, Function<String, String> inputNameBuilder) {
        if (MapUtils.isEmpty(interfaces) || MapUtils.isEmpty(properties)) {
            return;
        }

        properties.forEach((name, value) -> {
            interfaces.values().forEach((interfass) -> {
                List<String> paths = new ArrayList<>();
                paths.add(entity);
                if (StringUtils.isNotBlank(capabilityName)) {
                    paths.add(capabilityName);
                }
                paths.add(name);
                injectInputIntoOperations(inputNameBuilder.apply(name), paths, value, interfass.getOperations());
            });
        });
    }

    private static void injectInputIntoOperations(String inputName, List<String> path, AbstractPropertyValue value, Map<String, Operation> operations) {
        if (MapUtils.isEmpty(operations)) {
            return;
        }
        if (value != null) {
            value = new FunctionPropertyValue(ToscaFunctionConstants.GET_PROPERTY, path);
        } else {
            value = new ScalarPropertyValue();
        }
        // the input name should be uppercase
        AbstractPropertyValue finalValue = value;
        operations.forEach((operationName, operation) -> {
            if (operation.getInputParameters() == null) {
                operation.setInputParameters(Maps.newHashMap());
            }

            // DO NOT OVERRIDE
            operation.getInputParameters().putIfAbsent(inputName, finalValue);
        });
    }

}
