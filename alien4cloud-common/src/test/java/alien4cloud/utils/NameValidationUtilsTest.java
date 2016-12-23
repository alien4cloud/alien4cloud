package alien4cloud.utils;

import java.util.List;
import java.util.concurrent.Callable;

import org.junit.Assert;
import org.junit.Test;

import com.google.common.collect.Lists;

import alien4cloud.exception.InvalidNameException;

public class NameValidationUtilsTest {

    @Test
    public void isValidTest() {
        Assert.assertTrue(NameValidationUtils.isValid("Compute"));
        Assert.assertTrue(NameValidationUtils.isValid("Compute_2"));

        Assert.assertFalse(NameValidationUtils.isValid("Computé"));
        Assert.assertFalse(NameValidationUtils.isValid("Compute-2"));
        Assert.assertFalse(NameValidationUtils.isValid("Compute.unix"));
        Assert.assertFalse(NameValidationUtils.isValid("Compute 2"));
    }

    @Test
    public void validateDefaultTest() {
        NameValidationUtils.validate("owner", "Compute");
        NameValidationUtils.validate("owner", "Compute_2");

        List<String> invalids = Lists.newArrayList("Computé", "Compute-2", "Compute.unix", "Compute 2");
        invalids.forEach(invalid -> {
            expectException(new Callable() {
                @Override
                public Object call() throws Exception {
                    NameValidationUtils.validate("owner", invalid);
                    return null;
                }
            }, InvalidNameException.class);
        });
    }

    @Test
    public void validateNodeNameTest() {
        NameValidationUtils.validateNodeName("Compute");
        NameValidationUtils.validateNodeName("Compute_2");

        List<String> invalids = Lists.newArrayList("Computé", "Compute-2", "Compute.unix", "Compute 2");
        invalids.forEach(invalid -> {
            expectException(new Callable() {
                @Override
                public Object call() throws Exception {
                    NameValidationUtils.validateNodeName(invalid);
                    return null;
                }
            }, InvalidNameException.class);
        });
    }

    @Test
    public void validateApplicationNameTest() {
        NameValidationUtils.validateApplicationName("app");
        NameValidationUtils.validateApplicationName("app_2");
        NameValidationUtils.validateApplicationName("app 2");
        NameValidationUtils.validateApplicationName("app é 2");

        List<String> invalids = Lists.newArrayList("app/hehe", "app\\dqdv", "app\\/dqdv");
        invalids.forEach(invalid -> {
            expectException(new Callable() {
                @Override
                public Object call() throws Exception {
                    NameValidationUtils.validateApplicationName(invalid);
                    return null;
                }
            }, InvalidNameException.class);
        });
    }

    @Test
    public void validateApplicationIdTest() {
        NameValidationUtils.validateApplicationId("app");
        NameValidationUtils.validateApplicationId("app_2");

        List<String> invalids = Lists.newArrayList("appè", "App-2", "App.unix", "App 2");
        invalids.forEach(invalid -> {
            expectException(new Callable() {
                @Override
                public Object call() throws Exception {
                    NameValidationUtils.validateApplicationId(invalid);
                    return null;
                }
            }, InvalidNameException.class);
        });
    }

    private void expectException(Callable callable, Class<? extends Throwable> expected) {
        try {
            callable.call();
        } catch (Exception e) {
            Assert.assertEquals(expected, e.getClass());
            return;
        }
        Assert.fail("Expected exception of type <" + expected + "> but got nothing");
    }

}
