package alien4cloud.utils;

import java.lang.reflect.Field;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;

import alien4cloud.model.cloud.CloudImageFlavor;
import alien4cloud.model.cloud.CloudImageRequirement;
import alien4cloud.model.cloud.MatchedNetworkTemplate;
import alien4cloud.model.cloud.NetworkTemplate;
import alien4cloud.model.common.Tag;
import alien4cloud.model.components.CSARDependency;
import alien4cloud.model.components.CapabilityDefinition;
import alien4cloud.model.components.Csar;
import alien4cloud.model.components.IndexedNodeType;
import alien4cloud.model.components.PropertyDefinition;
import alien4cloud.model.components.RequirementDefinition;
import alien4cloud.model.components.ScalarPropertyValue;
import alien4cloud.model.components.constraints.EqualConstraint;
import alien4cloud.model.components.constraints.GreaterOrEqualConstraint;
import alien4cloud.model.components.constraints.GreaterThanConstraint;
import alien4cloud.model.components.constraints.InRangeConstraint;
import alien4cloud.model.components.constraints.LengthConstraint;
import alien4cloud.model.components.constraints.LessOrEqualConstraint;
import alien4cloud.model.components.constraints.LessThanConstraint;
import alien4cloud.model.components.constraints.MaxLengthConstraint;
import alien4cloud.model.components.constraints.MinLengthConstraint;
import alien4cloud.model.components.constraints.PatternConstraint;
import alien4cloud.model.components.constraints.ValidValuesConstraint;

/**
 * Most of the equals and hashcode methods are generated using Lombok.
 * Jacoco is not able to ignore theses in coverage reports (feature request is done there).
 * This class performs equals and hashcode tests in order to avoid wrong results in coverage reports.
 */
public class EqualsAndHashCodeAutoTest {

    @Test
    public void testEqualsAndHashCode() throws IllegalAccessException, NoSuchFieldException, InstantiationException {
        doTest(IndexedNodeType.class, "elementId", "archiveName", "archiveVersion");
        doTest(Tag.class, "name");
        // TODO is it really what we want to check on CloudImageFlavor ?
        doTest(CloudImageFlavor.class, "id", "numCPUs", "diskSize", "memSize");
        doTest(CloudImageRequirement.class, "numCPUs", "diskSize", "memSize");
        doTest(NetworkTemplate.class, "ipVersion", "cidr", "gatewayIp", "id");
        doTest(CSARDependency.class, "name", "version");
        doTest(CapabilityDefinition.class, "id");
        doTest(Csar.class, "name", "version");
        doTest(PropertyDefinition.class, "type", "required", "description", "defaultValue", "constraints");
        doTest(RequirementDefinition.class, "id");
        doTest(ScalarPropertyValue.class, "value");

        doTest(EqualConstraint.class, "equal");
        doTest(GreaterOrEqualConstraint.class, "greaterOrEqual");
        doTest(GreaterThanConstraint.class, "greaterThan");
        doTest(InRangeConstraint.class, "inRange");
        doTest(LengthConstraint.class, "length");
        doTest(LessOrEqualConstraint.class, "lessOrEqual");
        doTest(LessThanConstraint.class, "lessThan");
        doTest(MaxLengthConstraint.class, "maxLength");
        doTest(MinLengthConstraint.class, "minLength");
        doTest(PatternConstraint.class, "pattern");
        doTest(ValidValuesConstraint.class, "validValues");
    }

    private void doTest(Class<?> clazz, String... fieldNames) throws IllegalAccessException, InstantiationException, NoSuchFieldException {
        Object instance = clazz.newInstance();
        Object equalInstance = clazz.newInstance();
        String wrongTypeInstance = "";

        Assert.assertFalse(instance.equals(null));
        Assert.assertTrue(instance.equals(equalInstance));
        Assert.assertFalse(instance.equals(wrongTypeInstance));

        // generate field values for each field we need two different values (to test equals and not equals).
        Object[][] allFieldValues = new Object[fieldNames.length][];
        for (int i = 0; i < fieldNames.length; i++) {
            Field field = ReflectionUtil.getDeclaredField(clazz, fieldNames[i]);
            if (ReflectionUtil.isPrimitiveOrWrapperOrString(field.getType())) {
                allFieldValues[i] = new Object[] { "0", "1" };
            } else if (!field.getType().isInterface()) {
                allFieldValues[i] = new Object[] { field.getType().newInstance(), field.getType().newInstance() };
            } else {
                allFieldValues[i] = new Object[] { null, null };
            }
        }

        Object[][] testFieldValues = new Object[allFieldValues.length][];
        // try every field associations
        doTest(clazz, allFieldValues, testFieldValues, fieldNames, 0, allFieldValues.length, 0);
    }

    private void doTest(Class<?> clazz, Object[][] allFieldValues, Object[][] testFieldValues, String[] fieldNames, int start, int end, int index)
            throws IllegalAccessException, InstantiationException {
        if (index == fieldNames.length) {
            Object instance = clazz.newInstance();
            Object equalInstance = clazz.newInstance();
            Object otherInstance = clazz.newInstance();
            BeanWrapper instanceWrapper = new BeanWrapperImpl(instance);
            BeanWrapper equalInstanceWrapper = new BeanWrapperImpl(equalInstance);
            BeanWrapper otherInstanceWrapper = new BeanWrapperImpl(otherInstance);

            // set field values
            for (int i = 0; i < fieldNames.length; i++) {
                String fieldName = fieldNames[i];
                if (clazz.equals(PropertyDefinition.class) && fieldNames[i].equals("defaultValue")) {
                    fieldName = "default";
                }
                instanceWrapper.setPropertyValue(fieldName, testFieldValues[i][0]);
                equalInstanceWrapper.setPropertyValue(fieldName, testFieldValues[i][0]);
                otherInstanceWrapper.setPropertyValue(fieldName, testFieldValues[i][1]);
            }
            // Two instances that are the same must have the same hashcode (but two different instance may have the same hashcode).
            Assert.assertEquals(instance.hashCode(), equalInstance.hashCode());
            Assert.assertTrue(instance.equals(equalInstance));
        } else {
            // recursive prepare a set of field values to test so we test all combinations
            for (int i = start; i < end && end - i + 1 >= fieldNames.length - index; i++) {
                testFieldValues[index] = allFieldValues[i];
                doTest(clazz, allFieldValues, testFieldValues, fieldNames, i + 1, end, index + 1);
            }
        }
    }
}