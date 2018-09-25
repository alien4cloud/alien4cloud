package alien4cloud.rest.common;

import alien4cloud.common.MetaPropertiesService;
import alien4cloud.dao.IGenericSearchDAO;
import alien4cloud.model.common.IMetaProperties;
import alien4cloud.rest.internal.model.PropertyRequest;
import alien4cloud.rest.model.RestErrorBuilder;
import alien4cloud.rest.model.RestErrorCode;
import alien4cloud.rest.model.RestResponse;
import alien4cloud.rest.model.RestResponseBuilder;
import alien4cloud.security.AuthorizationUtil;
import alien4cloud.security.model.ApplicationRole;
import alien4cloud.tosca.properties.constraints.ConstraintUtil;
import lombok.extern.slf4j.Slf4j;
import org.alien4cloud.tosca.exceptions.ConstraintValueDoNotMatchPropertyTypeException;
import org.alien4cloud.tosca.exceptions.ConstraintViolationException;
import org.springframework.web.bind.annotation.RequestBody;

import javax.annotation.Resource;
import javax.inject.Inject;

@Slf4j
public abstract class AbstractMetaPropertyController<T extends IMetaProperties> {
    @Resource(name = "alien-es-dao")
    protected IGenericSearchDAO alienDAO;
    @Inject
    private MetaPropertiesService metaPropertiesService;

    /**
     * Update or create a property for an object that handle meta-properties.
     *
     * @param id id of the target
     * @param propertyRequest property request
     * @return information on the constraint
     * @throws ConstraintValueDoNotMatchPropertyTypeException
     * @throws ConstraintViolationException
     */
    protected RestResponse<ConstraintUtil.ConstraintInformation> upsertProperty(String id,
            @RequestBody PropertyRequest propertyRequest)
                    throws ConstraintViolationException, ConstraintValueDoNotMatchPropertyTypeException {

        IMetaProperties target = getTarget(id);
        try {
            metaPropertiesService.upsertMetaProperty(target, propertyRequest.getDefinitionId(), propertyRequest.getValue());
        } catch (ConstraintViolationException e) {
            log.error("Constraint violation error for property <" + propertyRequest.getDefinitionId() + "> with value <"
                    + propertyRequest.getValue() + ">", e);
            return RestResponseBuilder.<ConstraintUtil.ConstraintInformation> builder().data(e.getConstraintInformation())
                    .error(RestErrorBuilder.builder(RestErrorCode.PROPERTY_CONSTRAINT_VIOLATION_ERROR).message(e.getMessage()).build()).build();
        } catch (ConstraintValueDoNotMatchPropertyTypeException e) {
            log.error("Constraint value violation error for property <" + e.getConstraintInformation().getName() + "> with value <"
                    + e.getConstraintInformation().getValue() + "> and type <" + e.getConstraintInformation().getType() + ">", e);
            return RestResponseBuilder.<ConstraintUtil.ConstraintInformation> builder().data(e.getConstraintInformation())
                    .error(RestErrorBuilder.builder(RestErrorCode.PROPERTY_TYPE_VIOLATION_ERROR).message(e.getMessage()).build()).build();
        }
        return RestResponseBuilder.<ConstraintUtil.ConstraintInformation> builder().data(null).error(null).build();
    }

    protected abstract T getTarget(String id);

}