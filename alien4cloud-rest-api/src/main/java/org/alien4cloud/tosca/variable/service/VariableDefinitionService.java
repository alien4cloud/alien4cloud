package org.alien4cloud.tosca.variable.service;

import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.alien4cloud.tosca.model.Csar;
import org.alien4cloud.tosca.variable.EnvironmentVariableDefinitionDTO;
import org.alien4cloud.tosca.variable.QuickFileStorageService;
import org.alien4cloud.tosca.variable.model.Variable;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import com.google.common.collect.Lists;

import alien4cloud.application.ApplicationEnvironmentService;
import alien4cloud.application.ApplicationService;
import alien4cloud.model.application.Application;
import alien4cloud.model.application.ApplicationEnvironment;
import alien4cloud.security.AuthorizationUtil;

@Service
public class VariableDefinitionService {

    @Inject
    private ApplicationService applicationService;
    @Inject
    private ApplicationEnvironmentService applicationEnvironmentService;
    @Inject
    private QuickFileStorageService quickFileStorageService;

    public List<EnvironmentVariableDefinitionDTO> getInEnvironmentScope(String varName, String applicationId, String envId) {
        Application application = applicationService.getOrFail(applicationId);
        if (StringUtils.isBlank(envId)) {
            return Arrays.stream(applicationEnvironmentService.getAuthorizedByApplicationId(applicationId)).map(env -> getVariableDef(varName, env))
                    .collect(Collectors.toList());
        } else {
            ApplicationEnvironment env = applicationEnvironmentService.getOrFail(envId);
            AuthorizationUtil.checkAuthorizationForEnvironment(application, env);
            return Lists.newArrayList(getVariableDef(varName, env));
        }
    }

    public Variable getInApplicationScope(String varName, String applicationId) {
        applicationService.checkAndGetApplication(applicationId);
        Properties properties = quickFileStorageService.loadApplicationVariables(applicationId);
        return new Variable(varName, properties.getProperty(varName));
    }

    private EnvironmentVariableDefinitionDTO getVariableDef(String varName, ApplicationEnvironment env) {
        Properties variables = quickFileStorageService.loadEnvironmentVariables(Csar.createId(env.getApplicationId(), env.getTopologyVersion()), env.getId());
        EnvironmentVariableDefinitionDTO dto = new EnvironmentVariableDefinitionDTO();
        dto.setEnvironmentId(env.getId());
        dto.setEnvironmentName(env.getName());

        dto.setVariable(new Variable(varName, variables.getProperty(varName)));

        return dto;
    }
}
