package alien4cloud.utils;

import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

public class ElasticSearchUtilTest {

    //@Test
    public void doTest() {
        SearchResponse response = Mockito.mock(SearchResponse.class);
        SearchHits hits = Mockito.mock(SearchHits.class);

        Assert.assertTrue(ElasticSearchUtil.isResponseEmpty(null));
        Mockito.when(response.getHits()).thenReturn(null);
        Assert.assertTrue(ElasticSearchUtil.isResponseEmpty(response));
        Mockito.reset(response);
        Mockito.when(hits.getHits()).thenReturn(null);
        Mockito.when(response.getHits()).thenReturn(hits);
        Assert.assertTrue(ElasticSearchUtil.isResponseEmpty(response));

        Mockito.reset(response, hits);
        Mockito.when(hits.getHits()).thenReturn(new SearchHit[0]);
        Mockito.when(response.getHits()).thenReturn(hits);
        Assert.assertTrue(ElasticSearchUtil.isResponseEmpty(response));

        Mockito.reset(response, hits);
        Mockito.when(hits.getHits()).thenReturn(new SearchHit[10]);
        Mockito.when(response.getHits()).thenReturn(hits);
        Assert.assertFalse(ElasticSearchUtil.isResponseEmpty(response));
    }
}
