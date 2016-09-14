package alien4cloud.component.dao;

import java.util.concurrent.ExecutionException;

import javax.annotation.Resource;

import org.elasticsearch.client.Client;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.mapping.ElasticSearchClient;
import org.junit.After;
import org.junit.Before;

import alien4cloud.dao.ElasticSearchDAO;
import alien4cloud.model.application.Application;
import org.alien4cloud.tosca.model.templates.Topology;

public abstract class AbstractDAOTest {

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

    private void clearIndex(String indexName) throws InterruptedException, ExecutionException {
        nodeClient.prepareDeleteByQuery(indexName).setQuery(QueryBuilders.matchAllQuery()).execute().get();
    }

    public void refresh() {
        refresh(Application.class.getSimpleName().toLowerCase());
        refresh(ElasticSearchDAO.TOSCA_ELEMENT_INDEX);
        refresh(Topology.class.getSimpleName().toLowerCase());
    }

    @After
    public void clean() throws Exception {
        clearIndex(Application.class.getSimpleName().toLowerCase());
        clearIndex(ElasticSearchDAO.TOSCA_ELEMENT_INDEX);
        clearIndex(Topology.class.getSimpleName().toLowerCase());
        refresh();
    }
}
