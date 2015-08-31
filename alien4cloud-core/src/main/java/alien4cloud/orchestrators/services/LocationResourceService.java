package alien4cloud.orchestrators.services;

import java.util.List;
import java.util.Map;

import javax.annotation.Resource;

import org.springframework.stereotype.Component;

import alien4cloud.dao.IGenericSearchDAO;
import alien4cloud.dao.model.GetMultipleDataResult;
import alien4cloud.model.orchestrators.locations.LocationResourceTemplate;
import alien4cloud.orchestrators.plugin.ILocationResourceAccessor;
import alien4cloud.utils.MapUtil;

import com.google.common.collect.Lists;

/**
 *
 */
@Component
public class LocationResourceService {
    @Resource(name = "alien-es-dao")
    private IGenericSearchDAO alienDAO;

    public ILocationResourceAccessor accessor(final String locationId) {
        return new ILocationResourceAccessor() {
            @Override
            public List<LocationResourceTemplate> getResources() {
                // get all defined resources for this resource.
                GetMultipleDataResult<LocationResourceTemplate> result = alienDAO.find(LocationResourceTemplate.class, getLocationFilter(), Integer.MAX_VALUE);
                if (result.getData() == null) {
                    return Lists.newArrayList();
                }
                return Lists.newArrayList(result.getData());
            }

            @Override
            public List<LocationResourceTemplate> getResources(String type) {
                // Get all types that derives from the current type.
                String[] types = new String[] { type };
                // Get all the location resources templates for the given type.
                Map<String, String[]> filter = getLocationFilter();
                filter.put("template.type", types);
                GetMultipleDataResult<LocationResourceTemplate> result = alienDAO.find(LocationResourceTemplate.class, filter, Integer.MAX_VALUE);
                if (result.getData() == null) {
                    return Lists.newArrayList();
                }
                return Lists.newArrayList(result.getData());
            }

            private Map<String, String[]> getLocationFilter() {
                return MapUtil.newHashMap(new String[] { "locationId" }, new String[][] { new String[] { locationId } });
            }
        };
    }
}
