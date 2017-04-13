package org.alien4cloud.tosca.editor;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import alien4cloud.model.common.Usage;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.alien4cloud.tosca.catalog.events.ArchiveUsageRequestEvent;
import org.alien4cloud.tosca.catalog.events.BeforeArchiveDeleted;
import org.alien4cloud.tosca.catalog.events.BeforeArchiveIndexed;
import org.alien4cloud.tosca.catalog.events.BeforeArchivePromoted;
import org.alien4cloud.tosca.catalog.index.CsarService;
import org.alien4cloud.tosca.editor.operations.AbstractEditorOperation;
import org.alien4cloud.tosca.editor.operations.UpdateFileOperation;
import org.alien4cloud.tosca.model.CSARDependency;
import org.alien4cloud.tosca.model.Csar;
import org.alien4cloud.tosca.model.templates.Topology;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import com.google.common.cache.*;

import alien4cloud.component.repository.IFileRepository;
import alien4cloud.topology.TopologyServiceCore;
import alien4cloud.tosca.context.ToscaContext;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

/**
 * The topology edition context manager is responsible to manage the caching and lifecycle of TopologyEditionContexts.
 */
@Slf4j
@Component
public class EditionContextManager {
    /** Holds the topology context */
    private final static ThreadLocal<EditionContext> contextThreadLocal = new ThreadLocal<>();
    @Inject
    private CsarService csarService;
    @Inject
    private TopologyServiceCore topologyServiceCore;
    @Inject
    private EditorRepositoryService repositoryService;
    @Inject
    private IFileRepository artifactRepository;

    // TODO make cache management time a parameter
    private LoadingCache<String, EditionContext> contextCache;

    @PostConstruct
    public void setup() {
        // initialize the cache
        contextCache = CacheBuilder.newBuilder().expireAfterAccess(30, TimeUnit.MINUTES).removalListener(new RemovalListener<String, EditionContext>() {
            @Override
            public void onRemoval(RemovalNotification<String, EditionContext> removalNotification) {
                log.debug("Topology edition context with id {} has been evicted. {} pending operations are lost.", removalNotification.getKey(),
                        removalNotification.getValue().getOperations().size());
                for (AbstractEditorOperation operation : removalNotification.getValue().getOperations()) {
                    if (operation instanceof UpdateFileOperation) {
                        String fileId = ((UpdateFileOperation) operation).getTempFileId();
                        if (artifactRepository.isFileExist(fileId)) {
                            artifactRepository.deleteFile(fileId);
                        }
                    }
                }
            }
        }).build(new CacheLoader<String, EditionContext>() {
            @Override
            public EditionContext load(String csarId) throws Exception {
                log.debug("Loading edition context for archive {}", csarId);
                Csar csar = csarService.getOrFail(csarId);
                Topology topology = topologyServiceCore.getOrFail(csarId);
                // check if the topology git repository has been created already
                Path topologyGitPath = repositoryService.createGitDirectory(csar);
                log.debug("Edition context for archive {} loaded", csar);
                return new EditionContext(csar, topology, topologyGitPath);
            }
        });
    }

    /**
     * Initialize thread local contexts for the topology.
     * 
     * @param topologyId The id of the topology.
     */
    @SneakyThrows
    public synchronized void init(String topologyId) {
        contextThreadLocal.set(contextCache.get(topologyId));
        ToscaContext.set(contextThreadLocal.get().getToscaContext());
    }

    /**
     * Reset the state of the topology context to it's initial state.
     * 
     * @throws IOException In case the parsing of the directory content fails.
     */
    public void reset() throws IOException {
        Topology topology = topologyServiceCore.getOrFail(getTopology().getId());
        contextThreadLocal.get().reset(topology);
        ToscaContext.set(contextThreadLocal.get().getToscaContext());
    }

    /**
     * Get the current topology edition context for the thread.
     * 
     * @return The thread's topology edition context.
     */
    public static EditionContext get() {
        return contextThreadLocal.get();
    }

    /**
     * Get the archive of the current topology under edition.
     *
     * @return The thread's archive of the topology under edition.
     */
    public static Csar getCsar() {
        return contextThreadLocal.get().getCsar();
    }

    /**
     * Get the current topology under edition.
     *
     * @return The thread's topology under edition.
     */
    public static Topology getTopology() {
        return contextThreadLocal.get().getTopology();
    }

    /**
     * Remove thread local contexts.
     */
    public void destroy() {
        contextThreadLocal.remove();
        ToscaContext.destroy();
    }

    @EventListener
    public synchronized void handleArchiveRemoved(BeforeArchiveDeleted event) {
        contextCache.invalidate(event.getArchiveId());
    }

    @EventListener
    public synchronized void handleArchiveUpdated(BeforeArchiveIndexed event) {
        contextCache.invalidate(event.getArchiveRoot().getArchive().getId());
    }

    @EventListener
    public synchronized void handleArchivePromoted(BeforeArchivePromoted event) {
        contextCache.invalidate(event.getArchiveId());
    }

    @SneakyThrows
    @EventListener
    public synchronized void reportArchiveUsage(ArchiveUsageRequestEvent event) {
        for (String topologyId : contextCache.asMap().keySet()) {
            if (isTopologyUsingArchive(event.getArchiveName(), event.getArchiveVersion(), topologyId)) {
                event.addUsage(new Usage(contextCache.get(topologyId).getCsar().getName(), "Topology editor", topologyId,
                        contextCache.get(topologyId).getCsar().getWorkspace()));
            }
        }
    }

    @SneakyThrows
    private boolean isTopologyUsingArchive(String archiveName, String archiveVersion, String topologyId) {
        for (CSARDependency dependency : contextCache.get(topologyId).getToscaContext().getDependencies()) {
            if (archiveName.equals(dependency.getName()) && archiveVersion.equals(dependency.getVersion())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Invalidate all cached objects
     */
    public synchronized void clearCache() {
        contextCache.invalidateAll();
    }
}