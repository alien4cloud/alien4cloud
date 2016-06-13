package org.alien4cloud.tosca.editor;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;

import org.alien4cloud.tosca.editor.commands.AbstractEditorOperation;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.Lists;

import alien4cloud.dao.ESGenericIdDAO;
import alien4cloud.model.topology.Topology;
import alien4cloud.topology.TopologyServiceCore;
import alien4cloud.tosca.context.ToscaContext;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

/**
 * The toplogy edition context manager is responsible to manage the caching and lifecycle of TopologyEditionContexts.
 */
@Slf4j
@Component
public class TopologyEditionContextManager {
    /** Holds the topology context */
    private final static ThreadLocal<TopologyEditionContext> contextThreadLocal = new ThreadLocal<>();

    @Resource
    private TopologyServiceCore topologyServiceCore;
    @Resource(name = "alien-es-dao")
    private ESGenericIdDAO dao;
    @Value("${directories.alien}")
    private String topologyEditorGitPath;
    private Path localGitRepositoryPath;
    // TODO make cache management time a parameter
    private LoadingCache<String, TopologyEditionContext> contextCache;

    @PostConstruct
    public void setup() {
        localGitRepositoryPath = Paths.get(topologyEditorGitPath).resolve("topologyeditor");
        // initialize the cache
        contextCache = CacheBuilder.newBuilder().expireAfterAccess(5, TimeUnit.MINUTES).build(new CacheLoader<String, TopologyEditionContext>() {
            @Override
            public TopologyEditionContext load(String topologyId) throws Exception {
                log.debug("Loading topology context for topology {}", topologyId);
                Topology topology = topologyServiceCore.getOrFail(topologyId);
                // if we get it again this go through JSON and create a clone.
                Topology clonedTopology = topologyServiceCore.getOrFail(topologyId);
                ToscaContext context = new ToscaContext();
                // check if the topology git repository has been created already
                Path topologyGitPath = localGitRepositoryPath.resolve(topologyId);
                createGitDirectory(topologyGitPath);
                log.debug("Topology context for topology {} loaded", topologyId);
                return new TopologyEditionContext(topology, clonedTopology, new ToscaContext.Context(topology.getDependencies()), topologyGitPath,
                        Lists.<AbstractEditorOperation> newArrayList());
            }
        });
    }

    private void createGitDirectory(Path topologyGitPath) throws IOException {
        if (!Files.isDirectory(topologyGitPath)) {
            log.debug("Initializing topology git repository {}", localGitRepositoryPath.toAbsolutePath());
            Files.createDirectories(topologyGitPath);
        } else {
            log.info("Alien Repository folder already created at {}", localGitRepositoryPath.toAbsolutePath());
        }
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
     * Get the current topology edition context for the thread.
     * 
     * @return The thread's topology edition context.
     */
    public static TopologyEditionContext get() {
        return contextThreadLocal.get();
    }

    /**
     * Get the current topology under edition.
     *
     * @return The thread's topology under edition.
     */
    public static Topology getTopology() {
        return contextThreadLocal.get().getCurrentTopology();
    }

    /**
     * Remove thread local contexts.
     */
    public void destroy() {
        contextThreadLocal.remove();
        ToscaContext.destroy();
    }
}