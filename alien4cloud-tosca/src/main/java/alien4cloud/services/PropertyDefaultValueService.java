package alien4cloud.services;

import alien4cloud.tosca.context.ToscaContext;
import alien4cloud.utils.CloneUtil;
import com.google.common.collect.Maps;
import lombok.extern.slf4j.Slf4j;
import org.alien4cloud.tosca.model.definitions.*;
import org.alien4cloud.tosca.model.templates.Capability;
import org.alien4cloud.tosca.model.templates.NodeTemplate;
import org.alien4cloud.tosca.model.types.CapabilityType;
import org.alien4cloud.tosca.model.types.DataType;
import org.alien4cloud.tosca.model.types.NodeType;
import org.alien4cloud.tosca.normative.types.ToscaTypes;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

import static alien4cloud.utils.AlienUtils.safe;

@Slf4j
@Component
public class PropertyDefaultValueService {

    public Map<String, AbstractPropertyValue> feedDefaultValues(NodeTemplate node) {
        var values = CloneUtil.clone(node.getProperties());

        // First we inject default values from the node type if any
        NodeType type = ToscaContext.get(NodeType.class,node.getType());
        if (type != null) {
            // Just consider the node type , no need to go up the hierarchy since
            // default values are already coalesced in property definitions
            addMissingProperties(values, type.getProperties(), PropertyDefinition::getDefault);
        }

        // Now process complex properties
        forEachComplex(values,type.getProperties(),this::feedComplexProperties);

        return values;
    }

    public Map<String, AbstractPropertyValue> feedDefaultValuesForCapability(NodeTemplate node,String capabilityName) {
        Capability capability = node.getCapabilities().get(capabilityName);

        var values = CloneUtil.clone(capability.getProperties());

        NodeType type = ToscaContext.get(NodeType.class,node.getType());
        if (type != null) {
            CapabilityDefinition capabilityDefinition = type.getCapabilities().stream().filter(cp -> cp.getId().equals(capabilityName)).findFirst().orElse(null);
            if (capabilityDefinition != null) {
                // First we inject default values from the node type if any
                addMissingProperties(values,capabilityDefinition.getProperties(),Function.identity());

                // Then we must consider the CapabilityType
                CapabilityType capabilityType = ToscaContext.get(CapabilityType.class,capabilityDefinition.getType());
                if (capabilityType != null) {
                    addMissingProperties(values,capabilityType.getProperties(),PropertyDefinition::getDefault);

                    // And finally recurse trough complex props
                    forEachComplex(values,capabilityType.getProperties(),this::feedComplexProperties);
                }
            }
        }

        return values;
    }

    private <T> void addMissingProperties(Map<String,AbstractPropertyValue> values, Map<String,T> definitions, Function<T,AbstractPropertyValue> mapper) {
        for (var entry : safe(definitions).entrySet()) {
            AbstractPropertyValue v = mapper.apply(entry.getValue());

            if (values.get(entry.getKey()) == null && v != null) {
                // No property value but a default exists
                values.put(entry.getKey(),CloneUtil.clone(v));
            }
        }
    }

    private void feedComplexProperties(Map<String,Object> values,DataType type) {
        // First feed default values
        for (var entry : safe(type.getProperties()).entrySet()) {
            if (!values.containsKey(entry.getKey())) {
                if (entry.getValue().getDefault() != null) {
                    // No value but a default exists, lets use it
                    AbstractPropertyValue v = entry.getValue().getDefault();

                    Object o = null;

                    if (entry.getValue().getType().equals(ToscaTypes.MAP) && v instanceof ComplexPropertyValue) {
                        o = ((ComplexPropertyValue) v).getValue();
                    } else if (entry.getValue().getType().equals(ToscaTypes.LIST) && v instanceof ListPropertyValue) {
                        o = ((ListPropertyValue) v).getValue();
                    } else if (v instanceof ScalarPropertyValue) {
                        o = ((ScalarPropertyValue) v).getValue();
                    } else if (!ToscaTypes.isPrimitive(entry.getValue().getType()) && v instanceof ComplexPropertyValue) {
                        o = ((ComplexPropertyValue) v).getValue();
                    } else {
                        o = v;
                    }

                    values.put(entry.getKey(), CloneUtil.clone(o));
                } else if (!ToscaTypes.isPrimitive(entry.getValue().getType())) {
                    values.put(entry.getKey(), Maps.newHashMap());
                }
            }
        }

        // Then recurse inside
        forEachComplex(values,type.getProperties(), this::feedComplexProperties);
    }

     private <T> void forEachComplex(Map<String,T> values, Map<String,PropertyDefinition> definitions, BiConsumer<Map<String,Object>,DataType> consumer) {
        for (var entry : values.entrySet()) {
            PropertyDefinition definition = definitions.get(entry.getKey());
            if (definition != null) {
                if (ToscaTypes.isPrimitive(definition.getType())) {
                    // We need to consider map & list
                    if (entry.getValue() instanceof Map && definition.getType().equals(ToscaTypes.MAP)) {
                        forEachComplexInCollection(((Map) entry.getValue()).values(), definition);
                    } else if (entry.getValue() instanceof ComplexPropertyValue && definition.getType().equals(ToscaTypes.MAP)) {
                        forEachComplexInCollection(((ComplexPropertyValue) entry.getValue()).getValue().values(), definition);
                    } else if (entry.getValue() instanceof List && definition.getType().equals(ToscaTypes.LIST)) {
                        forEachComplexInCollection((List) entry.getValue(), definition);
                    } else if (entry.getValue() instanceof ListPropertyValue && definition.getType().equals(ToscaTypes.LIST)) {
                        forEachComplexInCollection(((ListPropertyValue) entry.getValue()).getValue(), definition);
                    }
                } else {
                    DataType type = ToscaContext.get(DataType.class,definition.getType());
                    if (type != null) {
                        // Otherwise it's a primitive datatype and there's nothing to do
                        if (entry.getValue() instanceof Map) {
                            consumer.accept((Map) entry.getValue(),type);
                        } else if (entry.getValue() instanceof ComplexPropertyValue) {
                            consumer.accept(((ComplexPropertyValue) entry.getValue()).getValue(),type);
                        }
                    }
                }
            }
        }
    }

    private void forEachComplexInCollection(Collection<?> collection,PropertyDefinition definition) {
        String typeName = definition.getEntrySchema().getType();

        if (!ToscaTypes.isPrimitive(typeName)) {
            DataType type = ToscaContext.get(DataType.class,typeName);
            if (type != null) {
                // Otherwise it's a primitive datatype
                for (Object o : collection) {
                    if (o instanceof Map) {
                        feedComplexProperties((Map)o ,type);
                    }
                }
            }
        }
    }
}
