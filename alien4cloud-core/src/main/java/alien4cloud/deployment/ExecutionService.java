package alien4cloud.deployment;

import alien4cloud.dao.IGenericSearchDAO;
import alien4cloud.dao.model.FacetedSearchResult;
import alien4cloud.model.runtime.Execution;
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
     * Get an execution by it's id.
     */
    public Execution getExecution(String executionId) {
        return alienDao.findById(Execution.class, executionId);
    }

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

    /**
     * For a given deployment, get the last known execution.
     */
    public Execution getLastExecution(String deploymentId) {
        FilterBuilder filterBuilder = buildFilters(deploymentId);
        FacetedSearchResult<Execution> executions = alienDao.facetedSearch(Execution.class, "", null, filterBuilder, null, 0, 1, "startDate", true);
        if (executions.getData() != null && executions.getData().length > 0) {
            return executions.getData()[0];
        } else {
            return null;
        }
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