package alien4cloud.rest.suggestion;

import java.io.IOException;
import java.util.Map;

import javax.annotation.Resource;

import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.mapping.ElasticSearchClient;
import org.elasticsearch.search.sort.SortBuilders;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import alien4cloud.rest.model.RestResponse;
import alien4cloud.rest.model.RestResponseBuilder;
import alien4cloud.rest.utils.JsonUtil;
import alien4cloud.utils.ElasticSearchUtil;
import alien4cloud.utils.MapUtil;

import com.mangofactory.swagger.annotations.ApiIgnore;

@RestController
@RequestMapping("/rest/suggestions")
public class GenericSuggestionController {

    @Resource
    private ElasticSearchClient esClient;

    /**
     * Get suggestion from a particular index and type from a particular path
     * 
     * @param index the index to search for suggestion
     * @param type the type to search for suggestion
     * @param path the path to get suggestion
     * @param searchText the text that end user type
     * @return an array of suggestion if any exists
     * @throws IOException when deserialization failed
     */
    @ApiIgnore
    @RequestMapping(value = "/{index}/{type}/{path}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public RestResponse<String[]> getSuggestions(@PathVariable("index") String index, @PathVariable("type") String type, @PathVariable("path") String path,
            @RequestParam("text") String searchText) throws IOException {
        QueryBuilder queryBuilder;
        if (searchText == null || searchText.trim().isEmpty()) {
            return RestResponseBuilder.<String[]> builder().data(new String[0]).build();
        } else {
            queryBuilder = QueryBuilders.regexpQuery(path, ".*?" + searchText + ".*");
        }
        SearchRequestBuilder searchRequestBuilder = esClient.getClient().prepareSearch(index);
        searchRequestBuilder.setSearchType(SearchType.QUERY_THEN_FETCH).setQuery(queryBuilder).setSize(10).setFrom(0);
        searchRequestBuilder.setFetchSource(path, null);
        searchRequestBuilder.setTypes(type);
        searchRequestBuilder.addSort(SortBuilders.fieldSort(path));
        SearchResponse searchResponse = searchRequestBuilder.execute().actionGet();
        if (ElasticSearchUtil.isResponseEmpty(searchResponse)) {
            return RestResponseBuilder.<String[]> builder().data(new String[0]).build();
        } else {
            String[] results = new String[searchResponse.getHits().getHits().length];
            for (int i = 0; i < results.length; i++) {
                Map<String, Object> result = JsonUtil.toMap(searchResponse.getHits().getAt(i).getSourceAsString());
                results[i] = String.valueOf(MapUtil.get(result, path));
            }
            return RestResponseBuilder.<String[]> builder().data(results).build();
        }
    }
}
