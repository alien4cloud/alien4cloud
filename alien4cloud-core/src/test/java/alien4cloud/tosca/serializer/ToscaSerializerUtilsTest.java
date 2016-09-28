package alien4cloud.tosca.serializer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import org.alien4cloud.tosca.model.definitions.AbstractPropertyValue;
import org.alien4cloud.tosca.model.definitions.ScalarPropertyValue;
import org.alien4cloud.tosca.model.definitions.constraints.AbstractPropertyConstraint;
import org.alien4cloud.tosca.model.definitions.constraints.EqualConstraint;
import org.alien4cloud.tosca.model.definitions.constraints.GreaterOrEqualConstraint;
import org.alien4cloud.tosca.model.definitions.constraints.GreaterThanConstraint;
import org.alien4cloud.tosca.model.definitions.constraints.InRangeConstraint;
import org.alien4cloud.tosca.model.definitions.constraints.LengthConstraint;
import org.alien4cloud.tosca.model.definitions.constraints.LessOrEqualConstraint;
import org.alien4cloud.tosca.model.definitions.constraints.LessThanConstraint;
import org.alien4cloud.tosca.model.definitions.constraints.MaxLengthConstraint;
import org.alien4cloud.tosca.model.definitions.constraints.MinLengthConstraint;
import org.alien4cloud.tosca.model.definitions.constraints.PatternConstraint;
import org.alien4cloud.tosca.model.definitions.constraints.ValidValuesConstraint;
import org.alien4cloud.tosca.model.templates.Capability;
import org.alien4cloud.tosca.model.templates.NodeTemplate;
import alien4cloud.tosca.normative.IPropertyType;
import alien4cloud.tosca.properties.constraints.exception.ConstraintValueDoNotMatchPropertyTypeException;
import alien4cloud.tosca.properties.constraints.exception.ConstraintViolationException;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

public class ToscaSerializerUtilsTest {

    private ToscaSerializerUtils utils;

    @Before
    public void prepare() {
        utils = new ToscaSerializerUtils();
    }

    @Test
    public void testCollectionIsNotEmpty() {
        Assert.assertFalse(utils.collectionIsNotEmpty(null));
        Assert.assertFalse(utils.collectionIsNotEmpty(new ArrayList<String>()));
        Assert.assertTrue(utils.collectionIsNotEmpty(Lists.newArrayList("Element")));
    }

    @Test
    public void testMapIsNotEmpty() {
        Assert.assertFalse(utils.mapIsNotEmpty(null));
        Assert.assertFalse(utils.mapIsNotEmpty(Maps.newHashMap()));
        Map<String, Object> map = Maps.newHashMap();
        map.put("key1", null);
        Assert.assertTrue(utils.mapIsNotEmpty(map));
        map = Maps.newHashMap();
        map.put("key1", "value1");
        Assert.assertTrue(utils.mapIsNotEmpty(map));
    }

    @Test
    public void testRenderScalar() {
        Assert.assertEquals("a scalar", ToscaPropertySerializerUtils.renderScalar("a scalar"));
        // contains a [ so should be quoted
        Assert.assertEquals("\"[a scalar\"", ToscaPropertySerializerUtils.renderScalar("[a scalar"));
        Assert.assertEquals("\"a ]scalar\"", ToscaPropertySerializerUtils.renderScalar("a ]scalar"));
        // contains a { so should be quoted
        Assert.assertEquals("\"{a scalar\"", ToscaPropertySerializerUtils.renderScalar("{a scalar"));
        Assert.assertEquals("\"a }scalar\"", ToscaPropertySerializerUtils.renderScalar("a }scalar"));
        // contains a : so should be quoted
        Assert.assertEquals("\":a scalar\"", ToscaPropertySerializerUtils.renderScalar(":a scalar"));
        Assert.assertEquals("\"a :scalar\"", ToscaPropertySerializerUtils.renderScalar("a :scalar"));
        // contains a - so should be quoted
        Assert.assertEquals("\"-a scalar\"", ToscaPropertySerializerUtils.renderScalar("-a scalar"));
        Assert.assertEquals("\"a -scalar\"", ToscaPropertySerializerUtils.renderScalar("a -scalar"));
        // starts or ends with a ' ' so should be quoted
        Assert.assertEquals("\" a scalar\"", ToscaPropertySerializerUtils.renderScalar(" a scalar"));
        Assert.assertEquals("\"a scalar \"", ToscaPropertySerializerUtils.renderScalar("a scalar "));
        // and then " should be escaped
        Assert.assertEquals("\"a \\\"scalar\\\" \"", ToscaPropertySerializerUtils.renderScalar("a \"scalar\" "));
    }

    @Test
    public void testRenderDescription() throws IOException {
        Assert.assertEquals("a single line description", utils.renderDescription("a single line description", "  "));
        Assert.assertEquals("|\n  a multi line \n  description", utils.renderDescription("a multi line \ndescription", "  "));
    }

