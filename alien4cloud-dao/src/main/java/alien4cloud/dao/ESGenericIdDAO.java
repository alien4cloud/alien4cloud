package alien4cloud.dao;

import java.io.IOException;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import org.elasticsearch.action.bulk.BulkRequestBuilder;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.get.MultiGetItemResponse;
import org.elasticsearch.action.get.MultiGetResponse;
import org.elasticsearch.common.collect.Lists;
import org.elasticsearch.mapping.MappingBuilder;

import alien4cloud.exception.IndexingServiceException;
import alien4cloud.model.common.IUpdatedDate;
import lombok.SneakyThrows;

/**
 * ElasticSearch DAO to manage id based operations.
 * 
 * @author luc boutier
 */
public abstract class ESGenericIdDAO extends ESIndexMapper implements IGenericIdDAO {

    @Override
    public <T> boolean exist(Class<T> clazz, String id) {
        return getClient().prepareGet(getIndexForType(clazz), MappingBuilder.indexTypeFromClass(clazz), id).setFields(new String[0]).execute().actionGet()
                .isExists();
    }

    /**
     * The save method should be in charge to set the creationDate and the lastUpdateDate.
     * 
     * @param data
     * @param <T>
     */
    private <T> void updateDate(T data) {
        if (data instanceof IUpdatedDate) {
            IUpdatedDate resource = (IUpdatedDate) data;
            resource.setLastUpdateDate(new Date());
            if (resource.getCreationDate() == null) {
                resource.setCreationDate(resource.getLastUpdateDate());
            }
        }
    }

    @Override
    @SneakyThrows({ IOException.class })
    public <T> void save(T data) {
        String indexName = getIndexForType(data.getClass());
        String typeName = MappingBuilder.indexTypeFromClass(data.getClass());

        updateDate(data);
        String json = getJsonMapper().writeValueAsString(data);
        getClient().prepareIndex(indexName, typeName).setOperationThreaded(false).setSource(json).setRefresh(true).execute().actionGet();
    }

    @Override
    @SneakyThrows({ IOException.class })
    public <T> void save(T[] entities) {
        if (entities == null || entities.length == 0) {
            return;
        }
        BulkRequestBuilder bulkRequestBuilder = getClient().prepareBulk().setRefresh(true);
        for (T data : entities) {
            String indexName = getIndexForType(data.getClass());
            String typeName = MappingBuilder.indexTypeFromClass(data.getClass());

            updateDate(data);
            String json = getJsonMapper().writeValueAsString(data);
            bulkRequestBuilder.add(getClient().prepareIndex(indexName, typeName).setSource(json));
        }
        bulkRequestBuilder.execute().actionGet();
    }

    @SuppressWarnings("unchecked")
    @Override
    @SneakyThrows({ IOException.class })
    public <T> T findById(Class<T> clazz, String id) {
        boolean abstractType = Modifier.isAbstract(clazz.getModifiers());
        assertIdNotNullFor(id, "findById");
        String indexName = getIndexForType(clazz);
        String typeName = abstractType ? null : MappingBuilder.indexTypeFromClass(clazz);
        GetResponse response = getClient().prepareGet(indexName, typeName, id).execute().actionGet();

        if (response == null || !response.isExists()) {
            ESIndexMapper.getLog().debug("Nothing found in index <{}>, type <{}>, for Id <{}>.", indexName, typeName, id);
            return null;
        }

        ESIndexMapper.getLog().debug("Found one in index <{}>, type <{}>, for Id <{}>.", indexName, typeName, id);

        if (abstractType) {
            return (T) getJsonMapper().readValue(response.getSourceAsString(), getTypesToClasses().get(response.getType()));
        }

        return getJsonMapper().readValue(response.getSourceAsString(), clazz);
    }

    @Override
    @SneakyThrows({ IOException.class })
    public <T> List<T> findByIds(Class<T> clazz, String... ids) {
        String indexName = getIndexForType(clazz);
        String typeName = MappingBuilder.indexTypeFromClass(clazz);
        MultiGetResponse response = getClient().prepareMultiGet().add(indexName, typeName, ids).execute().actionGet();

        if (response == null || response.getResponses() == null || response.getResponses().length == 0) {
            ESIndexMapper.getLog().debug("Nothing found in index <{}>, type <{}>, for Ids <{}>.", indexName, typeName, Arrays.toString(ids));
            return null;
        }

        List<T> result = new ArrayList<>();
        for (MultiGetItemResponse getItemResponse : response.getResponses()) {
            if (getItemResponse.getResponse().isExists()) {
                result.add(getJsonMapper().readValue(getItemResponse.getResponse().getSourceAsString(), clazz));
            }
        }

        return result;
    }

    @Override
    public void delete(Class<?> clazz, String id) {
        assertIdNotNullFor(id, "delete");
        String indexName = getIndexForType(clazz);
        String typeName = MappingBuilder.indexTypeFromClass(clazz);
        getClient().prepareDelete(indexName, typeName, id).setRefresh(true).execute().actionGet();
    }

    private void assertIdNotNullFor(String id, String operation) {
        if (id == null || id.trim().isEmpty()) {
            ESIndexMapper.getLog().error("Null or empty Id is not allowed for operation <" + operation + ">.");
            throw new IndexingServiceException("Null or empty Id is not allowed for operation <" + operation + ">.");
        }
    }

    protected Class<?>[] getRequestedTypes(Class<?> clazz) {
        if (Modifier.isAbstract(clazz.getModifiers())) {
            List<Class<?>> classes = Lists.newArrayList();
            for (Class<?> registeredClass : this.getTypesToClasses().values()) {
                if (clazz.isAssignableFrom(registeredClass)) {
                    classes.add(registeredClass);
                }
            }
            return classes.toArray(new Class<?>[classes.size()]);
        }
        return new Class<?>[] { clazz };
    }

    protected String[] getTypesStrings(Class<?>... classes) {
        if (classes == null) {
            return null;
        }
        List<String> types = new ArrayList<String>(classes.length);
        for (Class<?> clazz : classes) {
            if (clazz != null) {
                types.add(MappingBuilder.indexTypeFromClass(clazz));
            }
        }
        if (types.isEmpty()) {
            return null;
        }
        return types.toArray(new String[types.size()]);
    }
}