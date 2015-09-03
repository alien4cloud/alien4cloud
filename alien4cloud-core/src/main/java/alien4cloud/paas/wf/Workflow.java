package alien4cloud.paas.wf;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Workflow {

    public static final String INSTALL_WF = "install";

    public static final String UNINSTALL_WF = "uninstall";

    private String name;
    private String description;
    private boolean isStandard;

    private Map<String, AbstractStep> steps = new HashMap<String, AbstractStep>();

    private Set<String> hosts = new HashSet<String>();
    
    public void addStep(AbstractStep step) {
        steps.put(step.getName(), step);
    }

}
