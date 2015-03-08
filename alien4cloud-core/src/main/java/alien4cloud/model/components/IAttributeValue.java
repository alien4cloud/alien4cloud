package alien4cloud.model.components;

/**
 * An attribute can be a {@link ConcatPropertyValue} or a {@link AttributeDefinition}
 * 
 * @author mourouvi
 *
 */
public interface IAttributeValue {
    /**
     * Define if attribute property is a definition or not
     * 
     * @return true if the attribute value is a definition
     */
    boolean isDefinition();
}
