package alien4cloud.common;

import javax.annotation.Resource;

import org.elasticsearch.common.collect.Maps;
import org.elasticsearch.index.query.QueryBuilders;
import org.springframework.stereotype.Service;

import alien4cloud.dao.IGenericSearchDAO;
import alien4cloud.exception.NotFoundException;
import alien4cloud.model.common.IMetaProperties;
import alien4cloud.model.common.MetaPropConfiguration;
import alien4cloud.tosca.properties.constraints.ConstraintUtil.ConstraintInformation;
import alien4cloud.tosca.properties.constraints.exception.ConstraintValueDoNotMatchPropertyTypeException;
import alien4cloud.tosca.properties.constraints.exception.ConstraintViolationException;
import alien4cloud.utils.services.ConstraintPropertyService;

/**
 * Service that manage meta-property for resources with meta-properties.
 */
@Service
public class MetaPropertiesService {
    @Resource(name = "alien-es-dao")
    private IGenericSearchDAO alienDAO;
    @Resource
    private ConstraintPropertyService constraintPropertyService;

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
            constraintPropertyService.checkPropertyConstraint(key, value, propertyDefinition);
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
}
