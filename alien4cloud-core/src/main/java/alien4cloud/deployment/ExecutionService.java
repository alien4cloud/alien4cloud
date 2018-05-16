package alien4cloud.deployment;

import alien4cloud.dao.IGenericSearchDAO;
import alien4cloud.dao.model.FacetedSearchResult;
import alien4cloud.model.deployment.Execution;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.index.query.FilterBuilder;
import org.elasticsearch.index.query.FilterBuilders;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/**
 * Manage execution operations on a cloud.
 */
@Service
@Slf4j
public class ExecutionService {
    @Resource(name = "alien-es-dao")
    private IGenericSearchDAO alienDao;

    /**
     * Search executions. See below to known with filters are supported.
     *
     * @param query Query text.
     * @param deploymentId Id of the deployment for which to get executions.
     * @param from Query from the given index.
     * @param size Maximum number of results to retrieve.
     * @return the deployments with pagination
     */
    public FacetedSearchResult searchExecutions(String query, String deploymentId, int from, int size) {
        FilterBuilder filterBuilder = buildFilters(deploymentId);
        return alienDao.facetedSearch(Execution.class, query, null, filterBuilder, null, from, size, "startDate", true);
    }

    private FilterBuilder buildFilters(String deploymentId) {
        FilterBuilder filterBuilder = null;
        if (deploymentId != null) {
            FilterBuilder filter = FilterBuilders.termFilter("deploymentId", deploymentId);
            filterBuilder = filterBuilder == null ? filter : FilterBuilders.andFilter(filter, filterBuilder);
        }
        return filterBuilder;
    }

}