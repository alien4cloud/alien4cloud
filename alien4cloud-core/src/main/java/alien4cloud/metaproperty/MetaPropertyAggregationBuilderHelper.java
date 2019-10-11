package alien4cloud.metaproperty;

import com.google.common.collect.Lists;
import org.elasticsearch.index.query.ExistsQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.mapping.IFacetBuilderHelper;
import org.elasticsearch.mapping.TermsFilterBuilderHelper;
import org.elasticsearch.search.aggregations.AggregationBuilder;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.missing.MissingAggregationBuilder;
import org.elasticsearch.search.aggregations.bucket.terms.TermsAggregationBuilder;

import java.util.List;

class MetaPropertyAggregationBuilderHelper extends TermsFilterBuilderHelper implements IFacetBuilderHelper {

    private final int size;

    MetaPropertyAggregationBuilderHelper(String nestedPath,String esFieldName) {
        super(false,nestedPath,esFieldName );

        this.size = 10;
    }

    @Override
    public List<AggregationBuilder> buildFacets() {
        TermsAggregationBuilder termsBuilder = AggregationBuilders.terms(getEsFieldName()).field(getEsFieldName()+".keyword").size(size);
        MissingAggregationBuilder missingBuilder = AggregationBuilders.missing("missing_" + getEsFieldName()).field(getEsFieldName()+".keyword");

        return Lists.newArrayList(termsBuilder, missingBuilder);
    }


    @Override
    public QueryBuilder buildFilter(final String key, final String... values) {
        return super.buildFilter(key + ".keyword",values);
    }

}
