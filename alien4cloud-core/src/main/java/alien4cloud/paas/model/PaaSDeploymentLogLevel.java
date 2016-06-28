package alien4cloud.paas.model;

public enum PaaSDeploymentLogLevel {
    DEBUG("debug"), INFO("info"), WARN("warn"), ERROR("error");

    private String level;

    PaaSDeploymentLogLevel(String level) {
        this.level = level;
    }

    @Override
    public String toString() {
        return level;
    }

    public static PaaSDeploymentLogLevel fromLevel(String level) {
        if (level == null) {
            return null;
        }
        PaaSDeploymentLogLevel[] allValues = values();
        for (PaaSDeploymentLogLevel value : allValues) {
            if (value.toString().equals(level)) {
                return value;
            }
        }
        return null;
    }

    public static void main(String[] args) {
        System.out.println(fromLevel("debug"));
    }
}
