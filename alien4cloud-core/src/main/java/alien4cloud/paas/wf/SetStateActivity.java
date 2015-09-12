package alien4cloud.paas.wf;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SetStateActivity extends AbstractActivity {

    private String stateName;

    @Override
    public String toString() {
        return getNodeId() + ".setState[" + stateName + "]";
    }

    @Override
    public String getRepresentation() {
        return getNodeId() + "_" + stateName;
    }

}