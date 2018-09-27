package alien4cloud.rest.tags;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import javax.annotation.Resource;
import javax.inject.Inject;
import javax.validation.ConstraintViolation;
import javax.validation.Validator;

import alien4cloud.metaproperty.MetaPropertyEvent;
import alien4cloud.metaproperty.MetaPropertySearchContextBuilder;
import alien4cloud.model.service.ServiceResource;
import alien4cloud.rest.model.FilteredSearchRequest;
import org.alien4cloud.tosca.model.types.NodeType;
import org.apache.commons.collections.MapUtils;
import org.elasticsearch.index.query.ExistsFilterBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import alien4cloud.audit.annotation.Audit;
import alien4cloud.dao.IGenericSearchDAO;
import alien4cloud.dao.model.FacetedSearchResult;
import alien4cloud.dao.model.GetMultipleDataResult;
import alien4cloud.exception.AlreadyExistException;
import alien4cloud.exception.NotFoundException;
import alien4cloud.model.application.Application;
import alien4cloud.model.common.IMetaProperties;
import alien4cloud.model.common.MetaPropConfiguration;
import alien4cloud.model.orchestrators.locations.Location;
import alien4cloud.rest.model.RestErrorBuilder;
import alien4cloud.rest.model.RestErrorCode;
import alien4cloud.rest.model.RestResponse;
import alien4cloud.rest.model.RestResponseBuilder;
import alien4cloud.tosca.container.validation.ToscaSequence;
import alien4cloud.utils.MapUtil;
import alien4cloud.utils.PropertyUtil;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping({ "/rest/metaproperties", "/rest/v1/metaproperties", "/rest/latest/metaproperties" })
@Api
public class TagConfigurationController {

    @Resource
    private Validator validator;

    @Resource(name = "alien-es-dao")
    private IGenericSearchDAO dao;

    @Inject
    private ApplicationEventPublisher publisher;

    /**
     * Throw a not found exception if we found another tag with the same name, the same target
     * but a different id
     *
     * @param name
     * @param target
     * @param id
     */
    private void ensureNameAndTypeUnicity(final String name, final String target, final String id) {
        @SuppressWarnings("unchecked")
        GetMultipleDataResult<MetaPropConfiguration> result = dao.facetedSearch(MetaPropConfiguration.class, null,
                MapUtil.newHashMap(new String[] { "name", "target" }, new String[][] { new String[] { name }, new String[] { target } }), null, 0, 5);

        if (result.getData().length > 0 && !result.getData()[0].getId().equals(id)) {
            log.debug("An other tag with name [ {} ] and target [ {} ] already exists", target, name);
            throw new AlreadyExistException("An other tag with name " + name + " and target " + target + " already exists");
        }
    }

