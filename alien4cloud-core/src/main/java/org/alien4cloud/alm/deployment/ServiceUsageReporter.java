package org.alien4cloud.alm.deployment;

import static alien4cloud.dao.FilterUtil.fromKeyValueCouples;

import java.util.Arrays;

import javax.annotation.Resource;
import javax.inject.Inject;

import org.alien4cloud.alm.service.events.ServiceUsageRequestEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import alien4cloud.application.ApplicationEnvironmentService;
import alien4cloud.dao.IGenericSearchDAO;
import alien4cloud.dao.model.GetMultipleDataResult;
import alien4cloud.model.application.ApplicationEnvironment;
import alien4cloud.model.common.Usage;
import alien4cloud.model.deployment.Deployment;

/**
 * Service responsible to report services usage in deployments.
 */
@Service
public class ServiceUsageReporter {
    @Resource(name = "alien-es-dao")
    private IGenericSearchDAO alienDAO;
    @Inject
    private ApplicationEnvironmentService environmentService;

    @EventListener
    private void reportServiceUsage(ServiceUsageRequestEvent serviceChangedEvent) {
        GetMultipleDataResult<Deployment> usageResult = alienDAO.buildQuery(Deployment.class)
                .setFilters(fromKeyValueCouples("endDate", null, "serviceResourceIds", serviceChangedEvent.getServiceId())).prepareSearch()
                .search(0, 10000);
        if (usageResult.getTotalResults() > 0) {
            Usage[] usages = Arrays.stream(usageResult.getData()).map(deployment -> {
                ApplicationEnvironment environment = environmentService.getOrFail(deployment.getEnvironmentId());
                String usageName = "App (" + deployment.getSourceName() + "), Env (" + environment.getName() + ")";
                return new Usage(usageName, "Deployment", deployment.getId(), null);
            }).toArray(Usage[]::new);
            serviceChangedEvent.addUsages(usages);
        }
    }
}
