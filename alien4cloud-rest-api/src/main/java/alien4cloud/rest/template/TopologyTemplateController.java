package alien4cloud.rest.template;

import java.util.Map;

import javax.annotation.Resource;
import javax.validation.Valid;

import lombok.extern.slf4j.Slf4j;

import org.elasticsearch.index.query.QueryBuilders;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import alien4cloud.audit.annotation.Audit;
import alien4cloud.dao.IGenericSearchDAO;
import alien4cloud.dao.model.FacetedSearchResult;
import alien4cloud.exception.AlreadyExistException;
import alien4cloud.exception.InvalidArgumentException;
import alien4cloud.model.templates.TopologyTemplate;
import alien4cloud.model.topology.Topology;
import alien4cloud.rest.component.SearchRequest;
import alien4cloud.rest.model.RestResponse;
import alien4cloud.rest.model.RestResponseBuilder;
import alien4cloud.rest.topology.TopologyService;
import alien4cloud.security.AuthorizationUtil;
import alien4cloud.security.Role;
import alien4cloud.topology.TopologyServiceCore;
import alien4cloud.utils.ReflectionUtil;

import com.google.common.collect.Maps;
import com.mangofactory.swagger.annotations.ApiIgnore;
import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.ApiParam;
import com.wordnik.swagger.annotations.Authorization;

/**
 * Handle topology templates
 *
 * @author mourouvi
 *
 */
@Slf4j
@RestController
@RequestMapping("/rest/templates")
public class TopologyTemplateController {

    @Resource(name = "alien-es-dao")
    private IGenericSearchDAO alienDAO;
    @Resource
    private TopologyService topologyService;
    @Resource
    private TopologyServiceCore topologyServiceCore;

    /**
     * Create a new {@link TopologyTemplate}
     *
     * @param createTopologyTemplateRequest
     * @return
     */
    @ApiOperation(value = "Create a new empty topology template.", notes = "Returns the id of the created topology template. Role required [ ARCHITECT ]")
    @RequestMapping(value = "/topology", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    @Audit
    public RestResponse<String> create(@RequestBody @Valid CreateTopologyTemplateRequest createTopologyTemplateRequest) {
        AuthorizationUtil.checkHasOneRoleIn(Role.ARCHITECT);

        // Create the new topology linked to the topology template
        Topology topology = new Topology();

        TopologyTemplate template = topologyServiceCore.createTopologyTemplate(topology, checkNameUnicity(createTopologyTemplateRequest.getName()),
                createTopologyTemplateRequest.getDescription());

        log.info("Created topology template <{}>", template.getId());

        return RestResponseBuilder.<String> builder().data(template.getId()).build();
    }

    private String checkNameUnicity(String name) {
        if (alienDAO.count(TopologyTemplate.class, QueryBuilders.termQuery("name", name)) > 0) {
            log.debug("A topology template with the given name <{}> already exists.", name);
            // an application already exist with the given name.
            throw new AlreadyExistException("A topology template with the given name already exists.");
        }
        log.debug("Topology template name <{}> is unique.", name);
        return name;
    }

    /**
     * Retrieve an existing {@link TopologyTemplate}
     *
     * @param topologyTemplateId
     * @return
     */
    @ApiOperation(value = "Retrieve a topology template from it's id.", notes = "Returns a topology template with it's details. Role required [ Role.ARCHITECT ]")
    @RequestMapping(value = "/topology/{topologyTemplateId}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public RestResponse<TopologyTemplate> get(@PathVariable String topologyTemplateId) {

        AuthorizationUtil.checkHasOneRoleIn(Role.ARCHITECT);
        TopologyTemplate template = topologyService.getOrFailTopologyTemplate(topologyTemplateId);
        return RestResponseBuilder.<TopologyTemplate> builder().data(template).build();
    }

    /**
     * Search for and topology template
     *
     * @param searchRequest The element that contains criterias for search operation.
     * @return A rest response that contains a {@link FacetedSearchResult} containing applications.
     */
    @ApiOperation(value = "Search for topology templates.", notes = "Returns a search result with that contains topology templates matching the request. A application is returned only if the connected user has at least one application role in [ ARCHITECT ]")
    @RequestMapping(value = "/topology/search", method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public RestResponse<FacetedSearchResult> search(@RequestBody SearchRequest searchRequest) {
        Map<String, String[]> filters = searchRequest.getFilters();
        if (filters == null) {
            filters = Maps.newHashMap();
        }
        FacetedSearchResult searchResult = alienDAO.facetedSearch(TopologyTemplate.class, searchRequest.getQuery(), filters, null, searchRequest.getFrom(),
                searchRequest.getSize());

        return RestResponseBuilder.<FacetedSearchResult> builder().data(searchResult).build();
    }

    /**
     * Delete an existing {@link TopologyTemplate}
     *
     * @param topologyTemplateId
     * 
     * @return
     */
    @ApiOperation(value = "Delete a topology template given an id. Alse delete the related topology", notes = "Role required [ Role.ARCHITECT ]")
    @RequestMapping(value = "/topology/{topologyTemplateId}", method = RequestMethod.DELETE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Audit
    public RestResponse<Void> delete(@PathVariable String topologyTemplateId) {
        AuthorizationUtil.checkHasOneRoleIn(Role.ARCHITECT);
        TopologyTemplate topologyTemplate = topologyService.getOrFailTopologyTemplate(topologyTemplateId);
        alienDAO.delete(TopologyTemplate.class, topologyTemplate.getId());
        alienDAO.delete(Topology.class, topologyTemplate.getTopologyId());
        return RestResponseBuilder.<Void> builder().build();
    }

    /**
     * update an existing {@link TopologyTemplate}
     *
     * @param topologyTemplateId
     * @param updateTopologyTemplateRequest A Json of field:value to update in the topology template
     * @return
     */
    @ApiOperation(value = "Update an user by merging the userUpdateRequest into the existing user", authorizations = { @Authorization("ARCHITECT") })
    @RequestMapping(value = "/topology/{topologyTemplateId}", method = RequestMethod.PUT, produces = MediaType.APPLICATION_JSON_VALUE)
    @Audit
    public RestResponse<Void> update(@ApiParam(value = " Id of the topology template", required = true) @PathVariable String topologyTemplateId,
            @ApiIgnore @RequestBody UpdateTopologyTemplateRequest updateTopologyTemplateRequest) {
        AuthorizationUtil.checkHasOneRoleIn(Role.ARCHITECT);
        TopologyTemplate topologyTemplate = topologyService.getOrFailTopologyTemplate(topologyTemplateId);
        String currentTemplateName = topologyTemplate.getName();
        ReflectionUtil.mergeObject(updateTopologyTemplateRequest, topologyTemplate);
        if (topologyTemplate.getName() == null || topologyTemplate.getName().isEmpty()) {
            throw new InvalidArgumentException("Topology template's name cannot be null or empty");
        }
        if (!currentTemplateName.equals(topologyTemplate.getName())) {
            checkNameUnicity(topologyTemplate.getName());
        }
        alienDAO.save(topologyTemplate);
        return RestResponseBuilder.<Void> builder().build();
    }

}
