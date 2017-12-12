package org.alien4cloud.tosca.variable.service;

import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.alien4cloud.tosca.editor.EditorFileService;
import org.alien4cloud.tosca.model.Csar;
import org.alien4cloud.tosca.variable.QuickFileStorageService;
import org.alien4cloud.tosca.variable.ScopeVariableExpressionDTO;
import org.alien4cloud.tosca.variable.model.Variable;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import com.google.common.collect.Lists;

import alien4cloud.application.ApplicationEnvironmentService;
import alien4cloud.application.ApplicationService;
import alien4cloud.model.application.Application;
import alien4cloud.model.application.ApplicationEnvironment;
import alien4cloud.model.application.EnvironmentType;
import alien4cloud.security.AuthorizationUtil;

@Service
public class VariableExpressionService {

    @Inject
    private ApplicationService applicationService;
    @Inject
    private ApplicationEnvironmentService applicationEnvironmentService;
    @Inject
    private QuickFileStorageService quickFileStorageService;
    @Inject
    private EditorFileService editorFileService;

    public List<ScopeVariableExpressionDTO> getInEnvironmentScope(String varName, String applicationId, String topologyVersion, String envId) {
        Application application = applicationService.getOrFail(applicationId);
        if (StringUtils.isBlank(envId)) {
            return Arrays.stream(applicationEnvironmentService.getAuthorizedByApplicationId(applicationId))
                    .map(env -> getVariableDef(varName, Csar.createId(env.getApplicationId(), topologyVersion), env)).collect(Collectors.toList());
        } else {
            ApplicationEnvironment env = applicationEnvironmentService.getOrFail(envId);
            AuthorizationUtil.checkAuthorizationForEnvironment(application, env);
            return Lists.newArrayList(getVariableDef(varName, Csar.createId(env.getApplicationId(), topologyVersion), env));
        }
    }

    public Variable getInApplicationScope(String varName, String applicationId) {
        applicationService.checkAndGetApplication(applicationId);
        Properties properties = quickFileStorageService.loadApplicationVariables(applicationId);
        return new Variable(varName, properties.getProperty(varName));
    }

    private ScopeVariableExpressionDTO getVariableDef(String varName, String archiveId, ApplicationEnvironment env) {
        Properties variables = editorFileService.loadEnvironmentVariables(archiveId, env.getId());
        return getScopeVariableExpressionDTO(varName, env.getId(), env.getName(), variables);
    }

    private ScopeVariableExpressionDTO getScopeVariableExpressionDTO(String varName, String scopeId, String scopeName, Properties variables) {
        ScopeVariableExpressionDTO dto = new ScopeVariableExpressionDTO();
        dto.setScopeId(scopeId);
        dto.setScopeName(scopeName);
        dto.setVariable(new Variable(varName, variables.getProperty(varName)));
        return dto;
    }

    private ScopeVariableExpressionDTO getVariableDef(String varName, String archiveId, EnvironmentType environmentType) {
        Properties variables = editorFileService.loadEnvironmentTypeVariables(archiveId, environmentType);
        return getScopeVariableExpressionDTO(varName, environmentType.toString(), environmentType.toString(), variables);
    }

    public List<ScopeVariableExpressionDTO> getInEnvironmentTypeScope(String varName, String applicationId, String topologyVersion, String environmentType) {
        applicationService.checkAndGetApplication(applicationId);
        if (StringUtils.isBlank(environmentType)) {
            return Arrays.stream(EnvironmentType.values()).map(envType -> getVariableDef(varName, Csar.createId(applicationId, topologyVersion), envType))
                    .collect(Collectors.toList());
        } else {
            EnvironmentType envType = EnvironmentType.valueOf(environmentType);
            return Lists.newArrayList(getVariableDef(varName, Csar.createId(applicationId, topologyVersion), envType));
        }
    }
}
