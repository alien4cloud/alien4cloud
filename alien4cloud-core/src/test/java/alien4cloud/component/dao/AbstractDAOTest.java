package alien4cloud.component.dao;

import java.util.concurrent.ExecutionException;

import javax.annotation.Resource;

import org.elasticsearch.action.support.WriteRequest.RefreshPolicy;
import org.elasticsearch.client.Client;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.action.bulk.BulkRequestBuilder;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.mapping.ElasticSearchClient;
import org.junit.After;
import org.junit.Before;

import alien4cloud.dao.ElasticSearchDAO;
import alien4cloud.model.application.Application;
import org.alien4cloud.tosca.model.templates.Topology;
import org.alien4cloud.tosca.model.types.*;

public abstract class AbstractDAOTest {

    private boolean somethingFound(final SearchResponse searchResponse) {
        if (searchResponse == null || searchResponse.getHits() == null || searchResponse.getHits().getHits() == null
                || searchResponse.getHits().getHits().length == 0) {
            return false;
        }
        return true;
    }

    private void delete(String indexName, String typeName, QueryBuilder query) {
        // get all elements and then use a bulk delete to remove data.
        //SearchRequestBuilder searchRequestBuilder = nodeClient.prepareSearch(indexName).setTypes(typeName).setQuery(query)
        SearchRequestBuilder searchRequestBuilder = nodeClient.prepareSearch(typeName).setQuery(query)
                .setFetchSource(false);
        searchRequestBuilder.setFrom(0).setSize(1000);
        SearchResponse response = searchRequestBuilder.execute().actionGet();

        while (somethingFound(response)) {
            BulkRequestBuilder bulkRequestBuilder = nodeClient.prepareBulk().setRefreshPolicy(RefreshPolicy.IMMEDIATE);

            for (int i = 0; i < response.getHits().getHits().length; i++) {
                String id = response.getHits().getHits()[i].getId();
                //bulkRequestBuilder.add(nodeClient.prepareDelete(indexName, typeName, id));
                bulkRequestBuilder.add(nodeClient.prepareDelete(typeName, ElasticSearchDAO.TYPE_NAME, id));
            }

            bulkRequestBuilder.execute().actionGet();

            if (response.getHits().getTotalHits() == response.getHits().getHits().length) {
                response = null;
            } else {
                response = searchRequestBuilder.execute().actionGet();
            }
        }
    }


    @Resource
    protected ElasticSearchClient esclient;

    protected Client nodeClient;

    @Before
    public void before() throws Exception {
        nodeClient = esclient.getClient();
        clean();
    }

    private void refresh(String indexName) {
        nodeClient.admin().indices().prepareRefresh(indexName).execute().actionGet();
    }

    private void clearIndex(String indexName, String typeName) throws InterruptedException, ExecutionException {
        //nodeClient.prepareDeleteByQuery(indexName).setQuery(QueryBuilders.matchAllQuery()).execute().get();
		delete (indexName, typeName, QueryBuilders.matchAllQuery());
		
    }

    public void refresh() {
        refresh(Application.class.getSimpleName().toLowerCase());
        refresh(NodeType.class.getSimpleName().toLowerCase());
        refresh(ArtifactType.class.getSimpleName().toLowerCase());
        refresh(CapabilityType.class.getSimpleName().toLowerCase());
        refresh(Topology.class.getSimpleName().toLowerCase());
    }

    @After
    public void clean() throws Exception {
        clearIndex(Application.class.getSimpleName().toLowerCase(), Application.class.getSimpleName().toLowerCase());
        clearIndex(ElasticSearchDAO.TOSCA_ELEMENT_INDEX, NodeType.class.getSimpleName().toLowerCase());
        clearIndex(ElasticSearchDAO.TOSCA_ELEMENT_INDEX, ArtifactType.class.getSimpleName().toLowerCase());
        clearIndex(ElasticSearchDAO.TOSCA_ELEMENT_INDEX, CapabilityType.class.getSimpleName().toLowerCase());
        clearIndex(Topology.class.getSimpleName().toLowerCase(), Topology.class.getSimpleName().toLowerCase());
        refresh();
    }
}
