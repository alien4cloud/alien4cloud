package alien4cloud.ui.form;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class FormExampleObject {

    private Boolean booleanWrappedField;

    private boolean booleanField;

    private String textField;

    private int intField;

    private Integer intWrappedField;

    private double doubleField;

    private Double doubleWrappedField;

    private float floatField;

    private Float floatWrappedField;

    private long longField;

    private Long longWrappedField;

    private short shortField;

    private Short shortWrappedField;

    private Date dateField;

    private Map<String, String> mapOfPrimitive;

    private Set<String> setOfPrimitive;

    private List<String> listOfPrimitive;

    private String[] arrayOfPrimitive;

    private NestedObject nestedObject;

    private Map<String, NestedObject> mapOfComplex;

    private Set<NestedObject> setOfComplex;

    private List<NestedObject> listOfComplex;

    @Getter
    @Setter
    private static class NestedObject {

        private String nestedObjectField;
    }
}
