package alien4cloud.paas.wf;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class GetAttributeActivity extends AbstractActivity {

    private String attributeName;

    @Override
    public String toString() {
        return getNodeId() + ".getAttribute[" + attributeName + "]";
    }

    @Override
    public String getRepresentation() {
        return getNodeId() + "_get_" + attributeName;
    }

}