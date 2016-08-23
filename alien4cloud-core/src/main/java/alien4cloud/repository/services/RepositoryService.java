package alien4cloud.repository.services;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.annotation.Resource;

import org.apache.commons.lang3.StringUtils;
import org.elasticsearch.index.query.QueryBuilders;
import org.springframework.stereotype.Component;

import com.google.common.collect.Maps;

import alien4cloud.component.repository.IArtifactResolver;
import alien4cloud.component.repository.IConfigurableArtifactResolver;
import alien4cloud.component.repository.IConfigurableArtifactResolverFactory;
import alien4cloud.dao.IGenericSearchDAO;
import alien4cloud.dao.model.FacetedSearchResult;
import alien4cloud.exception.AlreadyExistException;
import alien4cloud.exception.NotFoundException;
import alien4cloud.model.repository.Repository;
import alien4cloud.plugin.Plugin;
import alien4cloud.plugin.PluginManager;
import alien4cloud.plugin.model.PluginComponent;
import alien4cloud.repository.model.RepositoryPluginComponent;
import alien4cloud.repository.model.ValidationResult;
import alien4cloud.rest.model.FilteredSearchRequest;
import alien4cloud.rest.utils.JsonUtil;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class RepositoryService {

    @Resource(name = "alien-es-dao")
    private IGenericSearchDAO alienDAO;

    @Resource
    private ConfigurableResolverRegistry configurableResolverRegistry;

    @Resource
    private ResolverRegistry resolverRegistry;

    @Resource
    private PluginManager pluginManager;

    private Map<String, IConfigurableArtifactResolver> registeredResolvers = Maps.newConcurrentMap();

    public void initialize() {
        // We won't have millions of configured repositories for all the platform
        List<Repository> repositories = alienDAO.customFindAll(Repository.class, QueryBuilders.matchAllQuery());
        if (repositories != null) {
            for (Repository repository : repositories) {
                Plugin plugin = alienDAO.findById(Plugin.class, repository.getPluginId());
                if (plugin.isEnabled()) {
                    createConfiguredResolver(repository.getId(), repository.getConfiguration(), repository.getPluginId());
                } else {
                    // TODO Later when the user has re-enabled the plugin must re-initialize this
                    log.info("Don't enable resolver as the plugin {} with name {} is not enabled ", plugin.getId(), plugin.getDescriptor().getName());
                }
            }
        }
    }

    public void unloadAllResolvers() {
        registeredResolvers.clear();
    }

    /**
     * Try to resolve an artifact from all configured resolver plugins and repositories
     * 
     * @param artifactReference reference of the artifact inside the repository
     * @param repositoryURL the repository's URL
     * @param repositoryType the type of the repository
     * @param credentials the credentials to retrieve the artifact
     * @return the artifact's path downloaded locally, null if artifact cannot be resolved
     */
    public Path resolveArtifact(String artifactReference, String repositoryURL, String repositoryType, String credentials) {
        for (IConfigurableArtifactResolver configurableArtifactResolver : registeredResolvers.values()) {
            Path resolvedArtifact = configurableArtifactResolver.resolveArtifact(artifactReference, repositoryURL, repositoryType, credentials);
            if (resolvedArtifact != null) {
                return resolvedArtifact;
            }
        }
        for (Map<String, IArtifactResolver> resolverMap : resolverRegistry.getInstancesByPlugins().values()) {
            for (IArtifactResolver resolver : resolverMap.values()) {
                Path resolvedArtifact = resolver.resolveArtifact(artifactReference, repositoryURL, repositoryType, credentials);
                if (resolvedArtifact != null) {
                    return resolvedArtifact;
                }
            }
        }
        return null;
    }

    public boolean canResolveArtifact(String artifactReference, String repositoryURL, String repositoryType, String credentials) {
        for (IConfigurableArtifactResolver configurableArtifactResolver : registeredResolvers.values()) {
            ValidationResult validationResult = configurableArtifactResolver.canHandleArtifact(artifactReference, repositoryURL, repositoryType, credentials);
            if (validationResult.equals(ValidationResult.SUCCESS)) {
                return true;
            }
        }
        for (Map<String, IArtifactResolver> resolverMap : resolverRegistry.getInstancesByPlugins().values()) {
            for (IArtifactResolver resolver : resolverMap.values()) {
                ValidationResult validationResult = resolver.canHandleArtifact(artifactReference, repositoryURL, repositoryType, credentials);
                if (validationResult.equals(ValidationResult.SUCCESS)) {
                    return true;
                }
            }
        }
        return false;
    }

    public FacetedSearchResult search(FilteredSearchRequest searchRequest) {
        return alienDAO.facetedSearch(Repository.class, searchRequest.getQuery(), searchRequest.getFilters(), null, searchRequest.getFrom(),
                searchRequest.getSize());
    }

    public List<RepositoryPluginComponent> listPluginComponents() {
        List<RepositoryPluginComponent> repositoryPluginComponents = new ArrayList<>();
        List<PluginComponent> pluginComponents = pluginManager.getPluginComponents(IArtifactResolver.class.getSimpleName());
        repositoryPluginComponents.addAll(pluginComponents.stream().map(
                pluginComponent -> new RepositoryPluginComponent(pluginComponent, getResolverFactoryOrFail(pluginComponent.getPluginId()).getResolverType()))
                .collect(Collectors.toList()));
        return repositoryPluginComponents;
    }

    private IConfigurableArtifactResolverFactory getResolverFactoryOrFail(String pluginId) {
        IConfigurableArtifactResolverFactory configurableArtifactResolverFactory = configurableResolverRegistry.getSinglePluginBean(pluginId);
        if (configurableArtifactResolverFactory == null) {
            throw new NotFoundException("Plugin " + pluginId + " is not found or is not enabled");
        }
        return configurableArtifactResolverFactory;
    }

    public Class<?> getRepositoryConfigurationType(String pluginId) {
        return getResolverFactoryOrFail(pluginId).getResolverConfigurationType();
    }

    private void createConfiguredResolver(String repositoryId, Object configuration, String pluginId) {
        IConfigurableArtifactResolverFactory configurableArtifactResolverFactory = getResolverFactoryOrFail(pluginId);
        IConfigurableArtifactResolver configurableArtifactResolver = configurableArtifactResolverFactory.newInstance();
        configurableArtifactResolver.setConfiguration(JsonUtil.toObject(configuration, configurableArtifactResolverFactory.getResolverConfigurationType()));
        registeredResolvers.put(repositoryId, configurableArtifactResolver);
    }

    public String createRepositoryConfiguration(String name, String pluginId, Object configuration) {
        String id = UUID.randomUUID().toString();
        Repository repository = new Repository(id, name, pluginId, getResolverFactoryOrFail(pluginId).getResolverType(), configuration);
        ensureNameUnicityAndSave(repository, null);
        createConfiguredResolver(id, configuration, pluginId);
        return id;
    }

    public void updateRepository(Repository updated, String oldName, boolean updateConfiguration) {
        ensureNameUnicityAndSave(updated, oldName);
        if (updateConfiguration) {
            if (registeredResolvers.containsKey(updated.getId())) {
                IConfigurableArtifactResolverFactory resolverFactory = getResolverFactoryOrFail(updated.getPluginId());
                IConfigurableArtifactResolver resolver = registeredResolvers.get(updated.getId());
                resolver.setConfiguration(JsonUtil.toObject(updated.getConfiguration(), resolverFactory.getResolverConfigurationType()));
            }
        } else {
            log.warn("Repository not found with id [" + updated.getId() + "] and name [" + updated.getName() + "]");
        }
    }

    public Repository getOrFail(String id) {
        Repository repository = alienDAO.findById(Repository.class, id);
        if (repository == null) {
            throw new NotFoundException("Repository [" + id + "] cannot be found");
        }
        return repository;
    }

    public void delete(String id) {
        alienDAO.delete(Repository.class, id);
        registeredResolvers.remove(id);
    }

    private synchronized void ensureNameUnicityAndSave(Repository repository, String oldName) {
        if (StringUtils.isBlank(oldName) || !Objects.equals(repository.getName(), oldName)) {
            // check that the orchestrator doesn't already exists
            if (alienDAO.count(Repository.class, QueryBuilders.termQuery("name", repository.getName())) > 0) {
                throw new AlreadyExistException("a repository with the given name already exists.");
            }
        }
        alienDAO.save(repository);
    }
}
