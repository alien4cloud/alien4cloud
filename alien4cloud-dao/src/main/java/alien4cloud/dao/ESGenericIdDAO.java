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
import org.elasticsearch.action.get.MultiGetRequestBuilder;
import org.elasticsearch.action.get.MultiGetResponse;
import org.elasticsearch.action.support.WriteRequest.RefreshPolicy;
import org.elasticsearch.common.xcontent.XContentType;
import com.google.common.collect.Lists;
import org.elasticsearch.mapping.FieldsMappingBuilder;
import org.elasticsearch.mapping.MappingBuilder;

import alien4cloud.exception.IndexingServiceException;
import alien4cloud.model.common.IDatableResource;
import lombok.SneakyThrows;

/**
 * ElasticSearch DAO to manage id based operations.
 * 
 * @author luc boutier
 */
public abstract class ESGenericIdDAO extends ESIndexMapper implements IGenericIdDAO {

    @Override
    public <T> boolean exist(Class<T> clazz, String id) {
        //return getClient().prepareGet(getIndexForType(clazz), MappingBuilder.indexTypeFromClass(clazz), id).setStoredFields(new String[0]).execute().actionGet()
        String[] indices = getIndexForType(clazz);
        if (indices.length == 1) { // concrete class: one index only
           return getClient().prepareGet(indices[0], TYPE_NAME, id).setStoredFields(new String[0]).execute().actionGet().isExists();
        } else { // abstract class : several indices
           MultiGetRequestBuilder mrb = getClient().prepareMultiGet();
           for (String index : indices) {
              mrb.add(index, TYPE_NAME, id);
           }
           MultiGetResponse response = mrb.execute().actionGet();

           if (response == null || response.getResponses() == null || response.getResponses().length == 0) {
              return false;
           }
           return true;
        }
    }

    /**
     * The save method should be in charge to set the creationDate and the lastUpdateDate.
     * 
     * @param data
     * @param <T>
     */
    private <T> void updateDate(T data) {
        if (data instanceof IDatableResource) {
            IDatableResource resource = (IDatableResource) data;
            resource.setLastUpdateDate(new Date());
            if (resource.getCreationDate() == null) {
                resource.setCreationDate(resource.getLastUpdateDate());
            }
        }
    }

    @Override
    @SneakyThrows({ IOException.class })
    public <T> void save(T data) {
        //String indexName = getIndexForType(data.getClass());
        // should be a concrete class: class name gives index
        String typeName = MappingBuilder.indexTypeFromClass(data.getClass());

        updateDate(data);
        String json = getJsonMapper().writeValueAsString(data);
	String idValue = null;
        try {
           idValue = (new FieldsMappingBuilder()).getIdValue(data);
        } catch (Exception e) {}
        if (idValue == null) {
	   //getClient().prepareIndex(indexName, typeName).setSource(json, XContentType.JSON)
	   getClient().prepareIndex(typeName, TYPE_NAME).setSource(json, XContentType.JSON)
                      .setRefreshPolicy(RefreshPolicy.IMMEDIATE).execute().actionGet();
	} else {
	   //getClient().prepareIndex(indexName, typeName, idValue).setSource(json, XContentType.JSON)
	   getClient().prepareIndex(typeName, TYPE_NAME, idValue).setSource(json, XContentType.JSON)
                      .setRefreshPolicy(RefreshPolicy.IMMEDIATE).execute().actionGet();
        }
    }

    @Override
    @SneakyThrows({ IOException.class })
    public <T> void save(T[] entities) {
        if (entities == null || entities.length == 0) {
            return;
        }
        BulkRequestBuilder bulkRequestBuilder = getClient().prepareBulk().setRefreshPolicy(RefreshPolicy.IMMEDIATE);
        for (T data : entities) {
            //String indexName = getIndexForType(data.getClass());
            // should be a concrete class: class name gives index
            String typeName = MappingBuilder.indexTypeFromClass(data.getClass());

            updateDate(data);
            String json = getJsonMapper().writeValueAsString(data);
            String idValue = null;
	    try {
		idValue = (new FieldsMappingBuilder()).getIdValue(data);
            } catch (Exception e) {}
            if (idValue == null) {
		//bulkRequestBuilder.add(getClient().prepareIndex(indexName, typeName).setSource(json, XContentType.JSON));
		bulkRequestBuilder.add(getClient().prepareIndex(typeName, TYPE_NAME).setSource(json, XContentType.JSON));
            } else {
		//bulkRequestBuilder.add(getClient().prepareIndex(indexName, typeName, idValue).setSource(json, XContentType.JSON));
		bulkRequestBuilder.add(getClient().prepareIndex(typeName, TYPE_NAME, idValue).setSource(json, XContentType.JSON));
            }
        }
        bulkRequestBuilder.execute().actionGet();
    }

