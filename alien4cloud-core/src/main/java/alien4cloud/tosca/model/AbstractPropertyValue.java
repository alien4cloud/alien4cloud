package alien4cloud.tosca.model;

/**
 * Abstract class for a property value (can be implemented by a scalar value or a function value).
 */
public abstract class AbstractPropertyValue implements IOperationParameter {
    @Override
    public boolean isDefinition() {
        return false;
    }
}