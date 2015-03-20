package alien4cloud.rest.audit;

import javax.annotation.Resource;

import org.elasticsearch.index.query.FilterBuilder;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import alien4cloud.audit.model.AuditTrace;
import alien4cloud.dao.IGenericSearchDAO;
import alien4cloud.dao.model.FacetedSearchResult;
import alien4cloud.rest.component.SearchRequest;
import alien4cloud.rest.model.RestResponse;
import alien4cloud.rest.model.RestResponseBuilder;
import alien4cloud.security.AuthorizationUtil;

import com.wordnik.swagger.annotations.ApiOperation;

@RestController
@RequestMapping("/rest/audit")
public class AuditController {

    @Resource(name = "alien-es-dao")
    private IGenericSearchDAO alienDAO;

    /**
     * Search for audit trace
     *
     * @param searchRequest The element that contains criterias for search operation.
     * @return A rest response that contains a {@link FacetedSearchResult} containing audit trace.
     */
    @ApiOperation(value = "Search for audit trace", notes = "Returns a search result with that contains auti traces matching the request. Audit search is only accessible to user with role [ ADMIN ]")
    @RequestMapping(value = "/search", method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public RestResponse<FacetedSearchResult> search(@RequestBody SearchRequest searchRequest) {
        FilterBuilder authorizationFilter = AuthorizationUtil.getResourceAuthorizationFilters();
        FacetedSearchResult searchResult = alienDAO.facetedSearch(AuditTrace.class, searchRequest.getQuery(), searchRequest.getFilters(), authorizationFilter,
                null, searchRequest.getFrom(), searchRequest.getSize());
        return RestResponseBuilder.<FacetedSearchResult> builder().data(searchResult).build();
    }

}
