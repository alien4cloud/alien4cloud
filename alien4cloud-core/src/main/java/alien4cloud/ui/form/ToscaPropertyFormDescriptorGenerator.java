package alien4cloud.ui.form;

import static alien4cloud.ui.form.GenericFormConstants.ARRAY_TYPE;
import static alien4cloud.ui.form.GenericFormConstants.COMPLEX_TYPE;
import static alien4cloud.ui.form.GenericFormConstants.CONTENT_TYPE_KEY;
import static alien4cloud.ui.form.GenericFormConstants.MAP_TYPE;
import static alien4cloud.ui.form.GenericFormConstants.ORDER_KEY;
import static alien4cloud.ui.form.GenericFormConstants.PROPERTY_TYPE_KEY;
import static alien4cloud.ui.form.GenericFormConstants.TOSCA_DEFINITION_KEY;
import static alien4cloud.ui.form.GenericFormConstants.TOSCA_TYPE;
import static alien4cloud.ui.form.GenericFormConstants.TYPE_KEY;

import java.util.Map;
import java.util.Set;

import javax.annotation.Resource;

import org.springframework.stereotype.Component;

import alien4cloud.component.ICSARRepositorySearchService;
import alien4cloud.exception.InvalidArgumentException;
import org.alien4cloud.tosca.model.CSARDependency;
import org.alien4cloud.tosca.model.types.DataType;
import org.alien4cloud.tosca.model.types.PrimitiveDataType;
import org.alien4cloud.tosca.model.definitions.PropertyDefinition;
import alien4cloud.tosca.normative.ToscaType;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

@Component
public class ToscaPropertyFormDescriptorGenerator {

    @Resource
    private ICSARRepositorySearchService searchService;

    public Map<String, Object> generateDescriptor(PropertyDefinition propertyDefinition, Set<CSARDependency> dependencies) {
        return doGenerateDescriptor(Sets.<String> newHashSet(), propertyDefinition, dependencies);
    }

    private Map<String, Object> doGenerateDescriptor(Set<String> processedDataTypes, PropertyDefinition propertyDefinition, Set<CSARDependency> dependencies) {
        if (ToscaType.isSimple(propertyDefinition.getType())) {
            return generateDescriptorForSimpleType(propertyDefinition);
        } else if (ToscaType.LIST.equals(propertyDefinition.getType())) {
            PropertyDefinition entryDefinition = propertyDefinition.getEntrySchema();
            if (entryDefinition == null) {
                throw new InvalidArgumentException("List type without entry schema");
            }
            return generateDescriptorForListType(processedDataTypes, entryDefinition, dependencies);
        } else if (ToscaType.MAP.equals(propertyDefinition.getType())) {
            PropertyDefinition entryDefinition = propertyDefinition.getEntrySchema();
            if (entryDefinition == null) {
                throw new InvalidArgumentException("Map type without entry schema");
            }
            return generateDescriptorForMapType(processedDataTypes, entryDefinition, dependencies);
        } else {
            DataType dataType = searchService.getElementInDependencies(DataType.class, propertyDefinition.getType(), dependencies);
            if (dataType == null) {
                throw new InvalidArgumentException("Data type <" + propertyDefinition.getType() + "> do not exist in dependencies " + dependencies);
            }
            if (processedDataTypes.add(dataType.getElementId())) {
                return generateDescriptorForDataType(processedDataTypes, dataType, dependencies);
            } else {
                return generateDescriptorForSimpleType(propertyDefinition);
            }
        }
    }

    private Map<String, Object> generateDescriptorForDataType(Set<String> processedDataTypes, DataType dataType, Set<CSARDependency> dependencies) {
        Map<String, Object> dataTypeDescriptors = Maps.newHashMap();
        if (dataType instanceof PrimitiveDataType) {
            dataTypeDescriptors.put(TYPE_KEY, TOSCA_TYPE);
            PropertyDefinition propertyDefinition = new PropertyDefinition();
            propertyDefinition.setType(dataType.getDerivedFrom().get(0));
            propertyDefinition.setConstraints(((PrimitiveDataType) dataType).getConstraints());
            dataTypeDescriptors.put(TOSCA_DEFINITION_KEY, propertyDefinition);
        } else {
            dataTypeDescriptors.put(TYPE_KEY, COMPLEX_TYPE);
            Map<String, Object> propertyTypes = Maps.newHashMap();
            dataTypeDescriptors.put(PROPERTY_TYPE_KEY, propertyTypes);
            if (dataType.getProperties() != null) {
                for (Map.Entry<String, PropertyDefinition> propertyDefinitionEntry : dataType.getProperties().entrySet()) {
                    propertyTypes.put(propertyDefinitionEntry.getKey(),
                            doGenerateDescriptor(processedDataTypes, propertyDefinitionEntry.getValue(), dependencies));
                }
                dataTypeDescriptors.put(ORDER_KEY, dataType.getProperties().keySet());
            }
        }
        return dataTypeDescriptors;
    }

    private Map<String, Object> generateDescriptorForSimpleType(PropertyDefinition propertyDefinition) {
        Map<String, Object> typeDescriptor = Maps.newHashMap();
        typeDescriptor.put(TYPE_KEY, TOSCA_TYPE);
        typeDescriptor.put(TOSCA_DEFINITION_KEY, propertyDefinition);
        return typeDescriptor;
    }

    private Map<String, Object> generateDescriptorForListType(Set<String> processedDataTypes, PropertyDefinition entryDefinition,
            Set<CSARDependency> dependencies) {
        Map<String, Object> listDescriptors = Maps.newHashMap();
        listDescriptors.put(TYPE_KEY, ARRAY_TYPE);
        listDescriptors.put(CONTENT_TYPE_KEY, doGenerateDescriptor(processedDataTypes, entryDefinition, dependencies));
        return listDescriptors;
    }

    private Map<String, Object> generateDescriptorForMapType(Set<String> processedDataTypes, PropertyDefinition entryDefinition,
            Set<CSARDependency> dependencies) {
        Map<String, Object> mapDescriptors = Maps.newHashMap();
        mapDescriptors.put(TYPE_KEY, MAP_TYPE);
        mapDescriptors.put(CONTENT_TYPE_KEY, doGenerateDescriptor(processedDataTypes, entryDefinition, dependencies));
        return mapDescriptors;
    }
}
