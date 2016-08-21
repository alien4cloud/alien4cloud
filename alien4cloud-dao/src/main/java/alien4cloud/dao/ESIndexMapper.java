package alien4cloud.dao;

import java.beans.IntrospectionException;
import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;

import javax.annotation.Resource;

import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import org.elasticsearch.action.ActionFuture;
import org.elasticsearch.action.admin.indices.create.CreateIndexRequestBuilder;
import org.elasticsearch.action.admin.indices.create.CreateIndexResponse;
import org.elasticsearch.action.admin.indices.exists.indices.IndicesExistsRequest;
import org.elasticsearch.action.admin.indices.exists.indices.IndicesExistsResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.mapping.ElasticSearchClient;
import org.elasticsearch.mapping.MappingBuilder;
import org.elasticsearch.util.MapUtil;

import alien4cloud.exception.IndexingServiceException;
import alien4cloud.rest.utils.JsonUtil;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

/**
 * Manages one or multiple indexes and the related java and elastic search types.
 *
 * @author luc boutier
 */
@Slf4j
public abstract class ESIndexMapper {
    /** Custom score field for alien. */
    public static final String ALIEN_SCORE = "alienScore";

    @Resource
    private ElasticSearchClient esClient;
    @Resource
    @Getter
    private MappingBuilder mappingBuilder;

    @Getter
    private final Map<String, String> typesToIndices = Maps.newHashMap();
    @Getter
    private final Map<String, Class<?>> typesToClasses = Maps.newHashMap();

    @Getter
    private String[] allIndexes;
    @Getter
    @Setter
    private ObjectMapper jsonMapper = new ObjectMapper();

    /**
     * Initialize the array of all indices managed by this dao.
     */
    public void initCompleted() {
        Set<String> indices = new HashSet<>(typesToIndices.values());
        allIndexes = indices.toArray(new String[indices.size()]);
        esClient.waitForGreenStatus(allIndexes);
    }

    /**
     * Create if not exist indices.
     * A TTL can be defined for all indices under this index (ESearch TTL notation)
     * 
     * @param indexName The index to initialize
     * @param classes An array of classes to map to this index.
     */
    @SneakyThrows({ IOException.class, IntrospectionException.class })
    public void initIndices(String indexName, String ttl, Class<?>... classes) {
        if (indexExist(indexName)) {
            addToMappedClasses(indexName, classes);
        } else {
            // create the index and add the mapping
            CreateIndexRequestBuilder createIndexRequestBuilder = esClient.getClient().admin().indices().prepareCreate(indexName);
            for (Class<?> clazz : classes) {
                String typeName = addToMappedClasses(indexName, clazz);
                String typeMapping = mappingBuilder.getMapping(clazz);
                Map<String, Object> typesMap = JsonUtil.toMap(typeMapping);

                addAlienScore(typesMap);
                addTTL(typesMap, ttl);

                String mapping = jsonMapper.writeValueAsString(typesMap);
                createIndexRequestBuilder.addMapping(typeName, mapping);
            }
            // TODO: fixme !!!
            try {
                final CreateIndexResponse createResponse = createIndexRequestBuilder.execute().actionGet();
                if (!createResponse.isAcknowledged()) {
                    throw new IndexingServiceException("Failed to create index <" + indexName + ">");
                }
            } catch (Exception e) {
                log.warn("Not able to init indice for index {}, maybe it has been created elsewhere", indexName);
            }
        }
    }

    /**
     * Add the alien score field for each type in the map.
     * 
     * @param typesMap The type map.
     */
    private void addAlienScore(Map<String, Object> typesMap) {
        for (Object typeMappingObject : typesMap.values()) {
            @SuppressWarnings("unchecked")
            Map<String, Object> typeMappingMap = (Map<String, Object>) typeMappingObject;
            Map<String, Object> propertiesMap = (Map<String, Object>) typeMappingMap.get("properties");
            Map<String, Object> scoreMapping = MapUtil.getMap(
                    new String[] { "include_in_all", "precision_step", "index", "boost", "store", "ignore_malformed", "type" },
                    new String[] { "false", "4", "not_analyzed", "1.0", "false", "false", "long" });
            propertiesMap.put(ALIEN_SCORE, scoreMapping);
        }
    }

