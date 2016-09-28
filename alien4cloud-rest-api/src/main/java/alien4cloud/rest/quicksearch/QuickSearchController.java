package alien4cloud.rest.quicksearch;

import java.util.Map;
import java.util.Set;

import javax.annotation.Resource;

import org.apache.commons.lang3.ArrayUtils;
import org.elasticsearch.index.query.FilterBuilder;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import alien4cloud.dao.ElasticSearchDAO;
import alien4cloud.dao.IGenericSearchDAO;
import alien4cloud.dao.model.FetchContext;
import alien4cloud.dao.model.GetMultipleDataResult;
import alien4cloud.model.application.Application;
import org.alien4cloud.tosca.model.types.NodeType;
import alien4cloud.rest.model.BasicSearchRequest;
import alien4cloud.rest.model.RestResponse;
import alien4cloud.rest.model.RestResponseBuilder;
import alien4cloud.security.AuthorizationUtil;
import alien4cloud.security.model.Role;

import com.google.common.collect.Sets;
import io.swagger.annotations.ApiOperation;

/**
 * Handle Quick Search requests.
 *
 * @author 'Igor Ngouagna'
 */
@RestController
@RequestMapping({"/rest/quicksearch", "/rest/v1/quicksearch", "/rest/latest/quicksearch"})
public class QuickSearchController {
    @Resource(name = "alien-es-dao")
    private IGenericSearchDAO alienDAO;

    @ApiOperation(value = "Search for applications or tosca elements in ALIEN's repository.")
    @RequestMapping(method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("isAuthenticated()")
    public RestResponse<GetMultipleDataResult> search(@RequestBody BasicSearchRequest requestObject) {

        Set<String> authoIndexes = Sets.newHashSet();
        Set<Class<?>> classes = Sets.newHashSet();

        // First phase : COMPONENTS search, needed role Role.COMPONENTS_BROWSER or Role.ADMIN
        if (AuthorizationUtil.hasOneRoleIn(Role.COMPONENTS_BROWSER)) {
            authoIndexes.add(ElasticSearchDAO.TOSCA_ELEMENT_INDEX);
            classes.add(NodeType.class);
        }

        GetMultipleDataResult searchResultComponents = searchByType(requestObject, authoIndexes, classes, null, null);

        // Second phase : APPLICATION search (with rights filter) or with the Role.ADMIN
        authoIndexes.clear();
        classes.clear();
        authoIndexes.add(Application.class.getSimpleName().toLowerCase());
        classes.add(Application.class);

        // Adding filters to get only authorized applications
        // only filter on users roles on the application if the current user is not an ADMIN
        FilterBuilder authorizationFilter = AuthorizationUtil.getResourceAuthorizationFilters();

        GetMultipleDataResult<?> searchResultApplications = searchByType(requestObject, authoIndexes, classes, null, authorizationFilter);

        // Final merge result : COMPONENTS + APPLICATIONS
        GetMultipleDataResult searchResult = new GetMultipleDataResult();
        searchResult.setQueryDuration(searchResultComponents.getQueryDuration() + searchResultApplications.getQueryDuration());
        searchResult.setTypes(ArrayUtils.addAll(searchResultComponents.getTypes(), searchResultApplications.getTypes()));
        searchResult.setData(ArrayUtils.addAll(searchResultComponents.getData(), searchResultApplications.getData()));
        searchResult.setTotalResults(searchResultComponents.getTotalResults() + searchResultApplications.getTotalResults());

        return RestResponseBuilder.<GetMultipleDataResult> builder().data(searchResult).build();
    }

    private GetMultipleDataResult searchByType(BasicSearchRequest requestObject, Set<String> authoIndexes, Set<Class<?>> classes,
            Map<String, String[]> filters, FilterBuilder filterBuilder) {

        String[] indices = authoIndexes.toArray(new String[authoIndexes.size()]);
        if (indices.length == 0) {
            return new GetMultipleDataResult();
        }
        Class<?>[] classesArray = classes.toArray(new Class<?>[classes.size()]);

        GetMultipleDataResult searchResult = alienDAO.search(indices, classesArray, requestObject.getQuery(), filters, filterBuilder,
                FetchContext.QUICK_SEARCH, requestObject.getFrom(), requestObject.getSize());

        return searchResult;
    }
}
