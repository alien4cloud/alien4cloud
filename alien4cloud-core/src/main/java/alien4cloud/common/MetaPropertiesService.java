package alien4cloud.common;

import javax.annotation.Resource;

import lombok.extern.slf4j.Slf4j;

import org.elasticsearch.common.collect.Maps;
import org.elasticsearch.index.query.QueryBuilders;
import org.springframework.stereotype.Service;

import alien4cloud.dao.IGenericSearchDAO;
import alien4cloud.model.common.IMetaProperties;
import alien4cloud.model.common.MetaPropConfiguration;
import alien4cloud.rest.model.RestErrorBuilder;
import alien4cloud.rest.model.RestErrorCode;
import alien4cloud.rest.model.RestResponse;
import alien4cloud.rest.model.RestResponseBuilder;
import alien4cloud.tosca.properties.constraints.ConstraintUtil.ConstraintInformation;
import alien4cloud.tosca.properties.constraints.exception.ConstraintValueDoNotMatchPropertyTypeException;
import alien4cloud.tosca.properties.constraints.exception.ConstraintViolationException;
import alien4cloud.utils.services.ConstraintPropertyService;

/**
 * Service that manages meta-property for resources with meta-properties.
 */
@Slf4j
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
     */
    public RestResponse<ConstraintInformation> upsertMetaProperty(IMetaProperties resource, String key, String value) {
        MetaPropConfiguration propertyDefinition = alienDAO.findById(MetaPropConfiguration.class, key);
        if (propertyDefinition == null) {
            return RestResponseBuilder
                    .<ConstraintInformation> builder()
                    .error(RestErrorBuilder.builder(RestErrorCode.NOT_FOUND_ERROR)
                            .message("Property update operation failed. Could not find property definition with id <" + propertyDefinition + ">.").build())
                    .build();
        }

        if (resource.getMetaProperties() == null) {
            resource.setMetaProperties(Maps.<String, String> newHashMap());
        }

        if (propertyDefinition.getConstraints() != null) {
            try {
                constraintPropertyService.checkPropertyConstraint(key, value, propertyDefinition);
            } catch (ConstraintViolationException e) {
                log.error("Constraint violation error for property <" + key + "> with value <" + value + ">", e);
                return RestResponseBuilder.<ConstraintInformation> builder().data(e.getConstraintInformation())
                        .error(RestErrorBuilder.builder(RestErrorCode.PROPERTY_CONSTRAINT_VIOLATION_ERROR).message(e.getMessage()).build()).build();
            } catch (ConstraintValueDoNotMatchPropertyTypeException e) {
                log.error("Constraint value d error for property <" + e.getConstraintInformation().getName() + "> with value <"
                        + e.getConstraintInformation().getValue() + "> and type <" + e.getConstraintInformation().getType() + ">", e);
                return RestResponseBuilder.<ConstraintInformation> builder().data(e.getConstraintInformation())
                        .error(RestErrorBuilder.builder(RestErrorCode.PROPERTY_TYPE_VIOLATION_ERROR).message(e.getMessage()).build()).build();
            }
        }

        if (resource.getMetaProperties().containsKey(key)) {
            resource.getMetaProperties().remove(key);
        }
        resource.getMetaProperties().put(key, value);
        alienDAO.save(resource);
        return RestResponseBuilder.<ConstraintInformation> builder().error(null).build();
    }

    /**
     * Remove an existing meta-property.
     *
     * @param resource The resource from which to remove the meta-property.
     * @param key The key/name of the meta-property to remove.
     */
    public void removeMetaProperty(IMetaProperties resource, String key) {
        if (resource.getMetaProperties() != null && resource.getMetaProperties().containsKey(key)) {
            resource.getMetaProperties().remove(key);
            alienDAO.save(resource);
        }
    }

    /**
     * Return the the meta property given its name
     * 
     * @param metaPropertyName
     * @return meta property
     */
    public MetaPropConfiguration getMetaPropertyIdByName(String metaPropertyName) {
        return alienDAO.customFind(MetaPropConfiguration.class, QueryBuilders.termQuery("name", metaPropertyName));
    }
}
