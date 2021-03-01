package org.alien4cloud.alm.deployment.configuration.flow.modifiers;

import alien4cloud.tosca.context.ToscaContext;
import alien4cloud.var.*;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import lombok.extern.slf4j.Slf4j;
import org.alien4cloud.alm.deployment.configuration.flow.FlowExecutionContext;
import org.alien4cloud.alm.deployment.configuration.flow.ITopologyModifier;
import org.alien4cloud.tosca.model.definitions.*;
import org.alien4cloud.tosca.model.templates.Capability;
import org.alien4cloud.tosca.model.templates.NodeTemplate;
import org.alien4cloud.tosca.model.templates.RelationshipTemplate;
import org.alien4cloud.tosca.model.templates.Topology;
import org.alien4cloud.tosca.model.types.*;
import org.alien4cloud.tosca.normative.types.ToscaTypes;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

import static alien4cloud.utils.AlienUtils.safe;

@Component
@Slf4j
public class VariableModifier implements ITopologyModifier {

    @Value("${features.auto_inputs:#{true}}")
    private boolean enabled;

    private static class VariableModifierContext {

        private final Topology topology;

        private final FlowExecutionContext flowExecutionContext;

        private final Map<String,PropertyDefinition> additionalInputs = Maps.newHashMap();

        private VariableModifierContext(Topology topology,FlowExecutionContext flowExecutionContext) {
            this.topology = topology;
            this.flowExecutionContext = flowExecutionContext;
        }
    }

    @Override
    public void process(Topology topology, FlowExecutionContext context) {
        if (!enabled) {
            return;
        }

        VariableModifierContext modifierContext = new VariableModifierContext(topology,context);

        for (NodeTemplate node : safe(topology.getNodeTemplates()).values()) {
            NodeType type = ToscaContext.get(NodeType.class, node.getType());

            if (type == null) {
                log.warn("Cannot find type {} - skipping variable substitution for node {}",node.getType(),node.getName());
                continue;
            }
            processNode(node,type,modifierContext);
        }
        mergeInputs(modifierContext);

        log.info("Variable modifier DONE");
    }

    private void processNode(NodeTemplate node,NodeType type,VariableModifierContext context) {
        Map<String,PropertyDefinition> mapDef = safe(type.getProperties());

        // Process properties
        for (Map.Entry<String, AbstractPropertyValue> entry : safe(node.getProperties()).entrySet()) {
            String path = String.format("%s.%s",node.getName(),entry.getKey());
            processProperty(entry,type,path,context);
        }

        // Process Capabilities
        for (Map.Entry<String, Capability> capabilityEntry : safe(node.getCapabilities()).entrySet()) {
            CapabilityType capabilityType = ToscaContext.get(CapabilityType.class,capabilityEntry.getValue().getType());

            for (Map.Entry<String,AbstractPropertyValue> entry : safe(capabilityEntry.getValue().getProperties()).entrySet()) {
                String path = String.format("%s.%s.%s",node.getName(),capabilityEntry.getKey(),entry.getKey());
                processProperty(entry, capabilityType, path, context);
            }
        }

        // Process Relationships
        for (Map.Entry<String, RelationshipTemplate> relationshipTemplateEntry : safe(node.getRelationships()).entrySet()) {
            RelationshipType relationshipType = ToscaContext.get(RelationshipType.class,relationshipTemplateEntry.getValue().getType());

            for (Map.Entry<String,AbstractPropertyValue> entry : safe(relationshipTemplateEntry.getValue().getProperties()).entrySet()) {
                String path = String.format("%s.%s.%s",node.getName(),relationshipTemplateEntry.getKey(),entry.getKey());
                processProperty(entry,relationshipType,path,context);
            }
        }
    }

    private void processProperty(Map.Entry<String,AbstractPropertyValue> entry, AbstractInheritableToscaType type, String path, VariableModifierContext context) {
        PropertyDefinition definition = safe(type.getProperties()).get(entry.getKey());

        if (entry.getValue() instanceof ScalarPropertyValue) {
            log.debug("Processing {}", path);
            entry.setValue((AbstractPropertyValue) processScalar((ScalarPropertyValue) entry.getValue(),definition,path,context));
        } else if (entry.getValue() instanceof ComplexPropertyValue) {
            log.debug("Processing {}[]", path);
            processComplex((ComplexPropertyValue) entry.getValue(),definition,path,context);
        } else if (entry.getValue() instanceof ListPropertyValue) {
            log.debug("Processing {}[]", path);
            processListComplex(((ListPropertyValue) entry.getValue()).getValue(),definition,path,context);
        }
    }

    private Object processScalar(ScalarPropertyValue value,PropertyDefinition definition,String path,VariableModifierContext context) {
        if (definition.getType().equals(ToscaTypes.STRING)) {
            Object result = doComplexReplacements(value.getValue(),path,context);
            if (result == value.getValue()) {
                return value;
            } else {
                return result;
            }
        }
        return value;
    }

    private void processComplex(ComplexPropertyValue value,PropertyDefinition definition,String path,VariableModifierContext context) {
        if (definition.getType().equals(ToscaTypes.MAP)) {
            processMapComplex(safe(value.getValue()),definition.getEntrySchema(),path, context);
        } else {
            processComplex(safe(value.getValue()), definition, path, context);
        }
    }