    /**
     * Add the ttl field for each type in the map.
     *
     * @param typesMap The type map.
     */
    private void addTTL(Map<String, Object> typesMap, String ttl) {
        if (ttl == null) {
            // if no ttl value is provided then just return.
            return;
        }
        for (Object typeMappingObject : typesMap.values()) {
            @SuppressWarnings("unchecked")
            Map<String, Object> typeMappingMap = (Map<String, Object>) typeMappingObject;
            Map<String, Object> ttlMapping = MapUtil.getMap(new String[] { "enabled", "default" }, new String[] { "true", ttl });
            typeMappingMap.put("_ttl", ttlMapping);
        }
    }

    @SneakyThrows({ ExecutionException.class, InterruptedException.class })
    private boolean indexExist(String indexName) {
        // check if existing before
        final ActionFuture<IndicesExistsResponse> indexExistFuture = esClient.getClient().admin().indices().exists(new IndicesExistsRequest(indexName));
        IndicesExistsResponse response;
        response = indexExistFuture.get();
        return response.isExists();
    }

    private void addToMappedClasses(String indexName, Class<?>[] classes) {
        for (Class<?> clazz : classes) {
            addToMappedClasses(indexName, clazz);
        }
    }

    private String addToMappedClasses(String indexName, Class<?> clazz) {
        log.info("Mapping class <" + clazz.getName() + "> to index <" + indexName + ">");
        String typeName = MappingBuilder.indexTypeFromClass(clazz);
        typesToIndices.put(typeName, indexName);
        typesToClasses.put(typeName, clazz);
        return typeName;
    }

    /**
     * Get the index in which the given type lies.
     *
     * @param clazz The type for which to get the index.
     * @return The index in which the given type lies.
     */
    public String getIndexForType(Class<?> clazz) {
        String typeName = MappingBuilder.indexTypeFromClass(clazz);
        String index = typesToIndices.get(typeName);
        if (index == null) {
            log.error("Class <" + clazz.getName() + "> is not registered in any indexes.");
            throw new IndexingServiceException("Requested type <" + typeName + "> is not registered in any indexes.");
        }
        return index;
    }

    /**
     * Return a class from the given elastic search type.
     *
     * @param type The elastic search type.
     * @return The class matching the given type if any, null if no class is matching the type.
     */
    public Class<?> getClassFromType(String type) {
        return this.typesToClasses.get(type);
    }

    /**
     * Get the class from the class name.
     *
     * @param className The name of the class to get.
     * @param allowNull If a null class name is allowed. If className is null and allowNull is true then null is returned, if className is null and allowNull is
     *            false then an {@link IndexingServiceException} is thrown.
     * @return The class matching the given class name, or null if className is null and allowNull is true.
     */
    public Class<?> getClassFromName(String className, boolean allowNull) {
        if (className == null || className.trim().isEmpty()) {
            if (allowNull) {
                return null;
            }
            throw new IndexingServiceException("Class type <" + className + "> not found.");
        } else {
            try {
                return Class.forName(className);
            } catch (ClassNotFoundException e) {
                log.error("Error while trying to perform search operation.", e);
                throw new IndexingServiceException("Class type <" + className + "> not found.", e);
            }
        }
    }

    public String[] getTypesFromClass(Class<?> clazz) {
        List<String> types = Lists.newArrayList();
        Collection<Class<?>> allManagedClasses = typesToClasses.values();
        for (Class<?> managedClass : allManagedClasses) {
            if (clazz.isAssignableFrom(managedClass)) {
                types.add(MappingBuilder.indexTypeFromClass(managedClass));
            }
        }
        return types.toArray(new String[types.size()]);
    }

    /**
     * Get the elastic search client linked to the index mapper.
     *
     * @return The elastic search client linked to the index mapper.
     */
    public Client getClient() {
        return this.esClient.getClient();
    }

    public static org.slf4j.Logger getLog() {
        return log;
    }

}
