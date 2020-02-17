package alien4cloud.purge;

import alien4cloud.dao.ESGenericSearchDAO;
import alien4cloud.dao.ElasticSearchDAO;

import alien4cloud.dao.MonitorESDAO;
import alien4cloud.dao.model.GetMultipleDataResult;
import alien4cloud.events.DeploymentUndeployedEvent;
import alien4cloud.model.deployment.Deployment;
import alien4cloud.model.deployment.DeploymentTopology;
import alien4cloud.model.deployment.DeploymentUnprocessedTopology;

import alien4cloud.model.runtime.Execution;
import alien4cloud.model.runtime.Task;
import alien4cloud.model.runtime.WorkflowStepInstance;
import alien4cloud.paas.model.*;
import com.google.common.collect.Maps;
import lombok.extern.slf4j.Slf4j;

import org.apache.commons.collections4.MultiValuedMap;
import org.apache.commons.collections4.multimap.HashSetValuedHashMap;
import org.apache.lucene.util.NamedThreadFactory;
import org.elasticsearch.action.bulk.BulkRequestBuilder;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.index.query.*;
import org.elasticsearch.mapping.MappingBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;

import java.util.*;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

@Slf4j
@Component
public class PurgeService {

    private final ScheduledExecutorService executorService = Executors.newScheduledThreadPool(1, new NamedThreadFactory("a4c-purge-service"));

    @Inject
    ElasticSearchDAO commonDao;

    @Inject
    MonitorESDAO monitorDao;

    /**
     * Time to wait between the end of an execution and the start of the next execution.
     */
    @Value("${purge.period:86400}")
    private Integer period;

    /**
     * Maximum number of deployments to purge at each purge execution.
     */
    @Value("${purge.threshold:1000}")
    private Integer threshold;

    /**
     * TTL in hours : the TTL since the endDate of the deployment (when endDate is defined).
     */
    @Value("${purge.ttl:240}")
    private Integer ttl;

    /**
     * The maximum number of IDs to delete a each bulk delete request.
     */
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

        private Map<Class<?>, Integer> stats = Maps.newHashMap();

        protected PurgeContext(BiConsumer<Class<?>,Collection<String>> callback) {
            this.callback = callback;
        }

        public <T> void add(Class<T> clazz, String id) {
            map.put(clazz,id);

            Collection<String> ids = map.get(clazz);
            if (ids.size() == batch) {
                callback.accept(clazz,ids);
                addStat(clazz, ids.size());
                map.remove(clazz);
            }
        }

        public void flush() {
            for (Class<?> clazz : map.keySet()) {
                Collection<String> ids = map.get(clazz);
                callback.accept(clazz, ids);
                addStat(clazz, ids.size());
            }
            map.clear();
        }

        public void addStat(Class<?> clazz, int nbOfItems) {
            Integer count = stats.get(clazz);
            if (count == null) {
                stats.put(clazz, nbOfItems);
            } else {
                stats.put(clazz, count + nbOfItems);
            }
        }

        public void logStats() {
            stats.forEach((aClass, count) -> {
                ESGenericSearchDAO dao = getDaoFor(aClass);
                String indexName = dao.getIndexForType(aClass);
                log.info("=> Purge has deleted {} entries in index {}", count, indexName);
            });

        }

    }

    private void run() {
        try {
            PurgeContext context = new PurgeContext(this::bulkDelete);

            long date = Calendar.getInstance().getTime().getTime() - (ttl * 60 * 60 * 1000);

            log.info("=> Start of deployments purge : TTL is {} (expiration date for this run : {}), considering {} deployments at each run, bulk delete batch size is {}", ttl, new Date(date), threshold, batch);

            FilterBuilder customFilter = FilterBuilders.boolFilter()
                    .mustNot(FilterBuilders.missingFilter("endDate"))
                    .must(FilterBuilders.rangeFilter("endDate").lte(date));

            GetMultipleDataResult<Deployment> deployments = commonDao.search(Deployment.class, null, null, customFilter, null, 0, threshold);

            if (log.isDebugEnabled()) {
                log.debug("=> {} deployments candidates for purge", deployments.getData().length);
            }
            if (deployments.getData().length >0) {

                for (Deployment d : deployments.getData()) {
                    fullPurge(context, d.getId());
                }

                // Flush pending deletes
                context.flush();

                // Delete deployments
                Collection<String> ids = Arrays.stream(deployments.getData()).map(Deployment::getId).collect(Collectors.toList());

                bulkDelete(Deployment.class,ids);
                context.addStat(Deployment.class, ids.size());

                if (log.isDebugEnabled()) {
                    log.debug("=> End of deployments purge");
                }
                context.logStats();

            }

        } catch(RuntimeException e) {
            log.error("Exception during purge:",e);
        }
    }

    private void fullPurge(PurgeContext context,String id) {
        if (log.isDebugEnabled()) {
            log.debug("=> Purging Deployment {}", id);
        }

        purge(context,id);

        purge(context,id,Task.class);
        purge(context,id,Execution.class);

        purge(context,id,PaaSDeploymentLog.class);
    }

    private void purge(PurgeContext context, String id) {
        context.add(DeploymentTopology.class,id);
        context.add(DeploymentUnprocessedTopology.class,id);


        purge(context, id, WorkflowStepInstance.class);
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

        // Index : DeploymentMonitorEvent
        // ------------------------------
        // PaaSWorkflowMonitorEvent
        // PaaSInstancePersistentResourceMonitorEvent
        // PaaSWorkflowStepMonitorEvent
        // PaaSMessageMonitorEvent
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

        if (log.isDebugEnabled()) {
            log.debug("BULK DELETE ON {}/{} -> {} items", indexName, typeName, ids.size());
        }

        for (String id : ids) {
            bulkRequestBuilder.add(dao.getClient().prepareDelete(indexName,typeName,id));
        }

        bulkRequestBuilder.execute().actionGet();
    }

    @EventListener
    private void onDeploymentUndeployed(DeploymentUndeployedEvent event) {
        PurgeContext context = new PurgeContext(this::bulkDelete);

        if (log.isDebugEnabled()) {
            log.debug("Cleaning Deployment {}", event.getDeploymentId());
        }

        purge(context,event.getDeploymentId());

        // Flush pending deletes
        context.flush();
    }
}
