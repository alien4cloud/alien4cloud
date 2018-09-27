package alien4cloud.metaproperty;

import org.elasticsearch.common.collect.Lists;
import org.elasticsearch.mapping.IFacetBuilderHelper;
import org.elasticsearch.mapping.TermsFilterBuilderHelper;
import org.elasticsearch.search.aggregations.AggregationBuilder;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.missing.MissingBuilder;
import org.elasticsearch.search.aggregations.bucket.terms.TermsBuilder;

import java.util.List;

class MetaPropertyAggregationBuilderHelper extends TermsFilterBuilderHelper implements IFacetBuilderHelper {

    private final int size;

    MetaPropertyAggregationBuilderHelper(String nestedPath,String esFieldName) {
        super(false,nestedPath,esFieldName);

        this.size = 10;
    }

    @Override
    public List<AggregationBuilder> buildFacets() {
        TermsBuilder termsBuilder = AggregationBuilders.terms(getEsFieldName()).field(getEsFieldName()).size(size);
        MissingBuilder missingBuilder = AggregationBuilders.missing("missing_" + getEsFieldName()).field(getEsFieldName());

        return Lists.newArrayList(termsBuilder, missingBuilder);
    }
}
