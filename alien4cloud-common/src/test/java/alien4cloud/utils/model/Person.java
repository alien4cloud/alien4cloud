package alien4cloud.utils.model;

import lombok.Getter;
import lombok.Setter;

/**
 * Simple data model to test deserialization of null instances for json merge syntax.
 */
@Getter
@Setter
public class Person {
    private String name;
    private Address address;
}