    private void processComplex(Map<String,Object> map,PropertyDefinition definition,String path,VariableModifierContext context) {
        DataType dataType = ToscaContext.get(DataType.class, definition.getType());

        if (dataType == null) {
            log.warn("Cannot find datatype {} , skipping variable substitution done in {}",definition.getType(),path);
            return;
        }

        Map<String, PropertyDefinition> definitions = safe(dataType.getProperties());

        for (Map.Entry<String, Object> entry : map.entrySet()) {
            String newPath = String.format("%s.%s", path, entry.getKey());
            PropertyDefinition subDefinition = definitions.get(entry.getKey());
            if (subDefinition != null) {
                if ((entry.getValue() instanceof String) && subDefinition.getType().equals(ToscaTypes.STRING)) {
                    entry.setValue(doComplexReplacements((String) entry.getValue(), newPath, context));
                } else if ((entry.getValue() instanceof Map) && subDefinition.getType().equals(ToscaTypes.MAP)) {
                    processMapComplex((Map) entry.getValue(), subDefinition.getEntrySchema(), newPath, context);
                } else if ((entry.getValue() instanceof Map) && (!ToscaTypes.isPrimitive(subDefinition.getType()))) {
                    processComplex((Map) entry.getValue(), subDefinition, newPath, context);
                } else if ((entry.getValue() instanceof List) && subDefinition.getType().equals(ToscaTypes.LIST)) {
                    processListComplex((List) entry.getValue(), subDefinition.getEntrySchema(), newPath, context);
                }
            }
        }
    }

    private void processMapComplex(Map<String,Object> map,PropertyDefinition definition,String path,VariableModifierContext context) {
        for (Map.Entry<String,Object> entry : map.entrySet()) {
            String newPath = String.format("%s[%s]",path,entry.getKey());

            if ((entry.getValue() instanceof String) && definition.getType().equals(ToscaTypes.STRING)) {
                entry.setValue(doComplexReplacements((String) entry.getValue(),newPath,context));
            } else if ((entry.getValue() instanceof Map) && (!ToscaTypes.isPrimitive(definition.getType()))) {
                processComplex((Map) entry.getValue(),definition,newPath,context);
            }
        }
    }

    private void processListComplex(List<Object> list,PropertyDefinition definition,String path,VariableModifierContext context) {
        for (int i = 0 ; i < list.size() ; i++ ) {
            String newPath = String.format("%s[%d]",path,i);

            if ((list.get(i) instanceof String) && definition.getType().equals(ToscaTypes.STRING)) {
                list.set(i,doComplexReplacements((String) list.get(i),newPath,context));
            } else if ((list.get(i) instanceof Map) && (!ToscaTypes.isPrimitive(definition.getType()))) {
                processComplex((Map) list.get(i),definition,newPath,context);
            }
         }
    }

    private Object doComplexReplacements(String value,String path,VariableModifierContext context) {
        try {
            List<AbstractToken> tokens = VariableTokenizer.tokenize(value);
            if (tokens.size() == 0) {
                return value;
            }

            if (tokens.size() == 1) {
                if (tokens.get(0) instanceof InputToken) {
                    return getInput(tokens.get(0),context);
                }
                return value;
            }

            ConcatPropertyValue concat = new ConcatPropertyValue();
            concat.setFunction_concat("concat");
            concat.setParameters(Lists.newArrayList());

            for (int i = 0 ; i < tokens.size() ; i++) {
                AbstractToken t = tokens.get(i);

                if (t instanceof ScalarToken) {
                    concat.getParameters().add(new ScalarPropertyValue(t.getValue()));
                } else if (t instanceof InputToken){
                    concat.getParameters().add(getInput(t,context));
                }
            }
            return concat;
        } catch(TokenizerException e) {
            return value;
        }
    }

    public FunctionPropertyValue getInput(AbstractToken token,VariableModifierContext context) {
        Map<String,PropertyDefinition> topologyInputs = context.topology.getInputs();

        String value = token.getValue();

        FunctionPropertyValue func = new FunctionPropertyValue();
        func.setFunction("get_input");
        func.setParameters(Lists.newArrayList(value));

        if (token instanceof ReferenceToken) {
            return func;
        }

        PropertyDefinition definition = safe(context.topology.getInputs()).get(value);
        if (definition != null) {
            if (!ToscaTypes.isPrimitive(definition.getType())) {
                context.flowExecutionContext.log().error(String.format("Cannot map variable with complex input '%s'",value));
                return null;
            }
            return func;
        }

        if (context.additionalInputs.containsKey(value)) {
            return func;
        }

        definition = new PropertyDefinition();
        definition.setType(ToscaTypes.STRING);
        definition.setDescription("created by variable modifier");
        definition.setRequired(false);
        context.additionalInputs.put(value,definition);

        return func;
    }

    private void mergeInputs(VariableModifierContext context) {
        Topology topology = context.topology;

        if (context.additionalInputs.size() == 0) {
            return;
        }

        if (topology.getInputs() == null) {
            topology.setInputs(context.additionalInputs);
        } else {
            topology.getInputs().putAll(context.additionalInputs);
        }
    }
}
