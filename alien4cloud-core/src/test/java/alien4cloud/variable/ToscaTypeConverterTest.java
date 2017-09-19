package alien4cloud.variable;

import org.alien4cloud.tosca.model.definitions.PropertyDefinition;
import org.alien4cloud.tosca.model.definitions.PropertyValue;
import org.alien4cloud.tosca.normative.types.TimeType;
import org.alien4cloud.tosca.normative.types.ToscaTypes;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.*;
import static org.junit.Assert.*;

public class ToscaTypeConverterTest {

    ToscaTypeConverter converter;

    @Before
    public void setUp() throws Exception {
    converter = new ToscaTypeConverter();
    }

    @Test
    public void convert() throws Exception {
        PropertyDefinition propertyDefinition = new PropertyDefinition();
        propertyDefinition.setType(ToscaTypes.TIME);
        PropertyValue propertyValue = converter.convert("2 d", propertyDefinition);
        //System.out.println(new TimeType().parse(propertyValue.getValue().toString()));
        //assertThat(propertyValue).isEqualTo(null);
    }

}