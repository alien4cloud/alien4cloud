package alien4cloud.rest.audit.model;

import java.util.Map;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class AuditTrace {

    private String userName;

    private String userEmail;

    private String userFirstName;

    private String userLastName;

    private String path;

    private String method;

    private Map<String, String> requestParameters;

    private String requestBody;

    private int responseStatus;

    private String responseBody;
}
