package org.alien4cloud.alm.deployment.configuration.flow;

import alien4cloud.model.application.Application;
import alien4cloud.model.application.ApplicationEnvironment;
import alien4cloud.model.application.ApplicationVersion;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

/**
 * The environment context.
 */
@Getter
@Setter
@AllArgsConstructor
public class EnvironmentContext {
    private Application application;
    private ApplicationEnvironment environment;
}