package alien4cloud.tosca.properties.constraints;

import lombok.Getter;
import lombok.Setter;

import org.junit.Test;

import alien4cloud.tosca.container.model.type.ToscaType;
import alien4cloud.tosca.properties.constraints.EqualConstraint;
import alien4cloud.tosca.properties.constraints.GreaterOrEqualConstraint;
import alien4cloud.tosca.properties.constraints.GreaterThanConstraint;
import alien4cloud.tosca.properties.constraints.InRangeConstraint;
import alien4cloud.tosca.properties.constraints.LengthConstraint;
import alien4cloud.tosca.properties.constraints.LessOrEqualConstraint;
import alien4cloud.tosca.properties.constraints.LessThanConstraint;
import alien4cloud.tosca.properties.constraints.MaxLengthConstraint;
import alien4cloud.tosca.properties.constraints.MinLengthConstraint;
import alien4cloud.tosca.properties.constraints.PatternConstraint;
import alien4cloud.tosca.properties.constraints.ValidValuesConstraint;
import alien4cloud.tosca.properties.constraints.exception.ConstraintValueDoNotMatchPropertyTypeException;
import alien4cloud.tosca.properties.constraints.exception.ConstraintViolationException;
import alien4cloud.utils.VersionUtil;

import com.google.common.collect.Lists;

public class PropertiesConstraintsTest {

    @Getter
    @Setter
    private static class TestClass {
        private Toto toto;
    }

    @Getter
    @Setter
    private static class Toto {
        private String tata;
        private String[] titi;
    }

    @Test
    public void testEqualConstraintSatisfied() throws Exception {
        EqualConstraint constraint = new EqualConstraint();
        constraint.setEqual("1");
        constraint.initialize(ToscaType.INTEGER);
        constraint.validate(1l);
        constraint.setEqual("toto");
        constraint.initialize(ToscaType.STRING);
        constraint.validate("toto");
        constraint.setEqual("1.6");
        constraint.initialize(ToscaType.VERSION);
        constraint.validate(VersionUtil.parseVersion("1.6"));
    }

    @Test(expected = ConstraintViolationException.class)
    public void testEqualConstraintFailed() throws Exception {
        EqualConstraint constraint = new EqualConstraint();
        constraint.setEqual("1");
        constraint.initialize(ToscaType.INTEGER);
        constraint.validate(2l);
    }

    @Test(expected = ConstraintViolationException.class)
    public void testComplexEqualConstraintFailed() throws Exception {
        EqualConstraint constraint = new EqualConstraint();
        constraint.setEqual(
                "toto:\n" +
                        "  tata: tata\n" +
                        "  titi:\n" +
                        "    - titi1\n" +
                        "    - titi2\n");
        TestClass testClass = new TestClass();
        testClass.setToto(new Toto());
        testClass.getToto().setTata("tata");
        testClass.getToto().setTiti(new String[] { "titi11", "titi22" });
        constraint.validate(testClass);
    }

    @Test
    public void testInRangeConstraintSatisfied() throws Exception {
        InRangeConstraint constraint = new InRangeConstraint();
        constraint.setInRange(Lists.newArrayList("1", "4"));
        constraint.initialize(ToscaType.INTEGER);
        constraint.setInRange(Lists.newArrayList("1.6", "1.8"));
        constraint.initialize(ToscaType.VERSION);
        constraint.validate(VersionUtil.parseVersion("1.7"));
    }

    @Test(expected = ConstraintViolationException.class)
    public void testInRangeConstraintFailed() throws Exception {
        InRangeConstraint constraint = new InRangeConstraint();
        constraint.setInRange(Lists.newArrayList("1", "4"));
        constraint.initialize(ToscaType.INTEGER);
        constraint.validate(0l);
    }

    @Test(expected = ConstraintViolationException.class)
    public void testVersionInRangeConstraintFailed() throws Exception {
        InRangeConstraint constraint = new InRangeConstraint();
        constraint.setInRange(Lists.newArrayList("1.6", "1.8"));
        constraint.initialize(ToscaType.VERSION);
        constraint.validate(VersionUtil.parseVersion("1.9"));
    }

    @Test
    public void testLengthMinLengthMaxLengthConstraintSatisfied() throws Exception {
        LengthConstraint lengthConstraint = new LengthConstraint();
        lengthConstraint.setLength(5);
        lengthConstraint.validate("abcde");

        MinLengthConstraint minLengthConstraint = new MinLengthConstraint();
        minLengthConstraint.setMinLength(5);
        minLengthConstraint.validate("abcdef");

        MaxLengthConstraint maxLengthConstraint = new MaxLengthConstraint();
        maxLengthConstraint.setMaxLength(5);
        maxLengthConstraint.validate("abc");
    }

    @Test(expected = ConstraintViolationException.class)
    public void testLengthConstraintFailed() throws Exception {
        LengthConstraint lengthConstraint = new LengthConstraint();
        lengthConstraint.setLength(4);
        lengthConstraint.validate("abcde");
    }

    @Test(expected = ConstraintViolationException.class)
    public void testMinLengthConstraintFailed() throws Exception {
        MinLengthConstraint lengthConstraint = new MinLengthConstraint();
        lengthConstraint.setMinLength(5);
        lengthConstraint.validate("abc");
    }

    @Test(expected = ConstraintViolationException.class)
    public void testMaxLengthConstraintFailed() throws Exception {
        MaxLengthConstraint lengthConstraint = new MaxLengthConstraint();
        lengthConstraint.setMaxLength(5);
        lengthConstraint.validate("abcdefgh");
    }

