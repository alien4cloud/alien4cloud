package alien4cloud.common;

import java.util.List;
import java.util.Map;

import javax.annotation.Resource;

import alien4cloud.dao.model.GetMultipleDataResult;
import alien4cloud.model.common.MetaPropertyTarget;
import alien4cloud.utils.MapUtil;
import org.alien4cloud.tosca.exceptions.ConstraintValueDoNotMatchPropertyTypeException;
import org.alien4cloud.tosca.exceptions.ConstraintViolationException;
import org.elasticsearch.common.collect.Maps;
import org.springframework.stereotype.Service;

import alien4cloud.dao.IGenericSearchDAO;
import alien4cloud.exception.NotFoundException;
import alien4cloud.model.common.IMetaProperties;
import alien4cloud.model.common.MetaPropConfiguration;
import alien4cloud.tosca.properties.constraints.ConstraintUtil.ConstraintInformation;
import alien4cloud.utils.services.ConstraintPropertyService;

/**
 * Service that manage meta-property for resources with meta-properties.
 */
@Service
public class MetaPropertiesService {
    @Resource(name = "alien-es-dao")
    private IGenericSearchDAO alienDAO;

    /**
     * Add or update a meta-property to a {{IMetaProperties}} resource.
     *
     * @param resource The resource for which to add or update the given meta-property.
     * @param key The id of the meta-property.
     * @param value The value of the meta-property.
     * @return
     * @throws ConstraintValueDoNotMatchPropertyTypeException
     * @throws ConstraintViolationException
     */
    public ConstraintInformation upsertMetaProperty(IMetaProperties resource, String key, String value) throws ConstraintViolationException,
            ConstraintValueDoNotMatchPropertyTypeException {
        MetaPropConfiguration propertyDefinition = alienDAO.findById(MetaPropConfiguration.class, key);
        if (propertyDefinition == null) {
            throw new NotFoundException("Property update operation failed. Could not find property definition with id <" + propertyDefinition + ">.");
        }

        if (value != null) {
            // by convention updateproperty with null value => reset to default if exists
            ConstraintPropertyService.checkPropertyConstraint(key, value, propertyDefinition);
        }

        if (resource.getMetaProperties() == null) {
            resource.setMetaProperties(Maps.<String, String> newHashMap());
        } else if (resource.getMetaProperties().containsKey(key)) {
            resource.getMetaProperties().remove(key);
        }

        resource.getMetaProperties().put(key, value);
        alienDAO.save(resource);
        return null;
    }

    /**
     * Load a map of MetaPropConfiguration from given ids.
     * 
     * @param ids The ids to fetch
     * @return The a map id -> MetaPropConfiguration
     */
    public Map<String, MetaPropConfiguration> getByIds(String[] ids) {
        Map<String, MetaPropConfiguration> configurationMap = Maps.newHashMap();
        List<MetaPropConfiguration> configurations = alienDAO.findByIds(MetaPropConfiguration.class, ids);
        for (MetaPropConfiguration configuration : configurations) {
            configurationMap.put(configuration.getId(), configuration);
        }
        return configurationMap;
    }

    /**
     *
     * @param name
     * @return the
     */
    public String getMetapropertykeyByName(String name, String target) {
        Map<String, String[]> filters = Maps.newHashMap();
        filters.put("name", new String[]{name});
        filters.put("target", new String[]{target.toString()});
        GetMultipleDataResult<MetaPropConfiguration> result = alienDAO.find(MetaPropConfiguration.class, filters, 1);
        if (result.getTotalResults() > 0) {
            return result.getData()[0].getId();
        }
        return null;
    }

    /**
     * @param target
     * @return For the given target type, a map of @{@link MetaPropConfiguration}. The key of the map is the meta property name.
     */
    public Map<String, MetaPropConfiguration> getMetaPropConfigurationsByName(String target) {
        // load all meta properties configurations for target of type components
        Map<String, String[]> filters =  MapUtil.newHashMap(new String[] { "target" }, new String[][] { new String[] { target } });
        GetMultipleDataResult<MetaPropConfiguration> metaPropConfigurations = alienDAO.find(MetaPropConfiguration.class, filters, Integer.MAX_VALUE);
        // a map of metaprop config name -> id
        Map<String, MetaPropConfiguration> metapropsNames = Maps.newHashMap();
        for (MetaPropConfiguration metaPropConfiguration : metaPropConfigurations.getData()) {
            metapropsNames.put(metaPropConfiguration.getName(), metaPropConfiguration);
        }
        return metapropsNames;
    }

}
