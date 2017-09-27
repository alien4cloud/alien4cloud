package org.alien4cloud.tosca.variable;

import alien4cloud.model.application.Application;
import alien4cloud.model.common.Tag;
import alien4cloud.model.orchestrators.locations.Location;
import com.google.common.collect.ImmutableMap;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;

public class AlienContextVariablesTest {

    private AlienContextVariables alienContextVariables;

    @Before
    public void setUp() throws Exception {
        alienContextVariables = new AlienContextVariables();
        Application app = new Application();
        app.setTags(Arrays.asList(new Tag("tagName", "tagValue"), new Tag("yolo", "oloy")));
        app.setMetaProperties(ImmutableMap.of("meta1", "meta1 value", "meta2", "meta2 value"));
        alienContextVariables.setApplication(app);

        Location location = new Location();
        location.setMetaProperties(ImmutableMap.of("loc_meta1", "meta1 value", "loc_meta2", "meta2 value"));
        alienContextVariables.setLocation(location);
    }

    @Test
    public void check_tags_can_be_resolved() throws Exception {
        Assertions.assertThat(alienContextVariables.getProperty("a4c.application.tags.yolo")).isEqualTo("oloy");
        Assertions.assertThat(alienContextVariables.getProperty("a4c.application.tags.tagName")).isEqualTo("tagValue");
    }

    @Test
    public void check_app_meta_can_be_resolved() throws Exception {
        Assertions.assertThat(alienContextVariables.getProperty("a4c.meta1")).isEqualTo("meta1 value");
        Assertions.assertThat(alienContextVariables.getProperty("a4c.meta2")).isEqualTo("meta2 value");
    }

    @Test
    public void check_loc_meta_can_be_resolved() throws Exception {
        Assertions.assertThat(alienContextVariables.getProperty("a4c.loc_meta1")).isEqualTo("meta1 value");
        Assertions.assertThat(alienContextVariables.getProperty("a4c.loc_meta2")).isEqualTo("meta2 value");
    }

}