package alien4cloud.topology.task;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class RedirectionTask extends AbstractTask {

    private String url;

    public RedirectionTask(String url) {
        setCode(TaskCode.REDIRECTION_REQUIRED);
        this.url = url;
    }

}
