package alien4cloud.model.topology;

public class HaPolicy extends AbstractPolicy {

    public static final String HA_POLICY = "tosca.policy.ha";

    @Override
    public String getType() {
        return HA_POLICY;
    }

}
