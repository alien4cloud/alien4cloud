package alien4cloud.dao;

import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.RangeQueryBuilder;

import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Optional;

public class QueryUtils {

    private QueryUtils() {}

    private static long toEpoch(Date date) {
        GregorianCalendar cal = new GregorianCalendar();
        cal.setTime(date);
        return cal.getTimeInMillis();
    }

    public static RangeQueryBuilder buildTimestampRangeQuery(Date from, Date to) {
        RangeQueryBuilder queryBuilder = null;

        if ((from != null) || (to != null)) {
            queryBuilder = QueryBuilders.rangeQuery("timestamp");
            if (from != null) {
                queryBuilder.from(toEpoch(from));
            }
            if (to != null) {
                queryBuilder.to(toEpoch(to));
            }
            return queryBuilder;
        } else {
            return null;
        }
    }

    public static QueryBuilder allOf(QueryBuilder... builders) {
        BoolQueryBuilder result = null;

        for (QueryBuilder b : builders) {
            if (b != null) {
                if (result == null) {
                    result = QueryBuilders.boolQuery();
                }

                result=result.must(b);
            }
        }
        return result;
    }
}
