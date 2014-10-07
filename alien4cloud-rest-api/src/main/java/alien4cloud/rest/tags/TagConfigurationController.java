package alien4cloud.rest.tags;

import java.util.Set;
import java.util.UUID;

import javax.annotation.Resource;
import javax.validation.ConstraintViolation;
import javax.validation.Validator;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import alien4cloud.dao.IGenericSearchDAO;
import alien4cloud.dao.model.FacetedSearchResult;
import alien4cloud.exception.NotFoundException;
import alien4cloud.model.common.MetaPropConfiguration;
import alien4cloud.rest.component.SearchRequest;
import alien4cloud.rest.model.RestErrorBuilder;
import alien4cloud.rest.model.RestErrorCode;
import alien4cloud.rest.model.RestResponse;
import alien4cloud.rest.model.RestResponseBuilder;

import com.google.common.collect.Sets;
import com.wordnik.swagger.annotations.ApiOperation;

@RestController
@RequestMapping("/rest/tagconfigurations")
public class TagConfigurationController {

    @Resource
    private Validator validator;

    @Resource(name = "alien-es-dao")
    private IGenericSearchDAO dao;

    @ApiOperation(value = "Save tag configuration.")
    @RequestMapping(method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
    public RestResponse<TagConfigurationSaveResponse> saveConfiguration(@RequestBody MetaPropConfiguration configuration) {
        if (configuration.getId() == null) {
            // Save or update
            configuration.setId(UUID.randomUUID().toString());
        }
        Set<ConstraintViolation<MetaPropConfiguration>> violations = validator.validate(configuration);
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
            return RestResponseBuilder.<TagConfigurationSaveResponse> builder().data(new TagConfigurationSaveResponse(configuration.getId(), null)).build();
        }
    }

    @ApiOperation(value = "Search for tag configurations registered in ALIEN.")
    @RequestMapping(value = "/search", method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public RestResponse<FacetedSearchResult> search(@RequestBody SearchRequest request) {
        FacetedSearchResult result = dao.facetedSearch(MetaPropConfiguration.class, request.getQuery(), request.getFilters(), null, request.getFrom(),
                request.getSize());
        return RestResponseBuilder.<FacetedSearchResult> builder().data(result).build();
    }

    @ApiOperation(value = "Remove tag configuration.")
    @RequestMapping(value = "/{tagConfigurationId}", method = RequestMethod.DELETE, produces = MediaType.APPLICATION_JSON_VALUE)
    public RestResponse<Void> removeConfiguration(@PathVariable String tagConfigurationId) {
        dao.delete(MetaPropConfiguration.class, tagConfigurationId);
        return RestResponseBuilder.<Void> builder().build();
    }

    @ApiOperation(value = "Get tag configuration.")
    @RequestMapping(value = "/{tagConfigurationId}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public RestResponse<MetaPropConfiguration> getConfiguration(@PathVariable String tagConfigurationId) {
        MetaPropConfiguration configuration = dao.findById(MetaPropConfiguration.class, tagConfigurationId);
        if (configuration == null) {
            throw new NotFoundException("Configuration is not found");
        }
        return RestResponseBuilder.<MetaPropConfiguration> builder().data(configuration).build();
    }
}