    @Test
    public void testGeConstraintSatisfied() throws Exception {
        GreaterOrEqualConstraint constraint = new GreaterOrEqualConstraint();
        constraint.setGreaterOrEqual("2");
        constraint.initialize(ToscaType.INTEGER);
        constraint.validate(2l);
        constraint.validate(3l);
        constraint.validate(Long.MAX_VALUE);
        constraint.setGreaterOrEqual("5");
        constraint.initialize(ToscaType.INTEGER);
        constraint.validate(5l);
        constraint.validate(6l);
        constraint.validate(7l);
        constraint.setGreaterOrEqual("1.6.1");
        constraint.initialize(ToscaType.VERSION);
        constraint.validate(VersionUtil.parseVersion("1.6.1"));
        constraint.validate(VersionUtil.parseVersion("1.6.1.1"));
    }

    @Test(expected = ConstraintViolationException.class)
    public void testGeConstraintFailed() throws Exception {
        GreaterOrEqualConstraint constraint = new GreaterOrEqualConstraint();
        constraint.setGreaterOrEqual("1.6.1");
        constraint.initialize(ToscaType.VERSION);
        constraint.validate(VersionUtil.parseVersion("1.6.0"));
    }

    @Test(expected = ConstraintValueDoNotMatchPropertyTypeException.class)
    public void testGeConstraintStringFailed() throws Exception {
        GreaterOrEqualConstraint constraint = new GreaterOrEqualConstraint();
        constraint.setGreaterOrEqual("b");
        constraint.initialize(ToscaType.STRING);
        constraint.validate("a");
    }

    @Test(expected = ConstraintViolationException.class)
    public void testGeConstraintVersionFailed() throws Exception {
        GreaterOrEqualConstraint constraint = new GreaterOrEqualConstraint();
        constraint.setGreaterOrEqual("2");
        constraint.initialize(ToscaType.INTEGER);
        constraint.validate(1l);
    }

    @Test
    public void testLeConstraintSatisfied() throws Exception {
        LessOrEqualConstraint constraint = new LessOrEqualConstraint();
        constraint.setLessOrEqual("2");
        constraint.initialize(ToscaType.INTEGER);
        constraint.validate(2l);
        constraint.validate(1l);
        constraint.validate(Long.MIN_VALUE);
        constraint.setLessOrEqual("5");
        constraint.initialize(ToscaType.INTEGER);
        constraint.validate(5l);
        constraint.validate(4l);
        constraint.validate(3l);
    }

    @Test(expected = ConstraintViolationException.class)
    public void testLeConstraintFailed() throws Exception {
        LessOrEqualConstraint constraint = new LessOrEqualConstraint();
        constraint.setLessOrEqual("2");
        constraint.initialize(ToscaType.INTEGER);
        constraint.validate(3l);
    }

    @Test
    public void testGtConstraintSatisfied() throws Exception {
        GreaterThanConstraint constraint = new GreaterThanConstraint();
        constraint.setGreaterThan("2");
        constraint.initialize(ToscaType.INTEGER);
        constraint.validate(3l);
        constraint.validate(Long.MAX_VALUE);
        constraint.setGreaterThan("5");
        constraint.initialize(ToscaType.INTEGER);
        constraint.validate(6l);
        constraint.validate(7l);
    }

    @Test(expected = ConstraintViolationException.class)
    public void testGtConstraintFailed() throws Exception {
        GreaterThanConstraint constraint = new GreaterThanConstraint();
        constraint.setGreaterThan("2");
        constraint.initialize(ToscaType.INTEGER);
        constraint.validate(1l);
    }

    @Test
    public void testLtConstraintSatisfied() throws Exception {
        LessThanConstraint constraint = new LessThanConstraint();
        constraint.setLessThan("2");
        constraint.initialize(ToscaType.INTEGER);
        constraint.validate(1l);
        constraint.validate(Long.MIN_VALUE);
        constraint.setLessThan("5");
        constraint.initialize(ToscaType.INTEGER);
        constraint.validate(4l);
        constraint.validate(3l);
    }

    @Test(expected = ConstraintViolationException.class)
    public void testLtConstraintFailed() throws Exception {
        LessThanConstraint constraint = new LessThanConstraint();
        constraint.setLessThan("2");
        constraint.initialize(ToscaType.INTEGER);
        constraint.validate(3l);
    }

    @Test
    public void testRegexpConstraintSatisfied() throws Exception {
        PatternConstraint constraint = new PatternConstraint();
        constraint.initialize(ToscaType.STRING);
        constraint.setPattern("\\d+");
        constraint.validate("123456");
    }

    @Test(expected = ConstraintViolationException.class)
    public void testRegexpConstraintFailed() throws Exception {
        PatternConstraint constraint = new PatternConstraint();
        constraint.setPattern("\\d+");
        constraint.validate("1234er56");
    }

    @Test
    public void testValidValuesConstraintStatisfied() throws Exception {
        ValidValuesConstraint constraint = new ValidValuesConstraint();
        constraint.setValidValues(Lists.newArrayList("1", "2", "3", "4"));
        constraint.initialize(ToscaType.INTEGER);
        constraint.validate(1l);
        constraint.validate(2l);
        constraint.validate(3l);
        constraint.validate(4l);
    }

    @Test(expected = ConstraintViolationException.class)
    public void testValidValuesConstraintFailed() throws Exception {
        ValidValuesConstraint constraint = new ValidValuesConstraint();
        constraint.setValidValues(Lists.newArrayList("1", "2", "3", "4"));
        constraint.initialize(ToscaType.INTEGER);
        constraint.validate(5l);
    }
}
