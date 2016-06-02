package org.alien4cloud.tosca.editor;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;

import alien4cloud.dao.ESGenericIdDAO;
import alien4cloud.model.topology.Topology;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import lombok.SneakyThrows;

/**
 * The toplogy edition context manager is responsible to manage the caching and lifecycle of TopologyEditionContexts.
 */
public class TopologyEditionContextManager {
    @Resource(name = "alien-dao")
    private ESGenericIdDAO dao;
    // TODO make cache management a parameter
    private LoadingCache<String, TopologyEditionContext> contextCache;

    @PostConstruct
    public void setup() {
        // initialize the cache
        contextCache = CacheBuilder.newBuilder().expireAfterAccess(5, TimeUnit.MINUTES)
                .build(
                        new CacheLoader<String, TopologyEditionContext>() {
                            @Override
                            public TopologyEditionContext load(String topologyId) throws Exception {
                                Topology topology =  dao.findById(Topology.class, topologyId);

                                return new TopologyEditionContext();
                            }
                        }
                );
    }

    @SneakyThrows
    public TopologyEditionContext get(String topologyId) {
        return contextCache.get(topologyId);
    }
}