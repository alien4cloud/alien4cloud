package alien4cloud.utils;

import org.junit.Assert;
import org.junit.Test;

import alien4cloud.orchestrators.plugin.IOrchestratorPluginFactory;
import alien4cloud.orchestrators.services.OrchestratorFactoriesRegistry;
import alien4cloud.plugin.IPluginLinker;

public class ReflectionUtilTest {
    @Test
    public void testGetGenericArgumentTypeDirect() {
        Class<?> clazz = ReflectionUtil.getGenericArgumentType(TestDirectPluginLinker.class, IPluginLinker.class, 0);
        Assert.assertEquals(String.class, clazz);
    }

    @Test
    public void testGetGenericArgumentTypeSubClass() {
        Class<?> clazz = ReflectionUtil.getGenericArgumentType(OrchestratorFactoriesRegistry.class, IPluginLinker.class, 0);
        Assert.assertEquals(IOrchestratorPluginFactory.class, clazz);
    }
}