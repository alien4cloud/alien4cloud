package org.alien4cloud.alm.deployment.configuration.services;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import org.alien4cloud.git.LocalGitManager;
import org.springframework.stereotype.Component;

import alien4cloud.common.ResourceUpdateInterceptor;

@Component
public class ResourceUpdateInterceptorForGit {

    @Inject
    private ResourceUpdateInterceptor resourceUpdateInterceptor;

    @Inject
    private LocalGitManager localGitManager;

    @PostConstruct
    public void configure(){
        resourceUpdateInterceptor.getOnNewEnvironment().add(applicationEnvironment -> {
            // create local git if needed
            localGitManager.checkout(applicationEnvironment, applicationEnvironment.getTopologyVersion());
        });

        resourceUpdateInterceptor.getOnEnvironmentTopologyVersionChanged().add(topologyVersionChangedInfo -> {
            // checkout the new branch
            localGitManager.checkout(topologyVersionChangedInfo.getEnvironment(), topologyVersionChangedInfo.getEnvironment().getTopologyVersion());
        });
    }
}
