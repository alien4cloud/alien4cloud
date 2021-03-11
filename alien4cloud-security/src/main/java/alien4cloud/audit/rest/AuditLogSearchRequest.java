package alien4cloud.audit.rest;


import alien4cloud.rest.model.FilteredSearchRequest;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.util.Date;

@Getter
@Setter
@ToString
@NoArgsConstructor
public class AuditLogSearchRequest extends FilteredSearchRequest {

    private Date fromDate;

    private Date toDate;
}