    @Test
    public void testMapIsNotEmptyAndContainsNotnullValues() {
        Assert.assertFalse(utils.mapIsNotEmptyAndContainsNotnullValues(null));
        Assert.assertFalse(utils.mapIsNotEmptyAndContainsNotnullValues(Maps.newHashMap()));
        Map<String, Object> map = Maps.newHashMap();
        map.put("key1", null);
        Assert.assertFalse(utils.mapIsNotEmptyAndContainsNotnullValues(map));
        map.put("key2", "something");
        Assert.assertTrue(utils.mapIsNotEmptyAndContainsNotnullValues(map));

        // inner collection
        Map<String, Set<String>> mapOfSet = Maps.newHashMap();
        Set<String> set = Sets.newHashSet();
        mapOfSet.put("key1", set);
        // the set is empty
        Assert.assertFalse(utils.mapIsNotEmptyAndContainsNotnullValues(mapOfSet));
        Set<String> filledSet = Sets.newHashSet("something");
        mapOfSet.put("key2", filledSet);
        // the second set contains something
        Assert.assertTrue(utils.mapIsNotEmptyAndContainsNotnullValues(mapOfSet));

        // inner map
        Map<String, Map<String, Set<String>>> mapOfmap = Maps.newHashMap();
        Map<String, Set<String>> innerMap = Maps.newHashMap();
        mapOfmap.put("key1", innerMap);
        // the inner map is empty
        Assert.assertFalse(utils.mapIsNotEmptyAndContainsNotnullValues(mapOfmap));
        Map<String, Set<String>> innerMap2 = Maps.newHashMap();
        Set<String> emptySet = Sets.newHashSet();
        innerMap2.put("key21", emptySet);
        mapOfmap.put("key2", innerMap2);
        // the inner set is empty
        Assert.assertFalse(utils.mapIsNotEmptyAndContainsNotnullValues(mapOfmap));
        filledSet = Sets.newHashSet("something");
        innerMap2.put("key22", filledSet);
        Assert.assertTrue(utils.mapIsNotEmptyAndContainsNotnullValues(mapOfmap));

        // ScalarPropertyValue
        ScalarPropertyValue spv = new ScalarPropertyValue();
        Map<String, AbstractPropertyValue> apvMap = new HashMap<String, AbstractPropertyValue>();
        apvMap.put("key1", spv);
        Assert.assertFalse(utils.mapIsNotEmptyAndContainsNotnullValues(apvMap));
        spv.setValue("value");
        Assert.assertTrue(utils.mapIsNotEmptyAndContainsNotnullValues(apvMap));
    }

    @Test
    public void testGetCsvToString() {
        Assert.assertEquals("", utils.getCsvToString(null));
        Assert.assertEquals("", utils.getCsvToString(Lists.newArrayList()));
        Assert.assertEquals("one", utils.getCsvToString(Lists.newArrayList("one")));
        Assert.assertEquals("one, two", utils.getCsvToString(Lists.newArrayList("one", "two")));
        Assert.assertEquals("1, 2", utils.getCsvToString(Lists.newArrayList(Integer.valueOf(1), Integer.valueOf(2))));
        Assert.assertEquals("one, two, \"three,four\"", utils.getCsvToString(Lists.newArrayList("one", "two", "three,four"), true));
    }

    @Test
    public void testHasCapabilitiesContainingNotNullProperties() {
        NodeTemplate nt = new NodeTemplate();
        Assert.assertFalse(utils.hasCapabilitiesContainingNotNullProperties(nt));
        Map<String, Capability> capabilities = Maps.newHashMap();
        nt.setCapabilities(capabilities);
        Assert.assertFalse(utils.hasCapabilitiesContainingNotNullProperties(nt));
        Capability capability1 = new Capability();
        capabilities.put("capa1", capability1);
        Assert.assertFalse(utils.hasCapabilitiesContainingNotNullProperties(nt));
        Capability capability2 = new Capability();
        Map<String, AbstractPropertyValue> properties = new HashMap<String, AbstractPropertyValue>();
        capability2.setProperties(properties);
        capabilities.put("capa2", capability2);
        Assert.assertFalse(utils.hasCapabilitiesContainingNotNullProperties(nt));
        properties.put("prop1", null);
        Assert.assertFalse(utils.hasCapabilitiesContainingNotNullProperties(nt));
        properties.put("prop2", new ScalarPropertyValue("value"));
        Assert.assertTrue(utils.hasCapabilitiesContainingNotNullProperties(nt));
    }

