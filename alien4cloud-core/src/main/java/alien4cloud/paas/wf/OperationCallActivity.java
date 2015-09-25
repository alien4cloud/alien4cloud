package alien4cloud.paas.wf;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OperationCallActivity extends AbstractActivity {

    private String interfaceName;
    private String operationName;

    public void setOperationFqn(String operationFqn) {
        int lastDotIdx = operationFqn.lastIndexOf(".");
        if (lastDotIdx > 0) {
            this.interfaceName = operationFqn.substring(0, lastDotIdx);
            this.operationName = operationFqn.substring(lastDotIdx + 1, operationFqn.length());
        }
    }

    @Override
    public String toString() {
        return getNodeId() + ".call[" + interfaceName + "." + operationName + "]";
    }

    @Override
    public String getRepresentation() {
        return operationName + "_" + getNodeId();
    }

}