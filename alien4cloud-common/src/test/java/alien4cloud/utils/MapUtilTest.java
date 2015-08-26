package alien4cloud.utils;

import java.util.Map;

import org.junit.Assert;
import org.junit.Test;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

public class MapUtilTest {

    @Test
    public void test() {
        Map<String, Object> test = Maps.newHashMap();
        test.put("toto", "tata");
        test.put("titi", Maps.newHashMap());
        test.put("fcuk", Lists.newArrayList("xx", "yy"));
        test.put("fcok", Lists.newArrayList("zz", "tt").toArray());
        ((Map<String, Object>) test.get("titi")).put("toctoc", "tactac");
        Assert.assertEquals("tactac", MapUtil.get(test, "titi.toctoc"));
        Assert.assertEquals("yy", MapUtil.get(test, "fcuk.1"));
        Assert.assertEquals("zz", MapUtil.get(test, "fcok.0"));
        Assert.assertNull(MapUtil.get(test, "path.not.exist"));
        Assert.assertNull(MapUtil.get(test, "fcuk.fcuk"));
    }
}