    @Test
    public void testRenderConstraint() {
        GreaterOrEqualConstraint greaterOrEqualConstraint = new GreaterOrEqualConstraint();
        Assert.assertEquals("greater_or_equal: null", utils.renderConstraint(greaterOrEqualConstraint));
        greaterOrEqualConstraint.setGreaterOrEqual("1");
        Assert.assertEquals("greater_or_equal: 1", utils.renderConstraint(greaterOrEqualConstraint));

        GreaterThanConstraint greaterThanConstraint = new GreaterThanConstraint();
        Assert.assertEquals("greater_than: null", utils.renderConstraint(greaterThanConstraint));
        greaterThanConstraint.setGreaterThan("1");
        Assert.assertEquals("greater_than: 1", utils.renderConstraint(greaterThanConstraint));

        LessOrEqualConstraint lessOrEqualConstraint = new LessOrEqualConstraint();
        Assert.assertEquals("less_or_equal: null", utils.renderConstraint(lessOrEqualConstraint));
        lessOrEqualConstraint.setLessOrEqual("1");
        Assert.assertEquals("less_or_equal: 1", utils.renderConstraint(lessOrEqualConstraint));

        LessThanConstraint lessThanConstraint = new LessThanConstraint();
        Assert.assertEquals("less_than: null", utils.renderConstraint(lessThanConstraint));
        lessThanConstraint.setLessThan("1");
        Assert.assertEquals("less_than: 1", utils.renderConstraint(lessThanConstraint));

        LengthConstraint lengthConstraint = new LengthConstraint();
        Assert.assertEquals("length: null", utils.renderConstraint(lengthConstraint));
        lengthConstraint.setLength(1);
        Assert.assertEquals("length: 1", utils.renderConstraint(lengthConstraint));

        MaxLengthConstraint maxLengthConstraint = new MaxLengthConstraint();
        Assert.assertEquals("max_length: null", utils.renderConstraint(maxLengthConstraint));
        maxLengthConstraint.setMaxLength(1);
        Assert.assertEquals("max_length: 1", utils.renderConstraint(maxLengthConstraint));

        MinLengthConstraint minLengthConstraint = new MinLengthConstraint();
        Assert.assertEquals("min_length: null", utils.renderConstraint(minLengthConstraint));
        minLengthConstraint.setMinLength(1);
        Assert.assertEquals("min_length: 1", utils.renderConstraint(minLengthConstraint));

        PatternConstraint patternConstraint = new PatternConstraint();
        Assert.assertEquals("pattern: null", utils.renderConstraint(patternConstraint));
        patternConstraint.setPattern("a");
        Assert.assertEquals("pattern: a", utils.renderConstraint(patternConstraint));
        patternConstraint.setPattern("[.*]");
        Assert.assertEquals("pattern: \"[.*]\"", utils.renderConstraint(patternConstraint));

        EqualConstraint equalConstraint = new EqualConstraint();
        Assert.assertEquals("equal: null", utils.renderConstraint(equalConstraint));
        equalConstraint.setEqual("value");
        Assert.assertEquals("equal: value", utils.renderConstraint(equalConstraint));
        equalConstraint.setEqual(" value");
        Assert.assertEquals("equal: \" value\"", utils.renderConstraint(equalConstraint));

        InRangeConstraint inRangeConstraint = new InRangeConstraint();
        Assert.assertEquals("in_range: []", utils.renderConstraint(inRangeConstraint));
        List<String> inRange = Lists.newArrayList();
        inRangeConstraint.setInRange(inRange);
        Assert.assertEquals("in_range: []", utils.renderConstraint(inRangeConstraint));
        inRange.add("1");
        Assert.assertEquals("in_range: [1]", utils.renderConstraint(inRangeConstraint));
        inRange.add("2");
        Assert.assertEquals("in_range: [1, 2]", utils.renderConstraint(inRangeConstraint));

        ValidValuesConstraint validValuesConstraint = new ValidValuesConstraint();
        Assert.assertEquals("valid_values: []", utils.renderConstraint(validValuesConstraint));
        List<String> validValues = Lists.newArrayList();
        validValuesConstraint.setValidValues(validValues);
        Assert.assertEquals("valid_values: []", utils.renderConstraint(validValuesConstraint));
        validValues.add("value1");
        Assert.assertEquals("valid_values: [value1]", utils.renderConstraint(validValuesConstraint));
        validValues.add("value2 ");
        Assert.assertEquals("valid_values: [value1, \"value2 \"]", utils.renderConstraint(validValuesConstraint));
        validValues.add("value3,value4");
        Assert.assertEquals("valid_values: [value1, \"value2 \", \"value3,value4\"]", utils.renderConstraint(validValuesConstraint));

        // finally test an unknown constraint
        AbstractPropertyConstraint abstractPropertyConstraint = new AbstractPropertyConstraint() {
            @Override
            public void validate(Object propertyValue) throws ConstraintViolationException {
            }

            @Override
            public void initialize(IPropertyType<?> propertyType) throws ConstraintValueDoNotMatchPropertyTypeException {
            }
        };
        Assert.assertEquals("", utils.renderConstraint(abstractPropertyConstraint));
    }
}
