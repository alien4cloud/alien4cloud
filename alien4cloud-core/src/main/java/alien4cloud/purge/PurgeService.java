package alien4cloud.purge;

import alien4cloud.dao.ESGenericSearchDAO;
import alien4cloud.dao.ElasticSearchDAO;

import alien4cloud.dao.MonitorESDAO;
import alien4cloud.dao.model.GetMultipleDataResult;
import alien4cloud.model.deployment.Deployment;
import alien4cloud.model.deployment.DeploymentTopology;
import alien4cloud.model.deployment.DeploymentUnprocessedTopology;

import alien4cloud.model.runtime.Execution;
import alien4cloud.model.runtime.Task;
import alien4cloud.paas.model.*;
import lombok.extern.slf4j.Slf4j;

import org.apache.commons.collections4.MultiValuedMap;
import org.apache.commons.collections4.multimap.HashSetValuedHashMap;
import org.elasticsearch.action.bulk.BulkRequestBuilder;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.index.query.*;
import org.elasticsearch.mapping.MappingBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;

import java.util.Arrays;
import java.util.Calendar;

import java.util.Collection;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

@Slf4j
@Component
public class PurgeService {

    private final ScheduledExecutorService executorService = Executors.newScheduledThreadPool(1);

    @Inject
    ElasticSearchDAO commonDao;

    @Inject
    MonitorESDAO monitorDao;

    @Value("${purge.period:86400}")
    private Integer period;

    @Value("${purge.threshold:100}")
    private Integer threshold;

    @Value("${purge.ttl:1800}")
    private Integer ttl;

    @Value("${purge.batch:1000}")
    private Integer batch;

    @PostConstruct
    private void init() {
        executorService.scheduleWithFixedDelay(this::run,period,period,TimeUnit.SECONDS);
    }

    @PreDestroy
    private void destroy() {
        executorService.shutdown();
        try {
            executorService.awaitTermination(5, TimeUnit.MINUTES);
        } catch (InterruptedException e) {
        }
    }

    private class PurgeContext {

        private final BiConsumer<Class<?>,Collection<String>> callback;

        private final MultiValuedMap<Class<?>,String> map = new HashSetValuedHashMap<>();

        protected PurgeContext(BiConsumer<Class<?>,Collection<String>> callback) {
            this.callback = callback;
        }

        public <T> void add(Class<T> clazz, String id) {
            map.put(clazz,id);

            Collection<String> ids = map.get(clazz);
            if (ids.size() == batch) {
                callback.accept(clazz,ids);
                map.remove(clazz);
            }
        }

        public void flush() {
            for (Class<?> clazz : map.keySet()) {
                callback.accept(clazz,map.get(clazz));
            }
            map.clear();
        }
    }

    private void run() {
        PurgeContext context = new PurgeContext(this::bulkDelete);

        long date = Calendar.getInstance().getTime().getTime() - ttl * 1000;

        FilterBuilder customFilter = FilterBuilders.boolFilter()
                .mustNot(FilterBuilders.missingFilter("endDate"))
                .must(FilterBuilders.rangeFilter("endDate").lte(date));

        GetMultipleDataResult<Deployment> deployments = commonDao.search(Deployment.class,null,null,customFilter,null,0,threshold);

        if (deployments.getData().length >0) {
            log.info("=> Begin of deployment purge");

            for (Deployment d : deployments.getData()) {
                purge(context,d.getId());
            }

            // Flush pending deletes
            context.flush();

            // Delete deployments
            Collection<String> ids = Arrays.stream(deployments.getData()).map(Deployment::getId).collect(Collectors.toList());

            bulkDelete(Deployment.class,ids);

            log.info("=> End of deployments purge");
        }
    }

    private void purge(PurgeContext context, String id) {
        log.info("=> Purging Deployment {}",id);

        context.add(DeploymentTopology.class,id);
        context.add(DeploymentUnprocessedTopology.class,id);

        purge(context,id,Task.class);
        purge(context,id,Execution.class);

        purge(context,id,TaskFailedEvent.class);
        purge(context,id,TaskSentEvent.class);
        purge(context,id,TaskStartedEvent.class);
        purge(context,id,TaskCancelledEvent.class);
        purge(context,id,TaskSucceededEvent.class);

        purge(context,id, WorkflowStepStartedEvent.class);
        purge(context,id, WorkflowStepCompletedEvent.class);

        purge(context,id, PaaSWorkflowSucceededEvent.class);
        purge(context,id, PaaSWorkflowStartedEvent.class);
        purge(context,id, PaaSWorkflowCancelledEvent.class);
        purge(context,id, PaaSWorkflowFailedEvent.class);

        purge(context,id,PaaSDeploymentStatusMonitorEvent.class);

        purge(context,id, PaaSInstanceStateMonitorEvent.class);

        purge(context,id,PaaSDeploymentLog.class);
    }

    private <T> void purge(PurgeContext context, String id, Class<T> clazz) {
        ESGenericSearchDAO dao = getDaoFor(clazz);

        String indexName = dao.getIndexForType(clazz);

        // Get Id of documents owned by our deployment
        SearchRequestBuilder searchRequestBuilder = dao.getClient()
                .prepareSearch(indexName)
                .setTypes(dao.getTypesFromClass(clazz))
                .setQuery(QueryBuilders.termQuery("deploymentId",id))
                .setFetchSource(false)
                .setFrom(0).setSize(batch);

        for (;;) {
            SearchResponse response = searchRequestBuilder.execute().actionGet();

            if (response.getHits() == null || response.getHits().getHits() == null) break;

            for (int i = 0; i < response.getHits().getHits().length; i++) {
                context.add(clazz, response.getHits().getHits()[i].getId());
            }

            // Redo the query if we hit the batch
            if (response.getHits().getHits().length < batch) break;
        }
    }

    private <T> ESGenericSearchDAO getDaoFor(Class<T> clazz) {
        if (AbstractMonitorEvent.class.isAssignableFrom(clazz)
                || DeploymentTopology.class.isAssignableFrom(clazz)
                || DeploymentUnprocessedTopology.class.isAssignableFrom(clazz)
                || PaaSDeploymentLog.class.isAssignableFrom(clazz)
            ) {
            return monitorDao;
        } else {
            return commonDao;
        }
    }

    private <T> void bulkDelete(Class<T> clazz,Collection<String> ids) {
        ESGenericSearchDAO dao = getDaoFor(clazz);
        BulkRequestBuilder bulkRequestBuilder = dao.getClient().prepareBulk().setRefresh(true);

        String indexName = dao.getIndexForType(clazz);
        String typeName = MappingBuilder.indexTypeFromClass(clazz);

        log.info("BULK DELETE ON {}/{} -> {} items",indexName,typeName,ids.size());

        for (String id : ids) {
            bulkRequestBuilder.add(dao.getClient().prepareDelete(indexName,typeName,id));
        }

        bulkRequestBuilder.execute().actionGet();
    }
}
