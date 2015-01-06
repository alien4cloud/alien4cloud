package alien4cloud.model.components;


/**
 * An operation parameter can be either a PropertyValue (value or expression) or a PropertyDefinition.
 */
public interface IOperationParameter {
    /**
     * Allow to know if the operation parameter is a property definition or a property value. Only parameter exposed as property definitions can be used for
     * "custom" operations.
     *
     * @return true if the operation parameter is a property definition and false if the parameter is a property value.
     */
    boolean isDefinition();
}