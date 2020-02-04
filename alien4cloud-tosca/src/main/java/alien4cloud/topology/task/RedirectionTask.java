package alien4cloud.topology.task;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class RedirectionTask extends AbstractTask {

    /**
     * The URL to redirect to. This will be considered by the Wizard UI in order to achieve a redirection.
     */
    private String url;

    /**
     * If not null, this param name will be added to the url with in value, the back URL (url to come back after the redirection).
     */
    private String backUrlParam;

    public RedirectionTask(String url) {
        setCode(TaskCode.REDIRECTION_REQUIRED);
        this.url = url;
    }

    public RedirectionTask(String url, String backUrlParam) {
        setCode(TaskCode.REDIRECTION_REQUIRED);
        this.url = url;
        this.backUrlParam = backUrlParam;
    }

}
