package alien4cloud.rest.suggestion;

import java.util.List;
import java.util.Set;

import javax.annotation.Resource;

import org.alien4cloud.tosca.model.types.AbstractToscaType;
import org.alien4cloud.tosca.model.types.ArtifactType;
import org.alien4cloud.tosca.model.types.CapabilityType;
import org.alien4cloud.tosca.model.types.NodeType;
import org.alien4cloud.tosca.model.types.RelationshipType;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.mapping.MappingBuilder;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.google.common.collect.Sets;

import alien4cloud.dao.ElasticSearchDAO;
import alien4cloud.dao.IGenericSearchDAO;
import alien4cloud.dao.model.FetchContext;
import alien4cloud.dao.model.GetMultipleDataResult;
import alien4cloud.model.application.Application;
import alien4cloud.model.common.Tag;
import alien4cloud.rest.model.RestResponse;
import alien4cloud.rest.model.RestResponseBuilder;
import springfox.documentation.annotations.ApiIgnore;

/**
 * Handle Suggestion requests.
 *
 * @author 'Igor Ngouagna'
 */
@RestController
@RequestMapping({ "/rest/suggest", "/rest/v1/suggest", "/rest/latest/suggest" })
public class SuggestionController {
    private static final int SUGGESTION_COUNT = 10;
    private static final String TAG_FIELD = "tags";
    private static final String[] INDEXES = new String[] { ElasticSearchDAO.TOSCA_ELEMENT_INDEX, Application.class.getSimpleName().toLowerCase() };
    private static final Class<?>[] CLASSES = new Class<?>[] { Application.class, NodeType.class, ArtifactType.class,
            CapabilityType.class, RelationshipType.class };

    @Resource(name = "alien-es-dao")
    private IGenericSearchDAO dao;

    /**
     * Get suggestion for tags based on current tags defined on the components.
     *
     * @param tagName The name of the tag for which to get suggestion.
     * @param searchPrefix The current prefix for the tag suggestion.
     * @return A {@link RestResponse} that contains a list of suggestions for the tag key.
     */
    @ApiIgnore
    @RequestMapping(value = "/tag/{tagName}/{searchPrefix}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("isAuthenticated()")
    public RestResponse<String[]> tagSuggest(@PathVariable String tagName, @PathVariable String searchPrefix) {
        String suggestFieldPath = TAG_FIELD.concat(".").concat(tagName);
        GetMultipleDataResult searchResult = dao.suggestSearch(INDEXES, CLASSES, suggestFieldPath, searchPrefix, FetchContext.TAG_SUGGESTION, 0,
                SUGGESTION_COUNT);
        String[] types = searchResult.getTypes();
        Set<String> tagsSuggestions = Sets.newHashSet();
        for (int i = 0; i < types.length; i++) {
            List<Tag> tags;
            if (types[i].equals(MappingBuilder.indexTypeFromClass(Application.class))) {
                Application app = (Application) searchResult.getData()[i];
                tags = app.getTags();
            } else {
                AbstractToscaType indexedToscaElement = (AbstractToscaType) searchResult.getData()[i];
                tags = indexedToscaElement.getTags();
            }
            addSuggestedTag(tags, tagName, searchPrefix, tagsSuggestions);
        }

        return RestResponseBuilder.<String[]> builder().data(tagsSuggestions.toArray(new String[tagsSuggestions.size()])).build();
    }

    private void addSuggestedTag(List<Tag> tags, String path, String searchPrefix, Set<String> tagsSuggestions) {
        for (Tag tag : tags) {
            String suggestion = "";
            if (path.equals("name")) {
                suggestion = tag.getName();
            } else if (path.equals("value")) {
                suggestion = tag.getValue();
            }
            if (suggestion.startsWith(searchPrefix)) {
                tagsSuggestions.add(suggestion);
            }
        }
    }

    @ApiIgnore
    @RequestMapping(value = "/nodetypes", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("isAuthenticated()")
    public RestResponse<String[]> nodeTypeSuggest(@RequestParam("text") String searchText) {
        if (searchText == null || searchText.trim().isEmpty()) {
            return RestResponseBuilder.<String[]> builder().data(new String[0]).build();
        }
        QueryBuilder queryOnText = QueryBuilders.regexpQuery("elementId", ".*?" + searchText + ".*");
        // FIXME the way of getting the highest version of a component has changed
        // QueryBuilder queryOnHighest = QueryBuilders.termQuery("highestVersion", true);
        QueryBuilder query = QueryBuilders.boolQuery().must(queryOnText);
        return RestResponseBuilder.<String[]> builder()
                .data(dao.selectPath(dao.getIndexForType(NodeType.class), new String[] { MappingBuilder.indexTypeFromClass(NodeType.class) }, query,
                        SortOrder.ASC, "elementId", 0, 10))
                .build();
    }

}