    @SuppressWarnings("unchecked")
    @Override
    @SneakyThrows({ IOException.class })
    public <T> T findById(Class<T> clazz, String id) {
        boolean abstractType = Modifier.isAbstract(clazz.getModifiers());
        assertIdNotNullFor(id, "findById");
        String[] indexName = getIndexForType(clazz);
        String typeName = abstractType ? null : MappingBuilder.indexTypeFromClass(clazz);
        //GetResponse response = getClient().prepareGet(indexName, typeName, id).execute().actionGet();
        
        if (indexName.length > 1) { // abstract class : several indices => perform a multiget
           List<T> result = findByIds(clazz, id);
           if ((result != null) && (result.size()>0)) {
              return result.get(0);
           } else {
              return null;
           }
        }

        GetResponse response = getClient().prepareGet(indexName[0], TYPE_NAME, id).execute().actionGet();

        if (response == null || !response.isExists()) {
            ESIndexMapper.getLog().debug("Nothing found in index [ {} ], type [ {} ], for Id [ {} ].", indexName[0], typeName, id);
            return null;
        }

        ESIndexMapper.getLog().debug("Found one in index [ {} ], type [ {} ], for Id [ {} ].", indexName[0], typeName, id);

        if (abstractType) {
            //return (T) getJsonMapper().readValue(response.getSourceAsString(), getTypesToClasses().get(response.getType()));
            return (T) getJsonMapper().readValue(response.getSourceAsString(), getTypesToClasses().get(response.getIndex()));
        }

        return getJsonMapper().readValue(response.getSourceAsString(), clazz);
    }

    @Override
    @SneakyThrows({ IOException.class })
    public <T> List<T> findByIds(Class<T> clazz, String... ids) {
        String[] indexName = getIndexForType(clazz);
        String typeName = MappingBuilder.indexTypeFromClass(clazz);
        //MultiGetResponse response = getClient().prepareMultiGet().add(indexName, typeName, ids).execute().actionGet();
        
        MultiGetRequestBuilder mrb = getClient().prepareMultiGet();
        for (String index : indexName) {
           mrb.add(index, TYPE_NAME, ids);
        }
        MultiGetResponse response = mrb.execute().actionGet();

        if (response == null || response.getResponses() == null || response.getResponses().length == 0) {
            ESIndexMapper.getLog().debug("Nothing found in index [ {} ], type [ {} ], for Ids [ {} ].", Arrays.toString(indexName), typeName, Arrays.toString(ids));
            return null;
        }

        List<T> result = new ArrayList<>();
        for (MultiGetItemResponse getItemResponse : response.getResponses()) {
            if (getItemResponse.getResponse().isExists()) {
                //result.add(getJsonMapper().readValue(getItemResponse.getResponse().getSourceAsString(), clazz));
                result.add((T)getJsonMapper().readValue(getItemResponse.getResponse().getSourceAsString(),
                                                        getTypesToClasses().get(getItemResponse.getResponse().getIndex())));
            }
        }

        return result;
    }

    @Override
    public void delete(Class<?> clazz, String id) {
        assertIdNotNullFor(id, "delete");
        //String indexName = getIndexForType(clazz);
        // should be a concrete class: class name gives index
        String typeName = MappingBuilder.indexTypeFromClass(clazz);
        //getClient().prepareDelete(indexName, typeName, id).setRefreshPolicy(RefreshPolicy.IMMEDIATE).execute().actionGet();
        getClient().prepareDelete(typeName, TYPE_NAME, id).setRefreshPolicy(RefreshPolicy.IMMEDIATE).execute().actionGet();
    }

    private void assertIdNotNullFor(String id, String operation) {
        if (id == null || id.trim().isEmpty()) {
            ESIndexMapper.getLog().error("Null or empty Id is not allowed for operation <" + operation + ">.");
            throw new IndexingServiceException("Null or empty Id is not allowed for operation <" + operation + ">.");
        }
    }

    protected Class<?>[] getRequestedTypes(Class<?> clazz) {
        // FIXME clazz might be null
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
