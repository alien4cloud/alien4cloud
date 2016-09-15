package org.alien4cloud.tosca.catalog;

import javax.inject.Inject;

import org.alien4cloud.tosca.catalog.index.TopologyCatalogService;
import org.alien4cloud.tosca.model.templates.Topology;
import org.alien4cloud.tosca.model.types.NodeType;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import alien4cloud.dao.model.FacetedSearchResult;
import alien4cloud.rest.model.FilteredSearchRequest;
import alien4cloud.rest.model.RestResponse;
import alien4cloud.rest.model.RestResponseBuilder;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;

/**
 * Controller to access topology catalog features.
 */
@Slf4j
@RestController
@RequestMapping({ "/rest/catalog/topologies", "/rest/v1/catalog/topologies", "/rest/latest/catalog/topologies" })
public class TopologyCatalogController {

    @Inject
    private TopologyCatalogService catalogService;

    /**
     * Search for topologies in the catalog.
     *
     * @param searchRequest The search request.
     * @return A {@link RestResponse} that contains a {@link FacetedSearchResult} of {@link NodeType}.
     */
    @ApiOperation(value = "Search for components (tosca types) in alien.")
    @RequestMapping(value = "/search", method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyAuthority('ADMIN', 'COMPONENTS_MANAGER', 'COMPONENTS_BROWSER')")
    public RestResponse<FacetedSearchResult<Topology>> search(@RequestBody FilteredSearchRequest searchRequest) {
        FacetedSearchResult<Topology> searchResult = catalogService.search(Topology.class, searchRequest.getQuery(), searchRequest.getSize(),
                searchRequest.getFilters());
        return RestResponseBuilder.<FacetedSearchResult<Topology>> builder().data(searchResult).build();
    }
}