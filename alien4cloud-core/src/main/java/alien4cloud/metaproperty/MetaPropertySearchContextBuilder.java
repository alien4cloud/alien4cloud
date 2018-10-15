package alien4cloud.metaproperty;

import alien4cloud.dao.IESMetaPropertiesSearchContext;
import alien4cloud.dao.IESMetaPropertiesSearchContextBuilder;
import alien4cloud.dao.IGenericSearchDAO;
import alien4cloud.dao.model.FacetedSearchResult;
import alien4cloud.dao.model.GetMultipleDataResult;

import alien4cloud.model.application.Application;
import alien4cloud.model.common.MetaPropConfiguration;
import alien4cloud.model.common.MetaPropertyTarget;
import alien4cloud.model.service.ServiceResource;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;

import lombok.extern.slf4j.Slf4j;
import org.alien4cloud.tosca.model.templates.Topology;
import org.alien4cloud.tosca.model.types.NodeType;
import org.elasticsearch.common.collect.Maps;
import org.elasticsearch.index.query.FilterBuilder;
import org.elasticsearch.mapping.IFacetBuilderHelper;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

@Slf4j
@Service
public class MetaPropertySearchContextBuilder implements IESMetaPropertiesSearchContextBuilder,ApplicationListener<MetaPropertyEvent> {

    /**
     * Prefix used for metaproperties
     */
    private static final String PREFIX = "metaProperties";

    /**
     * ES Dao
     */
    @Resource(name = "alien-es-dao")
    private IGenericSearchDAO alienDAO;

    /**
     * Class to Target map
     */
    private Map<Class<?>,String> classToTarget = new HashMap<>();

    /**
     * Metaproperty data
     */
    private Map<String,BiMap<String,String>> map = null;

    /**
     * Lock
     */
    private Lock lock = new ReentrantLock();

    /**
     * Lazily build the map
     *
     * @return the meta properties map
     */
    private Map<String,BiMap<String,String>> getMap() {
        try {
            lock.lock();
            if (map == null) {
                map = buildMap();
            }
            return map;
        } finally {
            lock.unlock();
        }
    }

    /**
     * Build the map.
     *
     * @return the meta properties map
     */
    private Map<String,BiMap<String,String>> buildMap() {
        Map<String,BiMap<String,String>> resultMap = new HashMap<>();

        Map<String, String[]> filters = Maps.newHashMap();
        filters.put("target", classToTarget.values().toArray(new String[0]));
        filters.put("filtered", new String[] { "true" });

        GetMultipleDataResult<MetaPropConfiguration> result = alienDAO.find(MetaPropConfiguration.class, filters,Integer.MAX_VALUE);

        for (MetaPropConfiguration meta : result.getData()) {
            BiMap<String,String> targetMap = resultMap.computeIfAbsent(meta.getTarget(),k -> HashBiMap.create());
            targetMap.put(meta.getId(),meta.getName());
        }

        return resultMap;
    }

    private MetaPropertySearchContextBuilder() {
        classToTarget.put(NodeType.class, MetaPropertyTarget.COMPONENT);
        classToTarget.put(ServiceResource.class, MetaPropertyTarget.SERVICE);
        classToTarget.put(Application.class, MetaPropertyTarget.APPLICATION);
        classToTarget.put(Topology.class, MetaPropertyTarget.TOPOLOGY);
    }

    @Override
    public void onApplicationEvent(MetaPropertyEvent metaPropertyEvent) {
        try {
            lock.lock();
            // The map will be rebuilt on next getMap
            map = null;
        } finally {
            lock.unlock();
        }
    }

    private class Context implements IESMetaPropertiesSearchContext {

        private final BiMap<String,String> map;

        private Context(BiMap<String,String> map) {
            this.map = map;
        }

        @Override
        public List<IFacetBuilderHelper> getFacetBuilderHelpers() {
            if (map != null) {
                return map.keySet().stream().map(f -> new MetaPropertyAggregationBuilderHelper(PREFIX , f)).collect(Collectors.toList());
            } else {
                return new ArrayList<>();
            }
        }

        @Override
        public FilterBuilder[] getFilterBuilders(Map<String, String[]> filters) {
            List<FilterBuilder> result = new ArrayList<>();

            if (filters != null) {
                for (IFacetBuilderHelper helper : getFacetBuilderHelpers()) {
                    String fieldName = helper.getEsFieldName();

                    if (filters.containsKey(fieldName)) {
                        result.add(helper.buildFilter(fieldName,filters.get(fieldName)));
                    }
                }
            }

            return result.toArray(new FilterBuilder[0]);
        }

        @Override
        public void preProcess(Map<String, String[]> filters) {
            if (map != null) {
                MetaPropertySubstitutionHelper.subst(filters, map.inverse(), PREFIX);
            }
        }

        @Override
        public void postProcess(FacetedSearchResult result) {
            if (map != null) {
                MetaPropertySubstitutionHelper.subst(result.getFacets(), map, PREFIX);
            }
        }
    }

    public <T> IESMetaPropertiesSearchContext getContext(Class<T> clazz) {
        return Optional.ofNullable(classToTarget.get(clazz))
                .map(c -> new Context(getMap().get(c)))
                .orElse(new Context(null));
    }
}