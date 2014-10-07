package alien4cloud.utils;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.Set;

import org.junit.Assert;
import org.junit.Test;

import alien4cloud.paas.IPaaSProviderFactory;
import alien4cloud.paas.PaaSProviderFactoriesService;
import alien4cloud.plugin.IPluginLinker;
import alien4cloud.utils.ReflectionUtil;

public class ReflectionUtilTest {
    @Test
    public void testGetAllGenericInterfaces() throws IOException {
        Set<Type> types = ReflectionUtil.getAllGenericInterfaces(TestClass.class);
        Assert.assertEquals(2, types.size());
    }

    @Test
    public void testGetGenericArgumentTypeDirect() {
        Class<?> clazz = ReflectionUtil.getGenericArgumentType(TestDirectPluginLinker.class, IPluginLinker.class, 0);
        Assert.assertEquals(String.class, clazz);
    }

    @Test
    public void testGetGenericArgumentTypeSubClass() {
        Class<?> clazz = ReflectionUtil.getGenericArgumentType(PaaSProviderFactoriesService.class, IPluginLinker.class, 0);
        Assert.assertEquals(IPaaSProviderFactory.class, clazz);
    }
}