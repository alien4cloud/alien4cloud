package alien4cloud.tosca.normative;

/**
 * This class represents all normative property type as string, integer, scalar-unit.size ...
 * 
 * @author Minh Khang VU
 */
public interface IPropertyType<T> {

    T parse(String text) throws InvalidPropertyValueException;

    String print(T value);

    String getTypeName();
}