    @ApiOperation(value = "Save tag configuration.")
    @RequestMapping(method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAuthority('ADMIN')")
    @Audit
    public RestResponse<TagConfigurationSaveResponse> saveConfiguration(@RequestBody MetaPropConfiguration configuration) {
        if (configuration.getName() != null && configuration.getTarget() != null) {
            ensureNameAndTypeUnicity(configuration.getName(), configuration.getTarget(), configuration.getId());
        }
        if (configuration.getId() == null) {
            configuration.setId(UUID.randomUUID().toString());
        }
        Set<ConstraintViolation<MetaPropConfiguration>> violations = validator.validate(configuration, ToscaSequence.class);
        if (violations != null && !violations.isEmpty()) {
            Set<TagConfigurationValidationError> violationDTOs = Sets.newHashSet();
            for (ConstraintViolation<MetaPropConfiguration> violation : violations) {
                TagConfigurationValidationError violationDTO = new TagConfigurationValidationError();
                violationDTO.setError(violation.getMessage());
                violationDTO.setPath(violation.getPropertyPath().toString());
                violationDTOs.add(violationDTO);
            }
            return RestResponseBuilder.<TagConfigurationSaveResponse> builder().data(new TagConfigurationSaveResponse(null, violationDTOs))
                    .error(RestErrorBuilder.builder(RestErrorCode.PROPERTY_CONSTRAINT_VIOLATION_ERROR).message("Invalid tag configuration").build()).build();
        } else {
            dao.save(configuration);

            // for each resource in the targeted class, add the new meta property with default value
            switch (configuration.getTarget().toString()) {
            case "application":
                addMetaPropertyToResources(Application.class, dao, configuration);
                break;
            case "location":
                addMetaPropertyToResources(Location.class, dao, configuration);
                break;
            // TODO : case environment
            default:
                break;
            }

            publisher.publishEvent(new MetaPropertyEvent(this));

            return RestResponseBuilder.<TagConfigurationSaveResponse> builder().data(new TagConfigurationSaveResponse(configuration.getId(), null)).build();
        }
    }

    /**
     * save the meta properties of the element
     * 
     * @param mpClass class of resources to update with new meta property
     * @param dao IGenericSearchDAO object to access ES
     * @param configuration configuration of new meta property
     */
    private <T extends IMetaProperties> void addMetaPropertyToResources(Class<T> mpClass, IGenericSearchDAO dao, MetaPropConfiguration configuration) {
        GetMultipleDataResult<T> result = dao.find(mpClass, null, Integer.MAX_VALUE);
        for (T element : result.getData()) {
            if (element.getMetaProperties() == null) {
                element.setMetaProperties(Maps.<String, String> newHashMap());
            }
            PropertyUtil.setScalarDefaultValueOrNull(element.getMetaProperties(), configuration.getId(), configuration.getDefault());
            dao.save(element);
            log.debug("Adding meta property [ {} ] to a resource of type [ {} ] ", configuration.getName(), element.getClass());
        }
    }

    private <T extends IMetaProperties> void removeMetaPropertyFromResources(Class<T> mpClass, IGenericSearchDAO dao, MetaPropConfiguration configuration) {

        // here we make an ES query to search only for objects that has a value for this meta-property
        ExistsFilterBuilder existsFilterBuilder = new ExistsFilterBuilder("metaProperties." + configuration.getId());
        List<T> result = dao.customFilterAll(mpClass, existsFilterBuilder);

        if (result != null) {
            for (T element : result) {
                if (MapUtils.isNotEmpty(element.getMetaProperties())) {
                    element.getMetaProperties().remove(configuration.getId());
                }
                dao.save(element);
                log.debug("Adding meta property [ {} ] to a resource of type [ {} ] ", configuration.getName(), element.getClass());
            }
        }
    }

    @ApiOperation(value = "Search for tag configurations registered in ALIEN.")
    @RequestMapping(value = "/search", method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("isAuthenticated()")
    public RestResponse<FacetedSearchResult> search(@RequestBody FilteredSearchRequest request) {
        FacetedSearchResult result = dao.facetedSearch(MetaPropConfiguration.class, request.getQuery(), request.getFilters(), null, request.getFrom(),
                request.getSize());
        return RestResponseBuilder.<FacetedSearchResult> builder().data(result).build();
    }

    @ApiOperation(value = "Remove tag configuration.")
    @RequestMapping(value = "/{tagConfigurationId}", method = RequestMethod.DELETE, produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAuthority('ADMIN')")
    @Audit
    public RestResponse<Void> removeConfiguration(@PathVariable String tagConfigurationId) {
        MetaPropConfiguration configuration = dao.findById(MetaPropConfiguration.class, tagConfigurationId);
        if (configuration == null) {
            throw new NotFoundException("Configuration is not found");
        }
        switch (configuration.getTarget().toString()) {
        case "application":
            removeMetaPropertyFromResources(Application.class, dao, configuration);
            break;
        case "location":
            removeMetaPropertyFromResources(Location.class, dao, configuration);
            break;
        case "component":
            removeMetaPropertyFromResources(NodeType.class, dao, configuration);
            break;
        case "service":
            removeMetaPropertyFromResources(ServiceResource.class, dao, configuration);
            break;
        // TODO : case environment
        default:
            break;
        }
        dao.delete(MetaPropConfiguration.class, tagConfigurationId);
        return RestResponseBuilder.<Void> builder().build();
    }

    @ApiOperation(value = "Get tag configuration.")
    @RequestMapping(value = "/{tagConfigurationId}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAuthority('ADMIN')")
    public RestResponse<MetaPropConfiguration> getConfiguration(@PathVariable String tagConfigurationId) {
        MetaPropConfiguration configuration = dao.findById(MetaPropConfiguration.class, tagConfigurationId);
        if (configuration == null) {
            throw new NotFoundException("Configuration is not found");
        }
        return RestResponseBuilder.<MetaPropConfiguration> builder().data(configuration).build();
    }
}
