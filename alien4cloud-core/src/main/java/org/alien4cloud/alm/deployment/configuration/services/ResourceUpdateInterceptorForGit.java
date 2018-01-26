package org.alien4cloud.alm.deployment.configuration.services;

import java.util.Map;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import org.alien4cloud.git.GitLocationDao;
import org.alien4cloud.git.LocalGitManager;
import org.alien4cloud.git.model.GitLocation;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import com.google.common.collect.Maps;

import alien4cloud.application.ApplicationEnvironmentService;
import alien4cloud.common.ResourceUpdateInterceptor;
import alien4cloud.model.application.ApplicationEnvironment;

@Component
public class ResourceUpdateInterceptorForGit {

    @Inject
    private ResourceUpdateInterceptor resourceUpdateInterceptor;

    @Inject
    private LocalGitManager localGitManager;

    @Inject
    private GitLocationDao gitLocationDao;

    @Inject
    private ApplicationEnvironmentService applicationEnvironmentService;

    @PostConstruct
    public void configure() {
        resourceUpdateInterceptor.getOnNewEnvironment().add(applicationEnvironment -> {
            // create local git if needed
            checkoutVersionBranch(applicationEnvironment);
        });

        resourceUpdateInterceptor.getOnEnvironmentTopologyVersionChanged().add(topologyVersionChangedInfo -> {
            // checkout the new branch
            checkoutVersionBranch(topologyVersionChangedInfo.getEnvironment());
        });

        resourceUpdateInterceptor.getOnTopologyVersionUpdated().add(topologyVersionUpdated -> {
            String newBaseVersion = StringUtils.replaceFirst(topologyVersionUpdated.getTo().getVersion(), "-SNAPSHOT", "");
            String fromBaseVersion = StringUtils.replaceFirst(topologyVersionUpdated.getFrom().getVersion(), "-SNAPSHOT", "");

            Map<String, String> branchNameFromTo = Maps.newHashMap();
            topologyVersionUpdated.getFrom().getTopologyVersions().forEach((fromTopologyVersion, applicationTopologyVersion) -> {
                String updatedVersion = StringUtils.replaceFirst(fromTopologyVersion, fromBaseVersion, newBaseVersion);
                if (topologyVersionUpdated.getTo().isReleased()) {
                    updatedVersion = StringUtils.replaceFirst(updatedVersion, "-SNAPSHOT", "");
                }
                branchNameFromTo.put(fromTopologyVersion, updatedVersion);
            });

            ApplicationEnvironment[] environments = applicationEnvironmentService.getByApplicationId(topologyVersionUpdated.getFrom().getApplicationId());
            for (ApplicationEnvironment environment : environments) {
                GitLocation gitLocation = gitLocationDao.findDeploymentSetupLocation(environment.getApplicationId(), environment.getId());
                localGitManager.renameBranches(gitLocation, branchNameFromTo);
            }
        });
    }

    private void checkoutVersionBranch(ApplicationEnvironment environment) {
        GitLocation location = gitLocationDao.findDeploymentSetupLocation(environment.getApplicationId(), environment.getId());
        location.setBranch(environment.getTopologyVersion());
        localGitManager.checkout(location);
    }
}
