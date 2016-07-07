package alien4cloud.rest.application.model;

import java.util.Date;
import java.util.Map;

import alien4cloud.rest.model.BasicSearchRequest;
import alien4cloud.rest.model.SortConfiguration;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
@NoArgsConstructor
public class SearchLogRequest extends BasicSearchRequest {

    private Date fromDate;

    private Date toDate;

    private SortConfiguration sortConfiguration;

    private Map<String, String[]> filters;

    public SearchLogRequest(String query, Integer from, Integer size, Date fromDate, Date toDate, SortConfiguration sortConfiguration,
            Map<String, String[]> filters) {
        super(query, from, size);
        this.fromDate = fromDate;
        this.toDate = toDate;
        this.sortConfiguration = sortConfiguration;
        this.filters = filters;
    }
}
