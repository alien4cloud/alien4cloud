package alien4cloud.deployment;

import alien4cloud.dao.IGenericSearchDAO;
import alien4cloud.dao.model.FacetedSearchResult;
import alien4cloud.model.runtime.Task;
import alien4cloud.model.runtime.WorkflowStepInstance;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.index.query.FilterBuilder;
import org.elasticsearch.index.query.FilterBuilders;
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
        FilterBuilder filterBuilder = buildFilters(executionId);
        return alienDao.facetedSearch(WorkflowStepInstance.class, query, null, filterBuilder, null, from, size);
    }

    private FilterBuilder buildFilters(String executionId) {
        FilterBuilder filterBuilder = null;
        if (executionId != null) {
            FilterBuilder filter = FilterBuilders.termFilter("executionId", executionId);
            filterBuilder = filterBuilder == null ? filter : FilterBuilders.andFilter(filter, filterBuilder);
        }
        return filterBuilder;
    }

}