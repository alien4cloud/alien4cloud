package alien4cloud.dao;

import alien4cloud.dao.model.FacetedSearchResult;
import org.elasticsearch.index.query.FilterBuilder;
import org.elasticsearch.mapping.IFacetBuilderHelper;

import java.util.List;
import java.util.Map;

public interface IESMetaPropertiesSearchContext {

    List<IFacetBuilderHelper> getFacetBuilderHelpers();

    FilterBuilder[] getFilterBuilders(Map<String, String[]> filters);

    void preProcess(Map<String, String[]> filters);
    void postProcess(FacetedSearchResult result);
}