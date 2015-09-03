package alien4cloud.paas.wf;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OperationCallActivity extends AbstractActivity {

    private String interfaceName;
    private String operationName;

    @Override
    public String toString() {
        return getNodeId() + ".call[" + interfaceName + "." + operationName + "]";
    }

    @Override
    public String getRepresentation() {
        return operationName + "_" + getNodeId();
    }

}