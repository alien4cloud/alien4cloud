package org.alien4cloud.tosca.variable.service;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.alien4cloud.tosca.editor.EditorFileService;
import org.alien4cloud.tosca.model.Csar;
import org.alien4cloud.tosca.variable.service.QuickFileStorageService;
import org.alien4cloud.tosca.variable.ScopeVariableExpressionDTO;
import org.alien4cloud.tosca.variable.model.Variable;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import alien4cloud.application.ApplicationEnvironmentService;
import alien4cloud.application.ApplicationService;
import alien4cloud.model.application.Application;
import alien4cloud.model.application.ApplicationEnvironment;
import alien4cloud.model.application.EnvironmentType;
import alien4cloud.security.AuthorizationUtil;
import alien4cloud.utils.YamlParserUtil;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

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
        Map<String, Object> variables = quickFileStorageService.loadApplicationVariablesAsMap(applicationId);
        return getScopeVariableExpressionDTO(varName, applicationId, applicationId, variables).getVariable();
    }

    private ScopeVariableExpressionDTO getVariableDef(String varName, String archiveId, ApplicationEnvironment env) {
        Map<String, Object> variables = editorFileService.loadEnvironmentVariables(archiveId, env.getId());
        return getScopeVariableExpressionDTO(varName, env.getId(), env.getName(), variables);
    }

    private ScopeVariableExpressionDTO getScopeVariableExpressionDTO(String varName, String scopeId, String scopeName, Map<String, Object> variables) {
        ScopeVariableExpressionDTO dto = new ScopeVariableExpressionDTO();
        dto.setScopeId(scopeId);
        dto.setScopeName(scopeName);
        dto.setVariable(new Variable(varName, YamlParserUtil.dump(variables.get(varName))));
        return dto;
    }

    private ScopeVariableExpressionDTO getVariableDef(String varName, String archiveId, EnvironmentType environmentType) {
        Map<String, Object> variables = editorFileService.loadEnvironmentTypeVariables(archiveId, environmentType);
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

    /**
     * Get all the top level variables.
     * A top level var is a var defined directly in a var file.
     * 
     * @param topologyVersion
     * @return
     */
    public Map<String, DefinedScope> getTopLevelVariables(String applicationId, String topologyVersion) {
        // Set<String> topLevelVars = Sets.newHashSet();
        Map<String, DefinedScope> topLevelVars = Maps.newHashMap();
        getApplicationTopLevelVariables(applicationId).forEach(var -> topLevelVars.computeIfAbsent(var, key -> new DefinedScope()).application = true);
        getEnvironmentTopLevelVariables(applicationId, topologyVersion)
                .forEach(var -> topLevelVars.computeIfAbsent(var, key -> new DefinedScope()).environment = true);
        getEnvironmentTypeTopLevelVariables(applicationId, topologyVersion)
                .forEach(var -> topLevelVars.computeIfAbsent(var, key -> new DefinedScope()).environmentType = true);
        // topLevelVars.addAll(getApplicationTopLevelVariables(applicationId));
        // topLevelVars.addAll(getEnvironmentTopLevelVariables(applicationId, topologyVersion));
        // topLevelVars.addAll(getEnvironmentTypeTopLevelVariables(applicationId, topologyVersion));
        return topLevelVars;
    }

    private Collection<? extends String> getApplicationTopLevelVariables(String applicationId) {
        applicationService.checkAndGetApplication(applicationId);
        Map<String, Object> variables = quickFileStorageService.loadApplicationVariablesAsMap(applicationId);
        return variables.keySet();
    }

    private Collection<? extends String> getEnvironmentTypeTopLevelVariables(String applicationId, String topologyVersion) {
        applicationService.checkAndGetApplication(applicationId);
        return Arrays.stream(EnvironmentType.values())
                .map(environmentType -> editorFileService.loadEnvironmentTypeVariables(Csar.createId(applicationId, topologyVersion), environmentType).keySet())
                .reduce(Sets.newHashSet(), (set, anotherSet) -> Sets.newHashSet(CollectionUtils.union(set, anotherSet)));
    }

    private Collection<? extends String> getEnvironmentTopLevelVariables(String applicationId, String topologyVersion) {
        applicationService.checkAndGetApplication(applicationId);
        return Arrays.stream(applicationEnvironmentService.getAuthorizedByApplicationId(applicationId))
                .map(env -> editorFileService.loadEnvironmentVariables(Csar.createId(applicationId, topologyVersion), env.getId()).keySet())
                .reduce(Sets.newHashSet(), (set, anotherSet) -> Sets.newHashSet(CollectionUtils.union(set, anotherSet)));
    }

    @Getter
    @Setter
    @EqualsAndHashCode(of = "name")
    public static class DefinedScope {
        private boolean application;
        private boolean environment;
        private boolean environmentType;
    }
}
