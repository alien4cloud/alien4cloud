package alien4cloud.rest.utils;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

public class PatchUtilTest {

    private TestObject testObject;

    @Before
    public void setUp(){
        testObject = new TestObject();
        testObject.map.put("a","1");
        testObject.map.put("b","2");
    }

    @Test
    public void check_a_patch_can_update_a_value() throws Exception {
        Map<String, String> patch = new HashMap<String, String>() {{
            put("a", "2");
        }};

        PatchUtil.setMap(testObject.getMap(), patch, true);

        assertThat(testObject.getMap()).containsEntry("a","2").containsEntry("b", "2");
    }

    @Test
    public void check_a_patch_can_add_an_entry() throws Exception {
        Map<String, String> patch = new HashMap<String, String>() {{
            put("c", "3");
        }};

        PatchUtil.setMap(testObject.getMap(), patch, true);

        assertThat(testObject.getMap()).containsEntry("a","1").containsEntry("b", "2").containsEntry("c", "3");
    }

    @Test
    public void check_a_patch_can_remove_a_value_1() throws Exception {
        Map<String, String> patch = new HashMap<String, String>() {{
            put("a", null);
        }};

        PatchUtil.setMap(testObject.getMap(), patch, true);

        assertThat(testObject.getMap()).hasSize(1);
        assertThat(testObject.getMap()).containsEntry("b", "2");
    }

    @Test
    public void check_a_patch_can_remove_a_value_2() throws Exception {
        Map<String, String> patch = new HashMap<String, String>() {{
            put("a", "null");
        }};

        PatchUtil.setMap(testObject.getMap(), patch, true);

        assertThat(testObject.getMap()).hasSize(1);
        assertThat(testObject.getMap()).containsEntry("b", "2");
    }

    @Test
    public void check_an_update_override_everything() throws Exception {
        Map<String, String> patch = new HashMap<String, String>() {{
            put("d", "4");
        }};

        PatchUtil.setMap(testObject.getMap(), patch, false);

        assertThat(testObject.getMap()).hasSize(1);
        assertThat(testObject.getMap()).containsEntry("d", "4");
    }

    @Getter
    @Setter
    @ToString
    private static class TestObject {
        private Map<String, String> map = new HashMap<>();
    }

}