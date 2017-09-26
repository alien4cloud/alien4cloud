package org.alien4cloud.tosca.variable;

import org.alien4cloud.tosca.model.definitions.PropertyDefinition;

public class PropertyDefinitionUtils {

    private PropertyDefinitionUtils() {
    }

    public static PropertyDefinition buildPropDef(String type) {
        return buildPropDef(type, true);
    }

    public static PropertyDefinition buildPropDef(String type, boolean required) {
        PropertyDefinition propertyDefinition = new PropertyDefinition();
        propertyDefinition.setType(type);
        propertyDefinition.setRequired(required);
        propertyDefinition.setPassword(false);
        propertyDefinition.setEntrySchema(null);
        return propertyDefinition;
    }

    public static PropertyDefinition buildPropDef(String type, String entrySchema) {
        return buildPropDef(type, entrySchema, true);
    }

    public static PropertyDefinition buildPropDef(String type, PropertyDefinition entrySchema) {
        return buildPropDef(type, entrySchema, true);
    }

    public static PropertyDefinition buildPropDef(String type, PropertyDefinition entrySchema, boolean required) {
        PropertyDefinition propertyDefinition = new PropertyDefinition();
        propertyDefinition.setType(type);
        propertyDefinition.setRequired(required);
        propertyDefinition.setPassword(false);
        propertyDefinition.setEntrySchema(entrySchema);
        return propertyDefinition;
    }

    public static PropertyDefinition buildPropDef(String type, String entrySchema, boolean required) {
        return buildPropDef(type, buildPropDef(entrySchema), required);
    }
}
