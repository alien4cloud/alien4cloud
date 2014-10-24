package alien4cloud.rest.suggestion;

import java.io.IOException;

import javax.annotation.Resource;

import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.mangofactory.swagger.annotations.ApiIgnore;

import alien4cloud.dao.IGenericSearchDAO;
import alien4cloud.rest.model.RestResponse;
import alien4cloud.rest.model.RestResponseBuilder;

@RestController
@RequestMapping("/rest/suggestions")
public class GenericSuggestionController {

    @Resource(name = "alien-es-dao")
    private IGenericSearchDAO alienDAO;

    /**
     * Get suggestion from a particular index and type from a particular path
     *
     * @param index      the index to search for suggestion
     * @param type       the type to search for suggestion
     * @param path       the path to get suggestion
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
            return RestResponseBuilder.<String[]>builder().data(new String[0]).build();
        } else {
            queryBuilder = QueryBuilders.regexpQuery(path, ".*?" + searchText + ".*");
        }
        return RestResponseBuilder.<String[]>builder().data(alienDAO.selectPath(index, new String[]{type}, queryBuilder, SortOrder.ASC, path, 0, 10)).build();
    }
}
