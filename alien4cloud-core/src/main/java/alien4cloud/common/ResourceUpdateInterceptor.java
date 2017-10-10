package alien4cloud.common;

import java.util.List;
import java.util.function.Consumer;

import org.springframework.stereotype.Component;

import com.google.common.collect.Lists;

import alien4cloud.model.application.Application;
import alien4cloud.model.application.ApplicationEnvironment;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Component
@Getter
public class ResourceUpdateInterceptor {
    private List<Consumer<Application>> onNewApplication = Lists.newArrayList();
    private List<Consumer<ApplicationEnvironment>> onNewEnvironment = Lists.newArrayList();
    private List<Consumer<TopologyVersionChangedInfo>> onEnvironmentTopologyVersionChanged = Lists.newArrayList();

    public void runOnNewApplication(Application application) {
        onNewApplication.forEach(func -> func.accept(application));
    }

    public void runOnNewEnvironment(ApplicationEnvironment applicationEnvironment) {
        onNewEnvironment.forEach(func -> func.accept(applicationEnvironment));
    }

    public void runOnEnvironmentTopologyVersionChanged(TopologyVersionChangedInfo topologyVersionChangedInfo) {
        onEnvironmentTopologyVersionChanged.forEach(func -> func.accept(topologyVersionChangedInfo));
    }

    @Getter
    @Setter
    @AllArgsConstructor
    public static class TopologyVersionChangedInfo{
        private ApplicationEnvironment environment;
        private String oldVersion;
        private String newVersion;
    }
}
