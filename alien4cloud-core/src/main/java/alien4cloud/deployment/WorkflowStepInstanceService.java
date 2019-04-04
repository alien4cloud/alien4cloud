package alien4cloud.deployment;

import alien4cloud.dao.IGenericSearchDAO;
import alien4cloud.dao.model.FacetedSearchResult;
import alien4cloud.model.runtime.Task;
import alien4cloud.model.runtime.WorkflowStepInstance;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/**
 * Manage task operations.
 */
@Service
@Slf4j
public class WorkflowStepInstanceService {
    @Resource(name = "alien-es-dao")
    private IGenericSearchDAO alienDao;

    /**
     * Search stepTasks. See below to known with filters are supported.
     *
     * @param query Query text.
     * @param executionId Id of the execution for which to get stepTasks.
     * @param from Query from the given index.
     * @param size Maximum number of results to retrieve.
     * @return the stepTasks with pagination
     */
    public FacetedSearchResult<WorkflowStepInstance> searchInstances(String query, String executionId, int from, int size) {
        QueryBuilder filterBuilder = buildFilters(executionId);
        return alienDao.facetedSearch(WorkflowStepInstance.class, query, null, filterBuilder, null, from, size);
    }

    private QueryBuilder buildFilters(String executionId) {
        QueryBuilder filterBuilder = null;
        if (executionId != null) {
            QueryBuilder filter = QueryBuilders.termQuery("executionId", executionId);
            //filterBuilder = filterBuilder == null ? filter : QueryBuilders.andQuery(filter, filterBuilder);
            filterBuilder = QueryBuilders.boolQuery().must(filter);
        }
        return filterBuilder;
    }

}
