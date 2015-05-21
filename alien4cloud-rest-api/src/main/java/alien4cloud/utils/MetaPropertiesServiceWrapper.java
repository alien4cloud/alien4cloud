package alien4cloud.utils;

import javax.annotation.Resource;

import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;

import alien4cloud.common.MetaPropertiesService;
import alien4cloud.dao.IGenericSearchDAO;
import alien4cloud.model.common.IMetaProperties;
import alien4cloud.rest.model.RestErrorBuilder;
import alien4cloud.rest.model.RestErrorCode;
import alien4cloud.rest.model.RestResponse;
import alien4cloud.rest.model.RestResponseBuilder;
import alien4cloud.tosca.properties.constraints.ConstraintUtil.ConstraintInformation;
import alien4cloud.tosca.properties.constraints.exception.ConstraintValueDoNotMatchPropertyTypeException;
import alien4cloud.tosca.properties.constraints.exception.ConstraintViolationException;

@Service
@Slf4j
public class MetaPropertiesServiceWrapper {
    @Resource(name = "alien-es-dao")
    private IGenericSearchDAO alienDAO;
    @Resource
    private MetaPropertiesService metaPropertiesService;

    /**
     * Wrap all exceptions thrown by the service and return the appropriate RestResponse
     *
     * @param resource
     * @param key
     * @param value
     * @return a RestResponse that contains the error
     */
    public RestResponse<ConstraintInformation> upsertMetaProperty(IMetaProperties resource, String key, String value) {
        ConstraintInformation constraintInformation = null;
        try {
            constraintInformation = metaPropertiesService.upsertMetaProperty(resource, key, value);
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
        return RestResponseBuilder.<ConstraintInformation> builder().data(constraintInformation).error(null).build();
    }
}
