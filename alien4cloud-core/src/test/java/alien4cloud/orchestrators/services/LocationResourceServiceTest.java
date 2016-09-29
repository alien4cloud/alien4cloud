package alien4cloud.orchestrators.services;

import java.util.List;
import java.util.UUID;

import javax.annotation.Resource;

import org.elasticsearch.index.query.QueryBuilders;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import alien4cloud.dao.IGenericSearchDAO;
import alien4cloud.model.orchestrators.locations.Location;
import alien4cloud.model.orchestrators.locations.LocationResourceTemplate;
import org.alien4cloud.tosca.model.templates.NodeTemplate;
import alien4cloud.orchestrators.locations.services.ILocationResourceService;
import alien4cloud.orchestrators.plugin.ILocationResourceAccessor;

import com.google.common.collect.Lists;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("classpath:application-context-test.xml")
public class LocationResourceServiceTest {

    @Resource(name = "alien-es-dao")
    private IGenericSearchDAO alienDAO;
    @Resource(name = "location-resource-service")
    private ILocationResourceService locationResourceService;

    private static final String LOCATION_ID = "location";
    private static final String UNCONFIGURED_LOCATION_ID = "unconfigured-location";
    private static final String CONFIGURED_TYPE = "configured_type";
    private static final int TYPE_CONFIGURED_ELEMENTS = 20;
    private static final int LOCATION_CONFIGURED_ELEMENTS = 100;
    private static final String UNCONFIGURED_TYPE = "unconfigured_type";

    @Before
    public void init() {
        alienDAO.delete(LocationResourceTemplate.class, QueryBuilders.matchAllQuery());
        Assert.assertEquals(0, alienDAO.count(LocationResourceTemplate.class, QueryBuilders.matchAllQuery()));
        // initialize random data for multiple locations
        initLocation(LOCATION_ID);
    }

    private void initLocation(String locationId) {
        Location location = new Location();
        location.setId(LOCATION_ID);
        alienDAO.save(location);
        location.setId(UNCONFIGURED_LOCATION_ID);
        alienDAO.save(location);
        for (int i = 0; i < 5; i++) {
            for (int j = 0; j < TYPE_CONFIGURED_ELEMENTS; j++) {
                String type = i == 0 ? CONFIGURED_TYPE : CONFIGURED_TYPE + "." + i;
                NodeTemplate template = new NodeTemplate();
                template.setType(type);
                template.setName("template_" + i + "_" + j);

                LocationResourceTemplate lrt = new LocationResourceTemplate();
                lrt.setId(UUID.randomUUID().toString());
                lrt.setLocationId(locationId);
                lrt.setGenerated(false);
                lrt.setEnabled(true);
                lrt.setService(false);
                lrt.setTemplate(template);
                lrt.setTypes(Lists.newArrayList(template.getType()));

                alienDAO.save(lrt);
            }
        }
    }

    @Test
    public void getResources() {
        ILocationResourceAccessor accessor = locationResourceService.accessor(LOCATION_ID);
        List<LocationResourceTemplate> resources = accessor.getResources();
        Assert.assertEquals(LOCATION_CONFIGURED_ELEMENTS, resources.size());
    }

    @Test
    public void getResourcesForUnconfiguredLocationShouldReturnEmptyList() {
        ILocationResourceAccessor accessor = locationResourceService.accessor(UNCONFIGURED_LOCATION_ID);
        List<LocationResourceTemplate> resources = accessor.getResources();
        Assert.assertEquals(0, resources.size());
    }

    @Test
    public void getResourcesOfType() {
        ILocationResourceAccessor accessor = locationResourceService.accessor(LOCATION_ID);
        List<LocationResourceTemplate> resources = accessor.getResources(CONFIGURED_TYPE);
        Assert.assertEquals(TYPE_CONFIGURED_ELEMENTS, resources.size());
    }

    @Test
    public void getResourcesOfTypeForUnconfiguredTypeShouldReturnEmptyList() {
        ILocationResourceAccessor accessor = locationResourceService.accessor(LOCATION_ID);
        List<LocationResourceTemplate> resources = accessor.getResources(UNCONFIGURED_TYPE);
        Assert.assertEquals(0, resources.size());
    }

    @Test
    public void getResourcesOfTypeForUnconfiguredLocationShouldReturnEmptyList() {
        ILocationResourceAccessor accessor = locationResourceService.accessor(UNCONFIGURED_LOCATION_ID);
        List<LocationResourceTemplate> resources = accessor.getResources(CONFIGURED_TYPE);
        Assert.assertEquals(0, resources.size());
    }
}
